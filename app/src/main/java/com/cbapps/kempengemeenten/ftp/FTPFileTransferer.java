package com.cbapps.kempengemeenten.ftp;

import android.util.Log;

import com.cb.kempengemeenten.R;
import com.cbapps.kempengemeenten.callback.OnErrorListener;
import com.cbapps.kempengemeenten.callback.OnProgressUpdateListener;
import com.cbapps.kempengemeenten.callback.OnSuccessListener;
import com.cbapps.kempengemeenten.callback.Predicate;
import com.cbapps.kempengemeenten.files.FileInfo;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.io.CopyStreamAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
					errorListener.onError(R.string.error_file_not_found);
				return null;
			}
			if (remoteFileInfo.isDirectory()) {
				if (errorListener != null)
					errorListener.onError(R.string.error_file_is_directory);
				return null;
			}

			File outputFile = new File(localFileInfo.getPath());
			if (localFileInfo.isDirectory()) {
				//If the (local) location the FTP file should be stored is a directory,
				//create a new file in that directory with the FTP-file's name.
				outputFile = new File(outputFile, remoteFileInfo.getName());
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
						errorListener.onError(R.string.error_ftp_bad_reply);
					return null;
				}
			} catch (FTPConnectionClosedException e) {
				if (errorListener != null)
					errorListener.onError(R.string.error_ftp_connection_closed);
				return null;
			} catch (FileNotFoundException e) {
				if (errorListener != null)
					errorListener.onError(R.string.error_file_not_found);
				return null;
			} catch (IOException e) {
				if (errorListener != null)
					errorListener.onError(R.string.error_unknown_exception);
				return null;
			} catch (Exception e) {
				if (errorListener != null)
					errorListener.onError(R.string.error_unknown_exception);
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
						errorListener.onError(R.string.location_not_set_title);
					return;
				}
				if (!localDirectoryInfo.isDirectory()) {
					errorListener.onError(R.string.error_file_is_directory);
					return;
				}

				for (int i = 0; i < remoteFileInfos.size(); i++) {
					if (predicate == null || predicate.test(remoteFileInfos.get(i))) {
						FileInfo inf = downloadFile(remoteFileInfos.get(i), localDirectoryInfo, fileSuccessListener,
								fileProgressListener, errorListener).get();
					}
				}
			} catch (Exception e) {
				Log.e(TAG, "Uncaught transfer exception: " + e.getMessage());
			}
		});
	}

	public Future<FileInfo> uploadFile(FileInfo localFileInfo, FileInfo remoteDirectoryInfo,
	                                     OnSuccessListener<FileInfo> successListener,
	                                     OnProgressUpdateListener<FileInfo> updateListener,
	                                     OnErrorListener errorListener) {
		return service.submit(() -> {
			if (remoteDirectoryInfo == null || localFileInfo == null) {
				if (errorListener != null)
					errorListener.onError(R.string.location_not_set_title);
				return null;
			}
			if (localFileInfo.isDirectory()) {
				if (errorListener != null)
					errorListener.onError(R.string.error_file_is_directory);
				return null;
			}

			File inputFile = new File(localFileInfo.getPath());
//			if (localFileInfo.isDirectory()) {
//				//If the (local) location the FTP file should be stored is a directory,
//				//create a new file in that directory with the FTP-file's name.
//				outputFile = new File(outputFile, remoteFileInfo.getName());
//			}

			try {
				long fileSize = localFileInfo.getSize();
				connection.getClient().setFileType(FTP.BINARY_FILE_TYPE);
				connection.getClient().setFileTransferMode(FTP.BINARY_FILE_TYPE);
				connection.getClient().setCopyStreamListener(new CopyStreamAdapter() {
					@Override
					public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
						if (updateListener != null)
							updateListener.onProgressUpdate(localFileInfo, (double) totalBytesTransferred / fileSize);
					}
				});

				InputStream inputStream = new FileInputStream(inputFile);
				boolean success = connection.getClient().storeFile(remoteDirectoryInfo.getPath() +
						"/" + localFileInfo.getName(), inputStream);

				connection.getClient().setCopyStreamListener(null);
				inputStream.close();

				if (success) {
					if (successListener != null)
						successListener.onSuccess(localFileInfo);
					return remoteDirectoryInfo;
				} else {
					if (errorListener != null)
						errorListener.onError(R.string.error_ftp_bad_reply);
					return null;
				}
			} catch (FTPConnectionClosedException e) {
				if (errorListener != null)
					errorListener.onError(R.string.error_ftp_connection_closed);
				return null;
			} catch (FileNotFoundException e) {
				if (errorListener != null)
					errorListener.onError(R.string.error_file_not_found);
				return null;
			} catch (IOException e) {
				if (errorListener != null)
					errorListener.onError(R.string.error_unknown_exception);
				return null;
			} catch (Exception e) {
				if (errorListener != null)
					errorListener.onError(R.string.error_unknown_exception);
				return null;
			}
		});
	}

	/**
	 * Uploads the specified files to the FTP server.
	 * @param localFileInfos the (local) files to upload.
	 * @param remoteDirectoryInfo the remote directory to upload the files to.
	 * @param predicate the predicate
	 * @param fileSuccessListener .
	 * @param fileProgressListener .
	 * @param errorListener .
	 */
	public void uploadFiles(List<FileInfo> localFileInfos, FileInfo remoteDirectoryInfo,
	                          Predicate<FileInfo> predicate,
	                          OnSuccessListener<FileInfo> fileSuccessListener,
	                          OnProgressUpdateListener<FileInfo> fileProgressListener,
	                          OnErrorListener<FileInfo> errorListener) {
		service.submit(() -> {
			try {
				if (localFileInfos == null || remoteDirectoryInfo == null) {
					if (errorListener != null)
						errorListener.onError(R.string.location_not_set_title);
					return;
				}
				if (!remoteDirectoryInfo.isDirectory()) {
					errorListener.onError(R.string.error_directory_is_file);
					return;
				}

				for (int i = 0; i < localFileInfos.size(); i++) {
					if (predicate == null || predicate.test(localFileInfos.get(i))) {
						FileInfo inf = uploadFile(localFileInfos.get(i), remoteDirectoryInfo, fileSuccessListener,
								fileProgressListener, errorListener).get();
					}
				}
			} catch (Exception e) {
				Log.e(TAG, "Uncaught transfer exception: " + e.getMessage());
			}
		});
	}
}
