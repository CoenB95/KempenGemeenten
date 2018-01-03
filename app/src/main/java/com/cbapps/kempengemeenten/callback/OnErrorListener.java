package com.cbapps.kempengemeenten.callback;

import android.support.annotation.Nullable;

/**
 * @author CoenB95
 */
@FunctionalInterface
public interface OnErrorListener<T> {
	void onError(@Nullable T info, String errorMessage);

	default void onError(String errorMessage) {
		onError(null, errorMessage);
	}
}
