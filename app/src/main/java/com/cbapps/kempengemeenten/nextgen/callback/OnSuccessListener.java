package com.cbapps.kempengemeenten.nextgen.callback;

/**
 * @author CoenB95
 */
@FunctionalInterface
public interface OnSuccessListener<T> {
	void onSuccess(T result);
}
