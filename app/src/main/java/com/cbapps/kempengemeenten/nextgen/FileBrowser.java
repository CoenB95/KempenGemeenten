package com.cbapps.kempengemeenten.nextgen;

import com.cbapps.kempengemeenten.nextgen.callback.OnErrorListener;
import com.cbapps.kempengemeenten.nextgen.callback.OnProgressUpdateListener;
import com.cbapps.kempengemeenten.nextgen.callback.OnSuccessListener;

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

}