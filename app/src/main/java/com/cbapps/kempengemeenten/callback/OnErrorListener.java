package com.cbapps.kempengemeenten.callback;

import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

/**
 * @author CoenB95
 */
@FunctionalInterface
public interface OnErrorListener<T> {
	void onError(@Nullable T info, @StringRes int errorMessage);

	default void onError(@StringRes int errorMessage) {
		onError(null, errorMessage);
	}
}
