package com.cbapps.kempengemeenten.ftp;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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

	private FTPFileConnection(String hostName, String username, String password) {
		client = new FTPClient();
		this.hostName = hostName;
		this.username = username;
		this.password = password;
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

	public static FTPFileConnection getDefaultConnection(Context context) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		return new FTPFileConnection("ftp.kempengemeenten.nl",
				preferences.getString("ftpUsername", ""),
				preferences.getString("ftpPassword", ""));
	}

	public FTPClient getClient() {
		return client;
	}

	public @StringRes int getError() {
		return error;
	}
}
