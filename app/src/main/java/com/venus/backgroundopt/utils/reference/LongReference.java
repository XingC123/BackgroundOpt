package com.venus.backgroundopt.utils.reference;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/2
 */
public class LongReference extends ObjectReference<Long> {
    public LongReference() {
        this(0L);
    }

    public LongReference(Long initialValue) {
        super(initialValue);
    }
}
