package com.networkmonitor.utils;

import android.os.Binder;

/**
 * Created by ling on 2/26/16.
 */
public class ServiceBinder<T> extends Binder {
    public final T mInstance;

    public ServiceBinder(T instance) {
        mInstance = instance;
    }
}
