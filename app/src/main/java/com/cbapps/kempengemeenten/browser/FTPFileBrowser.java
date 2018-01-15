package com.cbapps.kempengemeenten.browser;

import android.support.annotation.StringRes;
import android.util.Log;

import com.cb.kempengemeenten.R;
import com.cbapps.kempengemeenten.ftp.FTPFileConnection;
import com.cbapps.kempengemeenten.files.FTPFileInfo;
import com.cbapps.kempengemeenten.files.FileInfo;

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
	private @StringRes int error;

	public FTPFileBrowser(FTPFileConnection connection) {
		this.connection = connection;
	}

	@Override
	protected boolean changeDirectory(String subDirName) {
		error = -1;
		try {
			if (!connection.getClient().isConnected())
				connection.connect();
			FTPFile f = connection.getClient().mlistFile(subDirName);
			setCurrentFile(new FTPFileInfo(f).withPathAndName(subDirName));
			return true;
		} catch (FTPConnectionClosedException e) {
			Log.w(TAG, "Connection closed, try to reopen it.");
			if (connection.connect())
				return changeDirectory(subDirName);
			else {
				error = R.string.error_ftp_connection_closed;
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	protected @StringRes int getError() {
		return error;
	}

	@Override
	protected List<FileInfo> listFiles(String remoteDirectoryName) {
		error = -1;
		try {
			if (!connection.getClient().isConnected())
				connection.connect();
			FTPFile[] files = connection.getClient().listFiles(remoteDirectoryName);
			List<FileInfo> fileInfos = new ArrayList<>();
			for (FTPFile file : files) {
				fileInfos.add(new FTPFileInfo(file).withPath(remoteDirectoryName));
			}
			return fileInfos;
		} catch (FTPConnectionClosedException e) {
			Log.w(TAG, "Connection closed, try to reopen it.");
			if (connection.connect())
				return listFiles(remoteDirectoryName);
			else {
				error = connection.getError();
				return null;
			}
		} catch (IOException e) {
			error = R.string.error_unknown_exception;
			return null;
		}
	}
}
