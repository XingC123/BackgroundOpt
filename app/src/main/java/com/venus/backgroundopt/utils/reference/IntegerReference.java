package com.venus.backgroundopt.utils.reference;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/2
 */
public class IntegerReference extends ObjectReference<Integer> {
    public IntegerReference() {
        this(0);
    }

    public IntegerReference(Integer initialValue) {
        super(initialValue);
    }
}
