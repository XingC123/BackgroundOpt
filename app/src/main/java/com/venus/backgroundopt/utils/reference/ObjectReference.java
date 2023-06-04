package com.venus.backgroundopt.utils.reference;

import androidx.annotation.NonNull;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectReference<?> that = (ObjectReference<?>) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
