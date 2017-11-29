package com.cbapps.kempengemeenten.nextgen.callback;

/**
 * @author CoenB95
 */
@FunctionalInterface
public interface OnErrorListener<T> {
	void onError(T info, String errorMessage);

	default void onError(String errorMessage) {
		onError(null, errorMessage);
	}
}
