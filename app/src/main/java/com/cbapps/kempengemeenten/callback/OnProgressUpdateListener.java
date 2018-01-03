package com.cbapps.kempengemeenten.callback;

/**
 * @author CoenB95
 */
@FunctionalInterface
public interface OnProgressUpdateListener<T> {
	/**
	 * @param value the current progress, ranging from 0 to 1.
	 */
	void onProgressUpdate(T file, double value);
}
