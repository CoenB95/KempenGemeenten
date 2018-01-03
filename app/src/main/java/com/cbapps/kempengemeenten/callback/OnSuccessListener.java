package com.cbapps.kempengemeenten.callback;

/**
 * @author CoenB95
 */
@FunctionalInterface
public interface OnSuccessListener<T> {
	void onSuccess(T result);
}
