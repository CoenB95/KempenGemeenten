package com.cbapps.kempengemeenten.nextgen;

import android.util.Log;

import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author CoenB95
 */

public class LocalFileBrowser extends FileBrowser {

	private static final String TAG = "LocalFileBrowser";

	private String error;

	@Override
	protected boolean changeDirectory(String subDirName) {
		setCurrentFile(new DefaultFileInfo(new File(subDirName)));
		return true;
	}

	@Override
	protected String getError() {
		return error;
	}

	@Override
	protected List<FileInfo> listFiles(String directoryName) {
		File directory = new File(directoryName);
		File[] files = directory.listFiles();
		List<FileInfo> fileInfos = new ArrayList<>();
		for (File file : files) {
			fileInfos.add(new DefaultFileInfo(file));
		}
		return fileInfos;
	}
}
