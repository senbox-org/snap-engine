package com.bc.ceres.util;


/**
 * Marker interface for cleaner states.
 * Implementations should be top-level classes or static nested classes only
 * and must not hold a direct reference to the owner object registered in CleanerRegistry.
 */
public interface CleanUpState extends Runnable {
}
