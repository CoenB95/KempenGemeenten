package com.cbapps.kempengemeenten.nextgen;

import android.util.Log;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.io.CopyStreamAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author CoenB95
 */

public class FTPFileBrowser extends FileBrowser {

	private static final String TAG = "FTPFileBrowser";

	private FTPClient client;

	private String hostName;
	private String username;
	private String password;
	private String error;

	public FTPFileBrowser() {
		client = new FTPClient();
		hostName = "ftp.kempengemeenten.nl";
		username = "Geo1";
		password = "Ftpgeo1";
	}

	@Override
	protected boolean changeDirectory(String subDirName) {
		error = null;
		if (!connect()) {
			error = "Could not connect to FTP server.";
			return false;
		}
		try {
			FTPFile f = client.mlistFile(subDirName);
			setCurrentFile(new FTPFileInfo(f));
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean connect() {
		if (client.isConnected()) {
			return true;
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

	@Override
	protected boolean downloadFile(String remoteFileName, File outputFile) {
		error = null;
		if (!connect()) {
			error = "Could not connect to FTP server.";
			return false;
		}
		try {
			FileInfo info = new FTPFileInfo(client.mlistFile(remoteFileName));
			long fileSize = info.getSize();
			client.setCopyStreamListener(new CopyStreamAdapter() {
				@Override
				public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
					if (getProgressListener() != null)
						getProgressListener().onProgressUpdate(info, (double) totalBytesTransferred / fileSize);
				}
			});

			OutputStream outputStream = new FileOutputStream(outputFile);
			boolean success = client.retrieveFile(remoteFileName, outputStream);
			client.setCopyStreamListener(null);
			outputStream.close();
			if (success) {
				Log.d(TAG, "Download success.");
				return true;
			} else {
				error = "Bad reply: " + client.getReplyString();
				return false;
			}
		} catch (FileNotFoundException e) {
			error = "Output-file not found.";
			return false;
		} catch (Exception e) {
			error = "IOException.";
			return false;
		}
	}

	@Override
	protected String getError() {
		return error;
	}

	@Override
	protected List<FileInfo> listFiles(String remoteDirectoryName) {
		if (!connect()) {
			error = "Could not connect to FTP server.";
			return null;
		}
		try {
			FTPFile[] files = client.listFiles(remoteDirectoryName);
			List<FileInfo> fileInfos = new ArrayList<>();
			for (FTPFile file : files) {
				fileInfos.add(new FTPFileInfo(remoteDirectoryName, file));
			}
			return fileInfos;
		} catch (FTPConnectionClosedException e) {
			error = "Connection closed.";
			return null;
		} catch (IOException e) {
			error = "IOException.";
			return null;
		}
	}
}
