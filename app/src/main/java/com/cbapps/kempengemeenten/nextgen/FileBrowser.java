package com.cbapps.kempengemeenten.nextgen;

/**
 * @author CoenB95
 */

public abstract class FileBrowser {

//	private OnSuccessListener successListener;
//	private OnErrorListener errorListener;
//
//	protected FileBrowser(OnSuccessListener successListener) {
//		this.successListener = successListener;
//	}
//
//	protected FileBrowser(OnSuccessListener successListener, OnErrorListener errorListener) {
//		this.successListener = successListener;
//		this.errorListener = errorListener;
//	}

	@FunctionalInterface
	public interface OnErrorListener {
		void onError();
	}

	@FunctionalInterface
	public interface OnProgressUpdateListener {
		/**
		 *
		 * @param value the current progress, ranging from 0 to 1.
		 */
		void onProgressUpdate(double value);
	}

	@FunctionalInterface
	public interface OnSuccessListener<T> {
		void onSuccess(T result);
	}
}
