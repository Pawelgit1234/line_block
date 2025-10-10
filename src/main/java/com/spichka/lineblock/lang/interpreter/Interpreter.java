package com.spichka.lineblock.lang.interpreter;

import java.util.ArrayList;
import java.util.List;

import com.spichka.lineblock.lang.exceptions.LineBlockException;
import com.spichka.lineblock.lang.lexer.Token;
import com.spichka.lineblock.lang.lexer.TokenType;
import com.spichka.lineblock.lang.parser.ast.AstNode;
import com.spichka.lineblock.lang.parser.ast.BinaryOpNode;
import com.spichka.lineblock.lang.parser.ast.BlockNode;
import com.spichka.lineblock.lang.parser.ast.CommandNode;
import com.spichka.lineblock.lang.parser.ast.ConstantNode;
import com.spichka.lineblock.lang.parser.ast.ForNode;
import com.spichka.lineblock.lang.parser.ast.IfNode;
import com.spichka.lineblock.lang.parser.ast.LiteralNode;
import com.spichka.lineblock.lang.parser.ast.PlaceBlockNode;
import com.spichka.lineblock.lang.parser.ast.UnaryOpNode;
import com.spichka.lineblock.lang.parser.ast.VariableNode;
import com.spichka.lineblock.lang.parser.ast.WhileNode;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.CommandBlockExecutor;
import net.minecraft.world.World;

public class Interpreter {
    private final AstNode root;
    private final World world;

    private int deepness;
    private List<Variable> variables;

    private boolean stopRunning;
    private boolean breakLoop;
    private boolean continueLoop;

    public Interpreter(AstNode root, World world) {
        this.root = root;
        this.world = world;

        this.deepness = 0;
        this.variables = new ArrayList<Variable>();
        this.stopRunning = false;
        this.breakLoop = false;
        this.continueLoop = false;
    }

    public void interpret() {
        visit(root);
    }

    private Value visit(AstNode node) {
        if (node instanceof VariableNode n) return visitVariable(n);
        else if (node instanceof LiteralNode n) return visitLiteral(n);
        else if (node instanceof BlockNode n) return visitBlock(n);
        else if (node instanceof CommandNode n) return visitCommand(n);
        else if (node instanceof UnaryOpNode n) return visitUnaryOp(n);
        else if (node instanceof BinaryOpNode n) return visitBinaryOp(n);
        else if (node instanceof ConstantNode n) return visitConstant(n);
        else if (node instanceof PlaceBlockNode n) return visitPlaceBlock(n);
        else if (node instanceof IfNode n) return visitIf(n);
        else if (node instanceof WhileNode n) return visitWhile(n);
        else if (node instanceof ForNode n) return visitFor(n);
        
        throw new LineBlockException("Unknown AST node: " + node.getClass().getSimpleName());
    }

    private Value visitConstant(ConstantNode n) {
        if (n.constant.type == TokenType.PI)
            return new Value(Value.Type.FLOAT, (float) Math.PI);
        else if (n.constant.type == TokenType.E)
            return new Value(Value.Type.FLOAT, (float) Math.E);
        else
            throw new LineBlockException("Unknown constant: " + n.constant.type, n.constant);
    }

    private Value visitBlock(BlockNode n) {
        deepness++;

        for (AstNode stmt : n.statements) {
            if (continueLoop || breakLoop || stopRunning) {
                continueLoop = false;
                break;
            }

            visit(stmt);
        }

        deepness--;
        variables.removeIf(v -> v.deepness > deepness);

        return null;
    }

    private Value visitIf(IfNode n) {
        Value conditionValue = visit(n.conditionNode);
        if (conditionValue.getType() == Value.Type.BOOL) {
            if (conditionValue.asBool())
                visit(n.thenBranchNode);
            else if (n.elseBranchNode != null)
                visit(n.elseBranchNode);
        } else {
            throw new LineBlockException("If expects BOOL expression as condition");
        }

        return null;
    }

    private Value visitWhile(WhileNode n) {
        while (true) {
            Value conditionResult = visit(n.conditionNode);

            if (conditionResult.getType() != Value.Type.BOOL)
                throw new LineBlockException("WHILE expects BOOL expression as condition");
            
            if (!conditionResult.asBool() || breakLoop || stopRunning) {
                breakLoop = false;
                break;
            }

            visit(n.bodyNode);
        }

        return null;
    }

