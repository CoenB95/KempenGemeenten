package com.cbapps.kempengemeenten;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author CoenB95
 */

public class LocalFileBrowser extends FileBrowser {

	private static final String TAG = "LocalFileBrowser";

	private int error;

	@Override
	protected boolean changeDirectory(String subDirName) {
		setCurrentFile(new DefaultFileInfo(new File(subDirName)));
		return true;
	}

	@Override
	protected int getError() {
		return error;
	}

	@Override
	protected List<FileInfo> listFiles(String directoryName) {
		File directory = new File(directoryName);
		File[] files = directory.listFiles();
		List<FileInfo> fileInfos = new ArrayList<>();
		if (files == null)
			return fileInfos;
		for (File file : files) {
			fileInfos.add(new DefaultFileInfo(file));
		}
		return fileInfos;
	}
}
