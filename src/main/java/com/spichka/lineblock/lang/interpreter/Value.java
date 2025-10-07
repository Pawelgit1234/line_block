package com.spichka.lineblock.lang.interpreter;

import com.spichka.lineblock.lang.exceptions.LineBlockException;

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

    public float toFloat() {
        if (type == Type.FLOAT)
            return (float) value;
        else if (type == Type.INT)
            return (float) (int) value;
        
        throw new LineBlockException("Cannot convert type to float", null);
    }

    public boolean isNumber() {
        return type == Type.INT || type == Type.FLOAT;
    }

    public boolean equalsValue(Value other) {
        if (this.type != other.type) return false;
        return switch (type) {
            case INT -> ((int) value) == ((int) other.value);
            case FLOAT -> Math.abs(((float) value) - ((float) other.value)) < 1e-6;
            case BOOL -> ((boolean) value) == ((boolean) other.value);
            case STRING -> ((String) value).equals((String) other.value);
        };
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
