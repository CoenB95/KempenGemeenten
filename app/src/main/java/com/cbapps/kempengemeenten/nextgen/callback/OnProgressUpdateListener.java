package com.cbapps.kempengemeenten.nextgen.callback;

/**
 * @author CoenB95
 */
@FunctionalInterface
public interface OnProgressUpdateListener {
	/**
	 * @param value the current progress, ranging from 0 to 1.
	 */
	void onProgressUpdate(double value);
}
