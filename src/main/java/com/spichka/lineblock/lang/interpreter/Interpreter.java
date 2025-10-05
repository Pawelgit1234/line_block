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
import com.spichka.lineblock.lang.parser.ast.IfNode;
import com.spichka.lineblock.lang.parser.ast.LiteralNode;
import com.spichka.lineblock.lang.parser.ast.PlaceBlockNode;
import com.spichka.lineblock.lang.parser.ast.UnaryOpNode;
import com.spichka.lineblock.lang.parser.ast.VariableNode;

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

    public Interpreter(AstNode root, World world) {
        this.root = root;
        this.world = world;

        this.deepness = 0;
        this.variables = new ArrayList<Variable>();
        this.stopRunning = false;
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
        
        throw new LineBlockException("Unkown AST node: " + node.getClass().getSimpleName(), null);
    }

    private Value visitConstant(ConstantNode n) {
        if (n.constant.type == TokenType.PI)
            return new Value(Value.Type.FLOAT, 3.14);
        else if (n.constant.type == TokenType.E)
            return new Value(Value.Type.FLOAT, 2.71);
        else
            throw new LineBlockException("Unknown constant: " + n.constant.type, n.constant);
    }

    private Value visitBlock(BlockNode n) {
        deepness++;

        int i = 0;
        while (i < n.statements.size()) {
            if (stopRunning)
                break;

            AstNode statement = n.statements.get(i);
            Value result = visit(statement);

            if (result instanceof GotoValue gotoVal) {
                int target = i + gotoVal.targetIndex;
                if (target < 0 || target >= n.statements.size())
                    throw new LineBlockException("GOTO target out of bounds: " + target, null);
                i = target;
            } else {
                i++; // next
            }
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
            throw new LineBlockException("If expects BOOL expression as condition", null);
        }

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

        String binString = binary.toString();
        int intValue = Integer.parseInt(binString, 2);

        switch (n.type.type) {
            case INT:
                return new Value(Value.Type.INT, intValue);
            case FLOAT:
                return new Value(Value.Type.FLOAT, (float) intValue);
            case BOOL:
                return new Value(Value.Type.BOOL, intValue != 0);
            case STRING:
                return new Value(Value.Type.STRING, binString);
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
            throw new LineBlockException("PLACEBLOCK coordinates must be INT", null);
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
        Value left = visit(n.left);
        Value right = visit(n.right);
        TokenType op = n.operator.type;

        switch (op) {
            // --- Assigns ---
            case INT, FLOAT, BOOL, STRING -> {
                if (!(n.left instanceof VariableNode varNode))
                    throw new LineBlockException("Left side of assignment must be a variable", n.operator);

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
                    float divisor = right.asFloat();
                    if (divisor == 0)
                        throw new LineBlockException("Division by zero", n.operator);
                    return new Value(Value.Type.FLOAT, left.asFloat() / divisor);
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
                    return new Value(Value.Type.FLOAT, (float) Math.pow(left.asFloat(), right.asFloat()));
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
                return new Value(Value.Type.BOOL, left.equals(right));
            }
            case NE -> {
                return new Value(Value.Type.BOOL, !left.equals(right));
            }
            case GT -> {
                if (left.isNumber() && right.isNumber())
                    return new Value(Value.Type.BOOL, left.asFloat() > right.asFloat());
                throw new LineBlockException("GT expects numbers", n.operator);
            }
            case LT -> {
                if (left.isNumber() && right.isNumber())
                    return new Value(Value.Type.BOOL, left.asFloat() < right.asFloat());
                throw new LineBlockException("LT expects numbers", n.operator);
            }
            case GE -> {
                if (left.isNumber() && right.isNumber())
                    return new Value(Value.Type.BOOL, left.asFloat() >= right.asFloat());
                throw new LineBlockException("GE expects numbers", n.operator);
            }
            case LE -> {
                if (left.isNumber() && right.isNumber())
                    return new Value(Value.Type.BOOL, left.asFloat() <= right.asFloat());
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

            case GOTO -> {
                if (value.getType() != Value.Type.INT)
                    throw new LineBlockException("GOTO expects INT", n.operator);
                int target = value.asInt();
                return new GotoValue(target);
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
                    return new Value(Value.Type.FLOAT, (float) Math.sin(value.asFloat()));
                throw new LineBlockException("SIN expects INT or FLOAT", n.operator);
            }
            case COS -> {
                if (value.isNumber())
                    return new Value(Value.Type.FLOAT, (float) Math.cos(value.asFloat()));
                throw new LineBlockException("COS expects INT or FLOAT", n.operator);
            }
            case TAN -> {
                if (value.isNumber())
                    return new Value(Value.Type.FLOAT, (float) Math.tan(value.asFloat()));
                throw new LineBlockException("TAN expects INT or FLOAT", n.operator);
            }
            case ASIN -> {
                if (value.isNumber())
                    return new Value(Value.Type.FLOAT, (float) Math.asin(value.asFloat()));
                throw new LineBlockException("ASIN expects INT or FLOAT", n.operator);
            }
            case ACOS -> {
                if (value.isNumber())
                    return new Value(Value.Type.FLOAT, (float) Math.acos(value.asFloat()));
                throw new LineBlockException("ACOS expects INT or FLOAT", n.operator);
            }
            case ATAN -> {
                if (value.isNumber())
                    return new Value(Value.Type.FLOAT, (float) Math.atan(value.asFloat()));
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
                    return new Value(Value.Type.FLOAT, (float) Math.ceil(value.asFloat()));
                throw new LineBlockException("CEIL expects INT or FLOAT", n.operator);
            }
            case FLOOR -> {
                if (value.isNumber())
                    return new Value(Value.Type.FLOAT, (float) Math.floor(value.asFloat()));
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

        throw new LineBlockException("Variable with index " + index + " not found", null);
    }
}