    private Value visitFor(ForNode n) {
        deepness++;
        visit(n.initializerNode);

        while (true) {
            Value conditionResult = visit(n.conditionNode);

            if (conditionResult.getType() != Value.Type.BOOL)
                throw new LineBlockException("FOR expects BOOL expression as condition");

            if (!conditionResult.asBool() || breakLoop || stopRunning) {
                breakLoop = false;
                break;
            }
            
            visit(n.bodyNode);
            visit(n.incrementNode);
        }

        deepness--;
        variables.removeIf(v -> v.deepness >= deepness);

        return null;
    }

    private Value visitLiteral(LiteralNode n) {
        StringBuilder binary = new StringBuilder();

        for (Token bit : n.bits) {
            if (bit.type == TokenType.ZERO)
                binary.append('0');
            else if (bit.type == TokenType.ONE)
                binary.append('1');
            else
                throw new LineBlockException("Invalid bit in literal: " + bit.type, bit);
        }

        binary.reverse();

        String binString = binary.toString();
        int bitCount = binString.length();

        long longValue = Long.parseLong(binString, 2);

        switch (n.type.type) {
            case INT:
                return new Value(Value.Type.INT, (int) longValue);
            case FLOAT:
                if (bitCount != 32)
                    throw new LineBlockException("FLOAT literal must have exactly 32 bits", n.type);
                
                int intBits = (int) longValue;
                float floatValue = Float.intBitsToFloat(intBits);
                return new Value(Value.Type.FLOAT, floatValue);
            case BOOL:
                return new Value(Value.Type.BOOL, longValue != 0);
            case STRING:
                if (bitCount % 8 != 0)
                    throw new LineBlockException("STRING literal bit length must be multiple of 8", n.type);

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < bitCount; i += 8) {
                    String byteStr = binString.substring(i, i + 8);
                    int byteVal = Integer.parseInt(byteStr, 2);
                    sb.append((char) byteVal);
                }
                return new Value(Value.Type.STRING, sb.toString());
            default:
                throw new LineBlockException("Unknown literal type: " + n.type.type, n.type);
        }
    }

    private Value visitPlaceBlock(PlaceBlockNode n) {
        Value xValue = visit(n.x);
        Value yValue = visit(n.y);
        Value zValue = visit(n.z);

        if (xValue.getType() != Value.Type.INT ||
            yValue.getType() != Value.Type.INT ||
            zValue.getType() != Value.Type.INT) {
            throw new LineBlockException("PLACEBLOCK coordinates must be INT");
        }

        BlockPos pos = new BlockPos(
            xValue.asInt(),
            yValue.asInt(),
            zValue.asInt()
        );

        world.setBlockState(pos, n.block.getDefaultState(), 3);

        return null;
    }

    private Value visitBinaryOp(BinaryOpNode n) {
        TokenType op = n.operator.type;

        // --- Assigns ---
        if (op == TokenType.INT || op == TokenType.FLOAT || op == TokenType.BOOL || op == TokenType.STRING) {
            if (!(n.left instanceof VariableNode varNode))
                throw new LineBlockException("Left side of assignment must be a variable", n.operator);

            Value right = visit(n.right);

            Value.Type expectedType = switch (op) {
                case INT -> Value.Type.INT;
                case FLOAT -> Value.Type.FLOAT;
                case BOOL -> Value.Type.BOOL;
                case STRING -> Value.Type.STRING;
                default -> throw new LineBlockException("Unknown assign type: " + op, n.operator);
            };

            Value finalValue = castValue(right, expectedType, n.operator);

            int index = varNode.index.size();
            boolean updated = false;

            for (Variable v : variables) {
                if (v.index == index) {
                    v.value = finalValue;
                    updated = true;
                    break;
                }
            }

            if (!updated)
                variables.add(new Variable(index, finalValue, deepness));

            return finalValue;
        }

        // --- All other binary operations ---
        Value left = visit(n.left);
        Value right = visit(n.right);

        switch (op) {
            // --- Math ---
            case PLUS -> {
                if (left.isNumber() && right.isNumber()) {
                    if (left.getType() == Value.Type.FLOAT || right.getType() == Value.Type.FLOAT)
                        return new Value(Value.Type.FLOAT, left.asFloat() + right.asFloat());
                    else
                        return new Value(Value.Type.INT, left.asInt() + right.asInt());
                } else if (left.getType() == Value.Type.STRING || right.getType() == Value.Type.STRING) {
                    return new Value(Value.Type.STRING, left.toString() + right.toString());
                }
                throw new LineBlockException("PLUS expects numbers or strings", n.operator);
            }

            case MINUS -> {
                if (left.isNumber() && right.isNumber()) {
                    if (left.getType() == Value.Type.FLOAT || right.getType() == Value.Type.FLOAT)
                        return new Value(Value.Type.FLOAT, left.asFloat() - right.asFloat());
                    else
                        return new Value(Value.Type.INT, left.asInt() - right.asInt());
                }
                throw new LineBlockException("MINUS expects numbers", n.operator);
            }

            case MUL -> {
                if (left.isNumber() && right.isNumber()) {
                    if (left.getType() == Value.Type.FLOAT || right.getType() == Value.Type.FLOAT)
                        return new Value(Value.Type.FLOAT, left.asFloat() * right.asFloat());
                    else
                        return new Value(Value.Type.INT, left.asInt() * right.asInt());
                } else if (left.getType() == Value.Type.STRING && right.getType() == Value.Type.INT) {
                    return new Value(Value.Type.STRING, left.asString().repeat(Math.max(0, right.asInt())));
                }
                throw new LineBlockException("MUL expects numbers or (string * int)", n.operator);
            }

            case DIV -> {
                if (left.isNumber() && right.isNumber()) {
                    float divisor = right.toFloat();
                    if (divisor == 0)
                        throw new LineBlockException("Division by zero", n.operator);
                    return new Value(Value.Type.FLOAT, left.toFloat() / divisor);
                }
                throw new LineBlockException("DIV expects numbers", n.operator);
            }

            case MOD -> {
                if (left.isNumber() && right.isNumber()) {
                    if (left.getType() == Value.Type.FLOAT || right.getType() == Value.Type.FLOAT)
                        return new Value(Value.Type.FLOAT, left.asFloat() % right.asFloat());
                    else
                        return new Value(Value.Type.INT, left.asInt() % right.asInt());
                }
                throw new LineBlockException("MOD expects numbers", n.operator);
            }

            case POW -> {
                if (left.isNumber() && right.isNumber())
                    return new Value(Value.Type.FLOAT, (float) Math.pow(left.toFloat(), right.toFloat()));
                throw new LineBlockException("POW expects numbers", n.operator);
            }

            // --- Bits ---
            case BIT_AND -> {
                if (left.getType() == Value.Type.INT && right.getType() == Value.Type.INT)
                    return new Value(Value.Type.INT, left.asInt() & right.asInt());
                throw new LineBlockException("BIT_AND expects INT", n.operator);
            }

            case BIT_OR -> {
                if (left.getType() == Value.Type.INT && right.getType() == Value.Type.INT)
                    return new Value(Value.Type.INT, left.asInt() | right.asInt());
                throw new LineBlockException("BIT_OR expects INT", n.operator);
            }

            case BIT_XOR -> {
                if (left.getType() == Value.Type.INT && right.getType() == Value.Type.INT)
                    return new Value(Value.Type.INT, left.asInt() ^ right.asInt());
                throw new LineBlockException("BIT_XOR expects INT", n.operator);
            }

            case SHL -> {
                if (left.getType() == Value.Type.INT && right.getType() == Value.Type.INT)
                    return new Value(Value.Type.INT, left.asInt() << right.asInt());
                throw new LineBlockException("SHL expects INT", n.operator);
            }

            case SHR -> {
                if (left.getType() == Value.Type.INT && right.getType() == Value.Type.INT)
                    return new Value(Value.Type.INT, left.asInt() >> right.asInt());
                throw new LineBlockException("SHR expects INT", n.operator);
            }

            // --- Logic ---
            case AND -> {
                if (left.getType() == Value.Type.BOOL && right.getType() == Value.Type.BOOL)
                    return new Value(Value.Type.BOOL, left.asBool() && right.asBool());
                throw new LineBlockException("AND expects BOOL", n.operator);
            }

            case OR -> {
                if (left.getType() == Value.Type.BOOL && right.getType() == Value.Type.BOOL)
                    return new Value(Value.Type.BOOL, left.asBool() || right.asBool());
                throw new LineBlockException("OR expects BOOL", n.operator);
            }

            case XOR -> {
                if (left.getType() == Value.Type.BOOL && right.getType() == Value.Type.BOOL)
                    return new Value(Value.Type.BOOL, left.asBool() ^ right.asBool());
                throw new LineBlockException("XOR expects BOOL", n.operator);
            }

            // --- Equation ---
            case EQ -> {
                return new Value(Value.Type.BOOL, left.equalsValue(right));
            }
            case NE -> {
                return new Value(Value.Type.BOOL, !left.equalsValue(right));
            }
            case GT -> {
                if (left.isNumber() && right.isNumber())
                    return new Value(Value.Type.BOOL, left.toFloat() > right.toFloat());
                throw new LineBlockException("GT expects numbers", n.operator);
            }
            case LT -> {
                if (left.isNumber() && right.isNumber())
                    return new Value(Value.Type.BOOL, left.toFloat() < right.toFloat());
                throw new LineBlockException("LT expects numbers", n.operator);
            }
            case GE -> {
                if (left.isNumber() && right.isNumber())
                    return new Value(Value.Type.BOOL, left.toFloat() >= right.toFloat());
                throw new LineBlockException("GE expects numbers", n.operator);
            }
            case LE -> {
                if (left.isNumber() && right.isNumber())
                    return new Value(Value.Type.BOOL, left.toFloat() <= right.toFloat());
                throw new LineBlockException("LE expects numbers", n.operator);
            }

            default -> throw new LineBlockException("Unknown binary operator: " + op, n.operator);
        }
    }

    private Value castValue(Value v, Value.Type expected, Token op) {
        if (v.getType() == expected) return v;

        return switch (expected) {
            case INT -> {
                if (v.getType() == Value.Type.FLOAT) yield new Value(Value.Type.INT, (int) v.asFloat());
                if (v.getType() == Value.Type.BOOL) yield new Value(Value.Type.INT, v.asBool() ? 1 : 0);
                if (v.getType() == Value.Type.STRING)
                    try { yield new Value(Value.Type.INT, Integer.parseInt(v.asString())); }
                    catch (NumberFormatException e) { throw new LineBlockException("Cannot convert STRING to INT", op); }
                throw new LineBlockException("Cannot convert " + v.getType() + " to INT", op);
            }
            case FLOAT -> {
                if (v.getType() == Value.Type.INT) yield new Value(Value.Type.FLOAT, (float) v.asInt());
                if (v.getType() == Value.Type.BOOL) yield new Value(Value.Type.FLOAT, v.asBool() ? 1f : 0f);
                if (v.getType() == Value.Type.STRING)
                    try { yield new Value(Value.Type.FLOAT, Float.parseFloat(v.asString())); }
                    catch (NumberFormatException e) { throw new LineBlockException("Cannot convert STRING to FLOAT", op); }
                throw new LineBlockException("Cannot convert " + v.getType() + " to FLOAT", op);
            }
            case BOOL -> {
                if (v.isNumber()) yield new Value(Value.Type.BOOL, v.asFloat() != 0);
                if (v.getType() == Value.Type.STRING)
                    yield new Value(Value.Type.BOOL, !v.asString().isEmpty());
                throw new LineBlockException("Cannot convert " + v.getType() + " to BOOL", op);
            }
            case STRING -> new Value(Value.Type.STRING, v.toString());
        };
    }


    private Value visitCommand(CommandNode n) {
        if (n.token.type == TokenType.COMMAND) {
            BlockPos pos = n.token.pos;
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (!(blockEntity instanceof CommandBlockBlockEntity commandBlock))
                throw new LineBlockException("No command block found at " + pos, n.token);
            
            CommandBlockExecutor executor = commandBlock.getCommandExecutor();
            executor.execute(world);
        } else if (n.token.type == TokenType.STOP) {
            stopRunning = true;
        } else if (n.token.type == TokenType.BREAK) {
            breakLoop = true;
        } else if (n.token.type == TokenType.CONTINUE) {
            continueLoop = true;
        }

        return null;
    }

    private Value visitUnaryOp(UnaryOpNode n) {
        Value value = visit(n.operand);
        TokenType op = n.operator.type;

        switch (op) {
            case PRINT -> {
                if (!world.getPlayers().isEmpty()) {
                    var player = world.getPlayers().get(0);
                    player.sendMessage(Text.literal(value.toString()), false);
                }
                return null;
            }

            // ------------------ Numbers ------------------
            case PLUS -> {
                if (value.isNumber()) return value; // +x = x
                throw new LineBlockException("PLUS expects a number", n.operator);
            }

            case MINUS -> {
                if (value.getType() == Value.Type.INT)
                    return new Value(Value.Type.INT, -value.asInt());
                else if (value.getType() == Value.Type.FLOAT)
                    return new Value(Value.Type.FLOAT, -value.asFloat());
                throw new LineBlockException("MINUS expects INT or FLOAT", n.operator);
            }

            // ------------------ Bits ------------------
            case BIT_NOT -> {
                if (value.getType() == Value.Type.INT)
                    return new Value(Value.Type.INT, ~value.asInt());
                throw new LineBlockException("BIT_NOT expects INT", n.operator);
            }

            // ------------------ Logic ------------------
            case NOT -> {
                if (value.getType() == Value.Type.BOOL)
                    return new Value(Value.Type.BOOL, !value.asBool());
                throw new LineBlockException("NOT expects BOOL", n.operator);
            }

            // ------------------ Math ------------------
            case SIN -> {
                if (value.isNumber())
                    return new Value(Value.Type.FLOAT, Math.sin(value.toFloat()));
                throw new LineBlockException("SIN expects INT or FLOAT", n.operator);
            }
            case COS -> {
                if (value.isNumber())
                    return new Value(Value.Type.FLOAT, Math.cos(value.toFloat()));
                throw new LineBlockException("COS expects INT or FLOAT", n.operator);
            }
            case TAN -> {
                if (value.isNumber())
                    return new Value(Value.Type.FLOAT, Math.tan(value.toFloat()));
                throw new LineBlockException("TAN expects INT or FLOAT", n.operator);
            }
            case ASIN -> {
                if (value.isNumber())
                    return new Value(Value.Type.FLOAT, Math.asin(value.toFloat()));
                throw new LineBlockException("ASIN expects INT or FLOAT", n.operator);
            }
            case ACOS -> {
                if (value.isNumber())
                    return new Value(Value.Type.FLOAT, Math.acos(value.toFloat()));
                throw new LineBlockException("ACOS expects INT or FLOAT", n.operator);
            }
            case ATAN -> {
                if (value.isNumber())
                    return new Value(Value.Type.FLOAT, Math.atan(value.toFloat()));
                throw new LineBlockException("ATAN expects INT or FLOAT", n.operator);
            }
            case ABS -> {
                if (value.getType() == Value.Type.INT)
                    return new Value(Value.Type.INT, Math.abs(value.asInt()));
                else if (value.getType() == Value.Type.FLOAT)
                    return new Value(Value.Type.FLOAT, Math.abs(value.asFloat()));
                throw new LineBlockException("ABS expects INT or FLOAT", n.operator);
            }
            case CEIL -> {
                if (value.isNumber())
                    return new Value(Value.Type.FLOAT, Math.ceil(value.toFloat()));
                throw new LineBlockException("CEIL expects INT or FLOAT", n.operator);
            }
            case FLOOR -> {
                if (value.isNumber())
                    return new Value(Value.Type.FLOAT, Math.floor(value.toFloat()));
                throw new LineBlockException("FLOOR expects INT or FLOAT", n.operator);
            }

            default -> throw new LineBlockException("Unknown unary operator: " + op, n.operator);
        }
    }

    private Value visitVariable(VariableNode n) {
        int index = n.index.size();

        for (Variable var : variables) {
            if (var.index == index)
                return var.value;
        }

        throw new LineBlockException("Variable with index " + index + " not found");
    }
}