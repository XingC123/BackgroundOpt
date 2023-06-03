package com.venus.backgroundopt.utils.reference;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/2
 */
public class BooleanReference extends ObjectReference<Boolean> {
    public BooleanReference() {
        this(false);
    }

    public BooleanReference(Boolean initialValue) {
        super(initialValue);
    }
}
