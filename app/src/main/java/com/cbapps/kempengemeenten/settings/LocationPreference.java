package com.cbapps.kempengemeenten.settings;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;

import com.cb.kempengemeenten.R;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class LocationPreference extends DialogPreference {

	public static final int MODE_FTP_DIRECTORIES = 1;
	public static final int MODE_FTP_FILES = 2;
	public static final int MODE_LOCAL_DIRECTORIES = 3;
	public static final int MODE_LOCAL_FILES = 4;

	private String path;
	private int browseMode;

	public LocationPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.file_browser_layout);
		switch (attrs.getAttributeValue(null, "browseMode")) {
			case "ftpDirectories":
				browseMode = MODE_FTP_DIRECTORIES;
				break;
			case "ftpFiles":
				browseMode = MODE_FTP_FILES;
				break;
			case "localDirectories":
				browseMode = MODE_LOCAL_DIRECTORIES;
				break;
			case "localFiles":
				browseMode = MODE_LOCAL_FILES;
				break;
			default:
				browseMode = -1;
				break;
		}
	}

	public int getBrowseMode() {
		return browseMode;
	}

	public String getPath() {
		return path;
	}

	public boolean isDirectoriesMode() {
		switch (browseMode) {
			case MODE_FTP_DIRECTORIES:
			case MODE_LOCAL_DIRECTORIES:
				return true;
			default:
				return false;
		}
	}

	public boolean isFilesMode() {
		switch (browseMode) {
			case MODE_FTP_FILES:
			case MODE_LOCAL_FILES:
				return true;
			default:
				return false;
		}
	}

	public boolean isFTPMode() {
		switch (browseMode) {
			case MODE_FTP_DIRECTORIES:
			case MODE_FTP_FILES:
				return true;
			default:
				return false;
		}
	}

	public boolean isLocalMode() {
		switch (browseMode) {
			case MODE_LOCAL_DIRECTORIES:
			case MODE_LOCAL_FILES:
				return true;
			default:
				return false;
		}
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		setPath(restoreValue ? getPersistedString(path) : (String) defaultValue);
	}

	public void setPath(String path) {
		this.path = path;
		persistString(path);
		setSummary(path);
	}
}