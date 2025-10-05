package com.spichka.lineblock.lang.interpreter;

public class Variable {
    public final int index; // name
    public Value value;
    public final int deepness;

    public Variable(int index, Value value, int deepness) {
        this.index = index;
        this.value = value;
        this.deepness = deepness;
    }
}
