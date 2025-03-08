package pt.up.fe.comp2025.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.symboltable.JmmSymbolTable;

import java.util.Optional;

/**
 * Utility methods regarding types.
 */
public class TypeUtils {

    private final JmmSymbolTable table;

    /**
     * Initializes TypeUtils with a JmmSymbolTable instance.
     * Throws an exception if the provided table is not of the expected type.
     */
    public TypeUtils(SymbolTable table) {
        if (table instanceof JmmSymbolTable) {
            this.table = (JmmSymbolTable) table;
        } else {
            throw new IllegalArgumentException("Expected JmmSymbolTable instance");
        }
    }

    /**
     * Creates a new integer type.
     */
    public static Type newIntType() {
        return new Type("int", false);
    }

    /**
     * Creates a new boolean type.
     */
    public static Type newBooleanType() {
        return new Type("boolean", false);
    }

    /**
     * Creates a new void type.
     */
    public static Type newVoidType() {
        return new Type("void", false);
    }

    /**
     * Converts a JmmNode representing a type into a {@link Type} object.
     * Ensures the node has a "name" attribute before converting.
     */
    public static Type convertType(JmmNode typeNode) {
        if (!typeNode.hasAttribute("name")) {
            throw new IllegalArgumentException("Type node is missing 'name' attribute: " + typeNode.getKind());
        }

        String name = typeNode.get("name");
        boolean isArray = typeNode.hasAttribute("isArray") && Boolean.parseBoolean(typeNode.get("isArray"));

        return new Type(name, isArray);
    }

    /**
     * Determines the type of an expression based on its kind.
     */
    public Type getExprType(JmmNode expr) {
        Kind kind = Kind.fromString(expr.getKind());

        return switch (kind) {
            case LITERAL -> inferLiteralType(expr);
            case BINARY_OP -> inferBinaryOpType(expr);
            case IDENTIFIER -> lookupVariableType(expr);
            case ACCESS_OR_CALL -> inferArrayAccessType(expr);
            case METHOD_DECL -> lookupMethodReturnType(expr);
            default -> throw new IllegalArgumentException("Unknown expression type: " + kind);
        };
    }

    /**
     * Infers the type of a literal expression based on its value.
     */
    private Type inferLiteralType(JmmNode literalNode) {
        String value = literalNode.get("value");

        if (value.equals("true") || value.equals("false")) {
            return newBooleanType();
        } else {
            return newIntType();
        }
    }

    /**
     * Infers the result type of a binary operation based on the operator.
     */
    private Type inferBinaryOpType(JmmNode expr) {
        if (!expr.hasAttribute("op")) {
            throw new IllegalArgumentException("Binary operation node missing 'op' attribute");
        }

        String op = expr.get("op");

        if (op.equals("&&") || op.equals("||")) {
            return newBooleanType();
        } else if (op.matches("[+\\-*/]")) {
            return newIntType();
        } else if (op.matches("[<>=!]")) {
            return newBooleanType();
        }

        throw new IllegalArgumentException("Unknown binary operator: " + op);
    }

    /**
     * Retrieves the type of a variable by looking it up in the symbol table.
     * Throws an exception if the variable is not found.
     */
    private Type lookupVariableType(JmmNode variableNode) {
        if (!variableNode.hasAttribute("name")) {
            throw new IllegalArgumentException("Variable node is missing 'name' attribute");
        }

        String varName = variableNode.get("name");
        String methodName = variableNode.get("name");

        return Optional.ofNullable(table.getVariableType(varName, methodName))
                .orElseThrow(() -> new IllegalArgumentException("Unknown variable: " + varName));
    }

    /**
     * Infers the type of an array access expression.
     * Ensures that the accessed expression is an array.
     */
    private Type inferArrayAccessType(JmmNode arrayNode) {
        Type arrayExprType = getExprType(arrayNode.getChild(0));

        if (!arrayExprType.isArray()) {
            throw new IllegalArgumentException("Attempted indexing on a non-array type: " + arrayExprType);
        }

        return new Type(arrayExprType.getName(), false);
    }

    /**
     * Retrieves the return type of a method from the symbol table.
     * Throws an exception if the method is not found.
     */
    private Type lookupMethodReturnType(JmmNode methodNode) {
        if (!methodNode.hasAttribute("name")) {
            throw new IllegalArgumentException("Method node is missing 'name' attribute");
        }

        String methodName = methodNode.get("name");

        return Optional.ofNullable(table.getReturnType(methodName))
                .orElseThrow(() -> new IllegalArgumentException("Unknown method: " + methodName));
    }
}
