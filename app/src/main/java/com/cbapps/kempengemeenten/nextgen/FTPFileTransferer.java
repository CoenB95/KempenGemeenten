package com.cbapps.kempengemeenten.nextgen;

import org.apache.commons.net.io.CopyStreamAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author CoenB95
 */

public class FTPFileTransferer {
	private static final String TAG = "FTPFileBrowser";

	private ExecutorService service;
	private FTPFileConnection connection;
	private String error;

	public FTPFileTransferer(FTPFileConnection connection) {
		this.connection = connection;
		service = Executors.newFixedThreadPool(1);
	}

	public void downloadFile(FileInfo remoteFileInfo, FileInfo localFileInfo,
	                         FileBrowser.OnSuccessListener<FileInfo> successListener,
	                         FileBrowser.OnProgressUpdateListener updateListener,
	                         FileBrowser.OnErrorListener errorListener) {
		service.submit(() -> {
			if (remoteFileInfo == null || localFileInfo == null) {
				if (errorListener != null)
					errorListener.onError("Input and/or output file not specified.");
				return;
			}
			if (remoteFileInfo.isDirectory()) {
				if (errorListener != null)
					errorListener.onError("Remote 'file' is a directory. Did you mean to call downloadFiles() instead?");
				return;
			}

			File outputFile = new File(localFileInfo.getPath());
			if (localFileInfo.isDirectory()) {
				//If the (local) location the FTP file should be stored is a directory,
				//create a new file in that directory with the FTP-file's name.
				outputFile = new File(outputFile, remoteFileInfo.getName());
			}

			if (!connection.checkConnection()) {
				if (errorListener != null)
					errorListener.onError("Could not connect to FTP server.");
				return;
			}

			try {
				long fileSize = remoteFileInfo.getSize();
				connection.getClient().setCopyStreamListener(new CopyStreamAdapter() {
					@Override
					public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
						if (updateListener != null)
							updateListener.onProgressUpdate((double) totalBytesTransferred / fileSize);
					}
				});

				OutputStream outputStream = new FileOutputStream(outputFile);
				boolean success = connection.getClient().retrieveFile(remoteFileInfo.getPath(), outputStream);

				connection.getClient().setCopyStreamListener(null);
				outputStream.close();

				if (success) {
					if (successListener != null)
						successListener.onSuccess(null);
				} else {
					if (errorListener != null)
						errorListener.onError("Bad reply: " + connection.getClient().getReplyString());
				}
			} catch (FileNotFoundException e) {
				if (errorListener != null)
					errorListener.onError("Output-file not found");
			} catch (IOException e) {
				if (errorListener != null)
					errorListener.onError("IOException");
			} catch (Exception e) {
				if (errorListener != null)
					errorListener.onError("Uncaught exception: " + e.getMessage());
			}
		});
	}

	public void downloadFiles(List<FileInfo> remoteFileInfos, FileInfo localDirectoryInfo,
	                          FileBrowser.Predicate<FileInfo> predicate,
	                          FileBrowser.OnSuccessListener<Void> successListener,
	                          FileBrowser.OnProgressUpdateListener progressListener,
	                          FileBrowser.OnErrorListener errorListener,
	                          FileBrowser.OnSuccessListener<FileInfo> fileSuccessListener,
	                          FileBrowser.OnProgressUpdateListener fileProgressListener,
	                          FileBrowser.OnErrorListener fileErrorListener) {
		service.submit(() -> {
			if (remoteFileInfos == null || localDirectoryInfo == null) {
				if (errorListener != null)
					errorListener.onError("Input and/or output file(s) not specified.");
				return;
			}
			if (!localDirectoryInfo.isDirectory()) {
				errorListener.onError("Local 'directory' is a file. Did you mean to call downloadFile() instead?");
				return;
			}

			if (progressListener != null)
				progressListener.onProgressUpdate(0);
			int i = 0;
			FileInfo file = remoteFileInfos.get(i);

			if (predicate == null || predicate.test(file)) {
				downloadFile(file, localDirectoryInfo, result -> {
					if (fileSuccessListener != null)
						fileSuccessListener.onSuccess(file);
					if (progressListener != null)
						progressListener.onProgressUpdate((double) (i + 1) / remoteFileInfos.size());
				}, fileProgressListener, fileErrorListener);
			}

			if (successListener != null)
				successListener.onSuccess(null);
		});
	}
}
