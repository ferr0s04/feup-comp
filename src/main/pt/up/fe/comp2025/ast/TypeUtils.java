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
            case PRIMARY -> inferPrimaryType(expr);
            case NEW_OBJECT -> {
                String className = expr.get("name");
                yield new Type(className, false);
            }
            case THIS_REFERENCE -> {
                String className = table.getClassName();
                yield new Type(className, false);
            }
            case NEW_ARRAY -> {
                yield new Type("int", true);
            }
            default -> throw new IllegalArgumentException("Unsupported expression type: " + kind);
        };
    }

    /**
     * Infers the type of a primary expression (literal, identifier, etc.).
     */
    private Type inferPrimaryType(JmmNode primaryNode) {
        // Handle nested primary expressions
        if (primaryNode.getKind().equals("Primary")) {
            if (primaryNode.getChildren().size() == 1) {
                return inferPrimaryType(primaryNode.getChildren().get(0)); // Recursively handle nested expressions
            }
        }

        // Handle literals
        if (primaryNode.hasAttribute("value")) {
            return inferLiteralType(primaryNode);
        }

        // Handle identifiers
        if (primaryNode.getKind().equals("Identifier")) {
            return lookupVariableType(primaryNode);
        }

        // Handle object creation (e.g., new ClassName())
        if (primaryNode.getKind().equals("NewObject")) {
            String className = primaryNode.get("name");
            return new Type(className, false); // Assuming "false" for non-array objects
        }

        // Handle array creation (e.g., new int[10])
        if (primaryNode.getKind().equals("NewArray")) {
            String elementType = primaryNode.get("type");
            return new Type(elementType, true); // Mark it as an array
        }

        // If no case matches, throw an error
        throw new IllegalArgumentException("Unknown primary expression: " + primaryNode);
    }


    /**
     * Infers the type of a literal expression based on its value.
     */
    private Type inferLiteralType(JmmNode literalNode) {
        String value = literalNode.get("value");

        if (value.equals("true") || value.equals("false")) {
            return newBooleanType();
        } else if (value.matches("-?\\d+")) {
            return newIntType(); // Handle integer literals
        } else if (value.matches("\".*\"")) {
            // Handle string literals (if applicable)
            return new Type("String", false);
        } else {
            throw new IllegalArgumentException("Unsupported literal type: " + value);
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

        String methodName = variableNode.getAncestor(Kind.METHOD_DECL)
                .map(node -> node.get("name"))
                .orElse(null);

        // Ensure the variable is declared in the symbol table
        Type type = table.getVariableType(varName, methodName);
        if (type == null) {
            throw new IllegalArgumentException("Variable '" + varName + "' not declared in method '" + methodName + "'");
        }

        return type;
    }


    /**
     * Infers the type of an array access expression.
     * Ensures that the accessed expression is an array or a valid varargs type.
     */
    private Type inferArrayAccessType(JmmNode arrayNode) {
        // Get the type of the array expression (the child node)
        Type arrayExprType = getExprType(arrayNode.getChild(0));

        // Ensure the expression is an array or varargs
        if (!arrayExprType.isArray() && !"Varargs".equals(arrayExprType.getName())) {
            throw new IllegalArgumentException("Attempted indexing on a non-array or non-varargs type: " + arrayExprType);
        }

        // If the type is an array, return the type of its elements (e.g., int[] -> int)
        if (arrayExprType.isArray()) {
            return new Type(arrayExprType.getName(), false); // Return the type of the array element, not the array itself
        }

        // If the type is Varargs, return the element type
        return new Type(arrayExprType.getName(), false); // Assuming Varargs types are handled similarly to arrays
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

        // Ensure the method is declared in the symbol table
        Type type = table.getReturnType(methodName);
        if (type == null) {
            throw new IllegalArgumentException("Method '" + methodName + "' not declared");
        }

        return type;
    }

}
