package com.spichka.lineblock.lang.interpreter;

public class GotoValue extends Value {
    public final int targetIndex;

    public GotoValue(int targetIndex) {
        super(Type.INT, targetIndex); // workaround
        this.targetIndex = targetIndex;
    }
}
