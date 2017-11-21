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

public class FTPFileConnection {
	private static final String TAG = "FTPFileConnection";

	private FTPClient client;

	private String hostName;
	private String username;
	private String password;
	private String error;

	public FTPFileConnection() {
		client = new FTPClient();
		hostName = "ftp.kempengemeenten.nl";
		username = "Geo1";
		password = "Ftpgeo1";
	}

	public boolean checkConnection() {
		return connect();
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

	public FTPClient getClient() {
		return client;
	}
}
