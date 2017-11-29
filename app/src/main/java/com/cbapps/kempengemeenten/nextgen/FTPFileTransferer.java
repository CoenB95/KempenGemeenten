package com.cbapps.kempengemeenten.nextgen;

import android.util.Log;

import com.cbapps.kempengemeenten.nextgen.callback.OnErrorListener;
import com.cbapps.kempengemeenten.nextgen.callback.OnProgressUpdateListener;
import com.cbapps.kempengemeenten.nextgen.callback.OnSuccessListener;
import com.cbapps.kempengemeenten.nextgen.callback.Predicate;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.io.CopyStreamAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author CoenB95
 */

public class FTPFileTransferer {
	private static final String TAG = "FTPFileBrowser";

	private ExecutorService service;
	private FTPFileConnection connection;
	private FileInfo currentFile;
	private String error;

	public FTPFileTransferer(FTPFileConnection connection) {
		this.connection = connection;
		service = Executors.newFixedThreadPool(2);
	}

	public Future<FileInfo> downloadFile(FileInfo remoteFileInfo, FileInfo localFileInfo,
	                           OnSuccessListener<FileInfo> successListener,
	                           OnProgressUpdateListener<FileInfo> updateListener,
	                           OnErrorListener errorListener) {
		return service.submit(() -> {
			if (remoteFileInfo == null || localFileInfo == null) {
				if (errorListener != null)
					errorListener.onError("Input and/or output file not specified.");
				return null;
			}
			if (remoteFileInfo.isDirectory()) {
				if (errorListener != null)
					errorListener.onError("Remote 'file' is a directory. Did you mean to call downloadFiles() instead?");
				return null;
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
				return null;
			}

			try {
				long fileSize = remoteFileInfo.getSize();
				connection.getClient().setFileType(FTP.BINARY_FILE_TYPE);
				connection.getClient().setCopyStreamListener(new CopyStreamAdapter() {
					@Override
					public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
						if (updateListener != null)
							updateListener.onProgressUpdate(remoteFileInfo, (double) totalBytesTransferred / fileSize);
					}
				});

				OutputStream outputStream = new FileOutputStream(outputFile);
				boolean success = connection.getClient().retrieveFile(remoteFileInfo.getPath(), outputStream);

				connection.getClient().setCopyStreamListener(null);
				outputStream.flush();
				outputStream.close();

				if (success) {
					if (successListener != null)
						successListener.onSuccess(remoteFileInfo);
					return remoteFileInfo;
				} else {
					if (errorListener != null)
						errorListener.onError("Bad reply: " + connection.getClient().getReplyString());
					return null;
				}
			} catch (FileNotFoundException e) {
				if (errorListener != null)
					errorListener.onError("Output-file not found");
				return null;
			} catch (IOException e) {
				if (errorListener != null)
					errorListener.onError("IOException");
				return null;
			} catch (Exception e) {
				if (errorListener != null)
					errorListener.onError("Uncaught exception: " + e.getMessage());
				return null;
			}
		});
	}

	public void downloadFiles(List<FileInfo> remoteFileInfos, FileInfo localDirectoryInfo,
	                          Predicate<FileInfo> predicate,
	                          OnSuccessListener<FileInfo> fileSuccessListener,
	                          OnProgressUpdateListener<FileInfo> fileProgressListener,
	                          OnErrorListener<FileInfo> errorListener) {
		service.submit(() -> {
			try {
				if (remoteFileInfos == null || localDirectoryInfo == null) {
					if (errorListener != null)
						errorListener.onError("Input and/or output file(s) not specified.");
					return;
				}
				if (!localDirectoryInfo.isDirectory()) {
					errorListener.onError("Local 'directory' is a file. Did you mean to call downloadFile() instead?");
					return;
				}

				for (int i = 0; i < remoteFileInfos.size(); i++) {
					currentFile = remoteFileInfos.get(i);

					if (predicate == null || predicate.test(currentFile)) {
						FileInfo inf = downloadFile(currentFile, localDirectoryInfo, fileSuccessListener,
								fileProgressListener, errorListener).get();
					}
				}
			} catch (Exception e) {
				Log.e(TAG, "Uncaught transfer exception: " + e.getMessage());
			}
		});
	}

	public FileInfo getCurrentFile() {
		return currentFile;
	}
}
