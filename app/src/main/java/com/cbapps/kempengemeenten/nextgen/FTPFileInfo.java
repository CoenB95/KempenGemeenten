package com.cbapps.kempengemeenten.nextgen;

import android.support.annotation.NonNull;

import org.apache.commons.net.ftp.FTPFile;

/**
 * @author CoenB95
 */
public class FTPFileInfo implements FileInfo {

	private FTPFile file;

	public FTPFileInfo(FTPFile file) {
		this.file = file;
	}

	public FTPFileInfo withPath(String baseDir) {
		FTPFileInfo info = new FTPFileInfo(file);
		if (baseDir != null && !baseDir.isEmpty())
			info.file.setName(baseDir + '/' + file.getName());
		return info;
	}

	public FTPFileInfo withPathAndName(String baseDir) {
		FTPFileInfo info = new FTPFileInfo(file);
		if (baseDir != null && !baseDir.isEmpty())
			info.file.setName(baseDir);
		return info;
	}

	@Override
	public String getPath() {
		return file.getName();
	}

	@Override
	public String getName() {
		int index = file.getName().lastIndexOf('/');
		if (index >= 0)
			return file.getName().substring(index + 1);
		return file.getName();
	}

	@Override
	public long getSize() {
		return file.getSize();
	}

	@Override
	public boolean isDirectory() {
		return file.isDirectory();
	}

	@Override
	public String toString() {
		return "FTPFileInfo{" +
				"path=" + getPath() +
				", size=" + getSize() +
				'}';
	}

	@Override
	public int compareTo(@NonNull FileInfo other) {
		return getName().compareTo(other.getName());
	}
}
