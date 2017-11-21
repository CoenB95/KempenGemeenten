package com.cbapps.kempengemeenten.nextgen.callback;

/**
 * @author CoenB95
 */
@FunctionalInterface
public interface OnErrorListener {
	void onError(String errorMessage);
}
