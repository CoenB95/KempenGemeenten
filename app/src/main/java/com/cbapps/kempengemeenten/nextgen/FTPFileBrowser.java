package com.cbapps.kempengemeenten.nextgen;

import android.content.Context;
import android.util.Log;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.io.CopyStreamAdapter;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author CoenB95
 */

public class FTPFileBrowser extends FileBrowser {

	private static final String TAG = "FTPFileBrowser";

	private FTPClient client;
	private ExecutorService service;
	private String hostName;
	private String username;
	private String password;

	public FTPFileBrowser() {
		client = new FTPClient();
		service = Executors.newCachedThreadPool();
		hostName = "ftp.kempengemeenten.nl";
		username = "Geo1";
		password = "Ftpgeo1";
	}

	private boolean connect() {
		if (client.isConnected()) {
			throw new IllegalStateException("Client is still connected. Should have been closed.");
		}
		try {
			client.connect(hostName);
			client.enterLocalPassiveMode();
			try {
				if (!client.login(username, password))
					return false;
				client.setBufferSize(1024 * 1024);
			} catch (FTPConnectionClosedException e) {
				Log.e(TAG, "Connection closed while logging in.");
			}
			return true;
		} catch (IOException e) {
			Log.e(TAG, "Could not open connection: " + e.getMessage());
			return false;
		}
	}

	public void downloadFiles(String remoteFileName, File outputFile,
	                          OnSuccessListener<Void> successListener,
	                          final OnProgressUpdateListener updateListener,
	                          OnErrorListener errorListener) {
		service.submit(() -> {
			if (!connect()) {
				Log.e(TAG, "Download failed. Could not connect.");
				if (errorListener != null) errorListener.onError();
				return;
			}
			try {
				if (updateListener != null) {
					long fileSize = client.mlistFile(remoteFileName).getSize();
					client.setCopyStreamListener(new CopyStreamAdapter() {
						@Override
						public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
							updateListener.onProgressUpdate((double)totalBytesTransferred / fileSize);
						}
					});
				}
				OutputStream outputStream = new FileOutputStream(outputFile);
				if (client.retrieveFile(remoteFileName, outputStream)) {
					Log.d(TAG, "Download success.");
					if (successListener != null) successListener.onSuccess(null);
				} else {
					Log.e(TAG, "Download failed. Reply: " + client.getReplyString());
					if (errorListener != null) errorListener.onError();
				}
				client.setCopyStreamListener(null);
				outputStream.close();
			} catch (FileNotFoundException e) {
				Log.e(TAG, "Download failed: output-file not found.");
			} catch (IOException e) {
				Log.e(TAG, "Download failed: IOException.");
			}
		});
	}
}
