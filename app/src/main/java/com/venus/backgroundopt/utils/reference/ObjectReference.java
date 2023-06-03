package com.venus.backgroundopt.utils.reference;

import androidx.annotation.NonNull;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/2
 */
public class ObjectReference<T> {
    private T value;

    public ObjectReference() {
    }

    public ObjectReference(T initialValue) {
        this.value = initialValue;
    }

    public void set(T value) {
        this.value = value;
    }

    public T get() {
        return this.value;
    }

    @NonNull
    @Override
    public String toString() {
        return value.toString();
    }
}
