package com.cbapps.kempengemeenten;

import android.support.annotation.StringRes;
import android.util.Log;

import com.cb.kempengemeenten.R;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;

/**
 * @author CoenB95
 */

public class FTPFileConnection {
	private static final String TAG = "FTPFileConnection";

	private static FTPFileConnection connection;

	private FTPClient client;

	private String hostName;
	private String username;
	private String password;
	private @StringRes int error;

	private FTPFileConnection() {
		client = new FTPClient();
		hostName = "ftp.kempengemeenten.nl";
		username = "Geo1";
		password = "Ftpgeo1";
	}

	public boolean connect() {
		try {
			Log.w(TAG, "Connecting to server...");
			client.connect(hostName);
			client.enterLocalPassiveMode();
			try {
				if (!client.login(username, password)) {
					error = R.string.error_ftp_credentials_wrong;
					return false;
				}
				client.setBufferSize(1024 * 1024);
			} catch (FTPConnectionClosedException e) {
				Log.e(TAG, "Connection closed while logging in.");
				error = R.string.error_ftp_connection_closed;
			}
			return true;
		} catch (Exception e) {
			Log.e(TAG, "Could not open connection: " + e.getMessage());
			error = R.string.error_unknown_exception;
			return false;
		}
	}

	public static FTPFileConnection getConnection() {
		if (connection == null)
			connection = new FTPFileConnection();
		return connection;
	}

	public FTPClient getClient() {
		return client;
	}

	public @StringRes int getError() {
		return error;
	}
}
