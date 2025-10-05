package com.spichka.lineblock.lang.interpreter;

public class Value {
    public enum Type { INT, FLOAT, STRING, BOOL }

    private final Type type;
    private final Object value;

    public Value(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public int asInt() { return (int) value; }
    public float asFloat() { return (float) value; }
    public String asString() { return (String) value; }
    public boolean asBool() { return (boolean) value; }

    public boolean isNumber() {
        return type == Type.INT || type == Type.FLOAT;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
