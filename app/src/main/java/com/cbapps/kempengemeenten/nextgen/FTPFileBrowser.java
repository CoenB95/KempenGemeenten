package com.cbapps.kempengemeenten.nextgen;

import android.util.Log;

import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author CoenB95
 */

public class FTPFileBrowser extends FileBrowser {

	private static final String TAG = "FTPFileBrowser";

	private FTPFileConnection connection;
	private String error;

	public FTPFileBrowser(FTPFileConnection connection) {
		this.connection = connection;
	}

	@Override
	protected boolean changeDirectory(String subDirName) {
		error = null;
		if (!connection.checkConnection()) {
			error = "Could not connect to FTP server.";
			return false;
		}
		try {
			FTPFile f = connection.getClient().mlistFile(subDirName);
			setCurrentFile(new FTPFileInfo(f).withPathAndName(subDirName));
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	protected String getError() {
		return error;
	}

	@Override
	protected List<FileInfo> listFiles(String remoteDirectoryName) {
		if (!connection.checkConnection()) {
			error = "Could not connect to FTP server.";
			return null;
		}
		try {
			FTPFile[] files = connection.getClient().listFiles(remoteDirectoryName);
			List<FileInfo> fileInfos = new ArrayList<>();
			for (FTPFile file : files) {
				fileInfos.add(new FTPFileInfo(file).withPath(remoteDirectoryName));
			}
			return fileInfos;
		} catch (FTPConnectionClosedException e) {
			error = "Connection closed.";
			return null;
		} catch (IOException e) {
			error = "IOException: " + e.getMessage();
			return null;
		}
	}
}
