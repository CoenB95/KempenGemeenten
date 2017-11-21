package com.cbapps.kempengemeenten.nextgen;

/**
 * @author CoenB95
 */

public interface FileInfo {
	String getPath();
	String getName();
	long getSize();
	boolean isDirectory();
}
