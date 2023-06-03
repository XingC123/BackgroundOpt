package com.venus.backgroundopt.utils.reference;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/2
 */
public class StringReference extends ObjectReference<String> {
    public StringReference() {
        this(null);
    }

    public StringReference(String initialValue) {
        super(initialValue);
    }
}
