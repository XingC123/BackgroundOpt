package com.venus.backgroundopt.utils.reference;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/2
 */
public class DoubleReference extends ObjectReference<Double> {
    public DoubleReference() {
        this(0D);
    }

    public DoubleReference(Double initialValue) {
        super(initialValue);
    }
}
