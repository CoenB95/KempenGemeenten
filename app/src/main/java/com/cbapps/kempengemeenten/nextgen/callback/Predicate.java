package com.cbapps.kempengemeenten.nextgen.callback;

/**
 * @author CoenB95
 */
@FunctionalInterface
public interface Predicate<T> {
	boolean test(T var1);
}
