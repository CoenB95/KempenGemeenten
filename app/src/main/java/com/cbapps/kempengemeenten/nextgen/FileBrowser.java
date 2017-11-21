package com.cbapps.kempengemeenten.nextgen;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author CoenB95
 */

public abstract class FileBrowser {

	private static final String TAG = "FileBrowser";

	private ExecutorService service;
	private FileInfo currentFile;
	private OnProgressUpdateListener progressListener;

	public FileBrowser() {
		service = Executors.newFixedThreadPool(1);
	}

	protected abstract boolean changeDirectory(String subDirName);

	public void downloadFile(File outputFile, OnSuccessListener<Void> successListener,
	                         OnProgressUpdateListener updateListener,
	                         OnErrorListener errorListener) {
		service.submit(() -> {
			if (outputFile == null) {
				if (errorListener != null)
					errorListener.onError("Output file not specified.");
				return;
			}
			if (currentFile.isDirectory()) {
				if (errorListener != null)
					errorListener.onError("Input 'file' is a directory. Did you mean to call downloadFiles() instead?");
				return;
			}

			File correctedOutputFile = outputFile;
			if (outputFile.isDirectory()) {
				correctedOutputFile = new File(outputFile, currentFile.getName());
			}

			progressListener = updateListener;
			if (downloadFile(currentFile.getPath(), correctedOutputFile)) {
				if (successListener != null)
					successListener.onSuccess(null);
			} else {
				if (errorListener != null)
					errorListener.onError("Downloading file failed: " + getError());
			}
			progressListener = null;
		});
	}

	protected abstract boolean downloadFile(String remoteFileName, File outputFile);

	public void downloadFiles(File outputDirectory,
	                          Predicate<FileInfo> predicate,
	                          OnSuccessListener<Void> successListener,
	                          OnProgressUpdateListener updateListener,
	                          OnProgressUpdateListener globalUpdateListener,
	                          OnErrorListener errorListener) {
		service.submit(() -> {
			if (!currentFile.isDirectory()) {
				errorListener.onError("Input 'directory' is a file. Did you mean to call downloadFile() instead?");
				return;
			}
			if (!outputDirectory.isDirectory()) {
				errorListener.onError("Output 'directory' is a file. Did you mean to call downloadFile() instead?");
				return;
			}
			List<FileInfo> files = listFiles(currentFile.getPath());
			if (files == null) {
				errorListener.onError("Error listing files to download: " + getError());
			} else {
				if (globalUpdateListener != null)
					globalUpdateListener.onProgressUpdate(null,0);
				for (int i = 0; i < files.size(); i++) {
					FileInfo file = files.get(i);
					if (predicate == null || predicate.test(file)) {
						progressListener = updateListener;
						File outputFile = new File(outputDirectory, file.getName());
						if (!downloadFile(file.getPath(), outputFile)) {
							if (errorListener != null)
								errorListener.onError("Downloading files failed: " + getError());
							progressListener = null;
							return;
						}
						progressListener = null;
					}
					if (globalUpdateListener != null)
						globalUpdateListener.onProgressUpdate(null, (double) (i + 1) / files.size());
				}
				if (successListener != null)
					successListener.onSuccess(null);
			}
		});
	}

	public FileInfo getCurrentFile() {
		return currentFile;
	}

	protected abstract String getError();

	protected OnProgressUpdateListener getProgressListener() {
		return progressListener;
	}

	public void listFiles(OnSuccessListener<List<FileInfo>> successListener,
	                      OnErrorListener errorListener) {
		service.submit(() -> {
			List<FileInfo> fileInfos = listFiles(currentFile.getPath());
			if (fileInfos != null) {
				if (successListener != null)
					successListener.onSuccess(fileInfos);
			} else {
				if (errorListener != null)
					errorListener.onError("Error listing files: " + getError());
			}
		});
	}

	protected abstract List<FileInfo> listFiles(String remoteDirectoryName);

	public void moveIntoDirectory(String remoteDirectoryName, OnSuccessListener<FileInfo> successListener,
	                              OnErrorListener errorListener) {
		service.submit(() -> {
			if (changeDirectory(remoteDirectoryName)) {
				if (successListener != null)
					successListener.onSuccess(currentFile);
			} else {
				if (errorListener != null)
					errorListener.onError("Unknown error.");
			}
		});
	}

	protected void setCurrentFile(FileInfo value) {
		currentFile = value;
	}

	@FunctionalInterface
	public interface OnErrorListener {
		void onError(String errorMessage);
	}

	@FunctionalInterface
	public interface OnProgressUpdateListener {
		/**
		 *
		 * @param value the current progress, ranging from 0 to 1.
		 */
		void onProgressUpdate(FileInfo file, double value);
	}

	@FunctionalInterface
	public interface OnSuccessListener<T> {
		void onSuccess(T result);
	}

	@FunctionalInterface
	public interface Predicate<T> {
		boolean test(T var1);
	}
}
