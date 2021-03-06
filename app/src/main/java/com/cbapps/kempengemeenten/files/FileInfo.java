package com.cbapps.kempengemeenten.files;

/**
 * @author CoenB95
 */

public interface FileInfo extends Comparable<FileInfo> {
	String getPath();
	String getName();
	long getSize();
	boolean isDirectory();
}
