package com.venus.backgroundopt.reference;

/**
 * @author XingC
 * @date 2024/2/2
 */
public class ObjectReference<V> {
    private V value;

    public ObjectReference() {
    }

    public ObjectReference(V value) {
        this.value = value;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }
}
