package com.cbapps.kempengemeenten.nextgen;

import android.support.annotation.NonNull;

import org.apache.commons.net.ftp.FTPFile;

import java.io.File;

/**
 * @author CoenB95
 */
public class DefaultFileInfo implements FileInfo {

	private File file;

	public DefaultFileInfo(File file) {
		this.file = file;
	}

	@Override
	public String getPath() {
		return file.getAbsolutePath();
	}

	@Override
	public String getName() {
		return file.getName();
	}

	@Override
	public long getSize() {
		return file.length();
	}

	@Override
	public boolean isDirectory() {
		return file.isDirectory();
	}

	@Override
	public int compareTo(@NonNull FileInfo other) {
		return getName().compareTo(other.getName());
	}
}
