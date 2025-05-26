package pt.up.fe.comp2025.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.symboltable.JmmSymbolTable;

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
     * Checks if the type is varargs
     */
    private boolean isVarargs(Type type) {
        return type.getName().contains("Varargs");
    }


    public boolean isImported(Type type) {
        return isImported(type.getName());
    }

    public boolean isImported(String name) {
        var imports = table.getImports();
        for (var i = 0; i < imports.size(); i++) {
            var splited = imports.get(i).split("\\.");
            var last = splited[splited.length - 1];
            if (last.equals(name)) { return true; }
        }
        return false;
    }

    public boolean IsClass(Type type) {
        var class_a = table.getClassName();
        return type.getName().equals(class_a);
    }

    public boolean isClass(String typeName) {
        var class_a = table.getClassName();
        return typeName.equals(class_a);
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
            case LITERAL, STRING -> inferLiteralType(expr);
            case BINARY_OP -> inferBinaryOpType(expr);
            case IDENTIFIER -> lookupVariableType(expr);
            case ARRAY_ACCESS -> inferArrayAccessType(expr);
            case METHOD_DECL -> lookupMethodReturnType(expr);
            case PRIMARY, LENGTH_ACCESS -> inferPrimaryType(expr);
            case ASSIGN_STMT -> {
                lookupAssignStmt(expr);
                yield newVoidType();
            }
            case ARRAY_ASSIGN_STMT -> {
                // New case for Array Assignment
                JmmNode arrayNode = expr.getChild(0);  // Array (left-hand side)
                JmmNode indexNode = expr.getChild(1);  // Index (left-hand side)
                JmmNode valueNode = expr.getChild(2);  // Value to assign (right-hand side)

                // Check array type
                Type arrayType = getExprType(arrayNode);
                if (!arrayType.isArray()) {
                    throw new IllegalArgumentException("Left-hand side of array assignment must be an array type, but found: " + arrayType);
                }

                // Get element type of the array
                Type elementType = new Type(arrayType.getName(), false);  // Array element type (non-array)

                // Check the type of the value being assigned
                Type valueType = getExprType(valueNode);
                if (!valueType.equals(elementType)) {
                    throw new IllegalArgumentException("Type mismatch: Cannot assign " + valueType.getName() + " to " + elementType.getName());
                }

                yield newVoidType();
            }
            case NEW_OBJECT -> {
                String className = expr.get("name");
                yield new Type(className, false);
            }
            case THIS_REFERENCE -> {
                String className = table.getClassName();
                yield new Type(className, false);
            }
            case NEW_ARRAY -> new Type("int", true);
            case ARRAY_LITERAL -> {
                JmmNode firstElement = expr.getChild(0);
                Type firstElementType = getExprType(firstElement);
                yield new Type(firstElementType.getName(), true);
            }
            case INCREMENT -> { // New case for Increment expressions
                // In an Increment expression, the child is the identifier
                Type idType = getExprType(expr.getChild(0));
                if (!idType.getName().equals("int")) {
                    throw new IllegalArgumentException(
                            "Increment/decrement operator requires int type, but found: " + idType.getName()
                    );
                }
                yield idType;
            }
            case UNARY_OP -> inferUnaryOpType(expr);
            case METHOD_CALL -> {
                // Handle method calls
                String methodName = expr.get("name");
                Type targetType = getExprType(expr.getChild(0)); // The object/class being called on

                if (isImported(targetType)) {
                    JmmNode parent = expr.getParent();
                    if (parent != null) {
                        String parentKind = parent.getKind();

                        if (parentKind.equals("AssignStmt")) {
                            // Case: a = M.foo(); - assume return type matches variable type
                            JmmNode variable = parent.getChild(0); // LHS variable
                            Type variableType = lookupVariableType(variable);
                            yield variableType;

                        } else if (parentKind.equals("ExprStmt")) {
                            // Case: M.foo(); - standalone call, assume void return
                            yield newVoidType();

                        } else if (parentKind.equals("ReturnStmt")) {
                            // Case: return M.foo(); - assume return type matches method's return type
                            String currentMethodName = expr.getAncestor(Kind.METHOD_DECL)
                                    .map(node -> node.get("name"))
                                    .orElse(null);
                            if (currentMethodName != null) {
                                Type methodReturnType = table.getReturnType(currentMethodName);
                                yield methodReturnType != null ? methodReturnType : newVoidType();
                            }
                            yield newVoidType();

                        } else {
                            // For other contexts (like inside expressions), assume based on usage
                            // To simple ??? - TODO: rever
                            Type returnType = lookupMethodReturnType(expr);
                            yield returnType;
                        }
                    } else {
                        // No parent context, assume void
                        yield newVoidType();
                    }

                } else {
                    // Target is not imported - it's either 'this' or a local class
                    if (targetType.getName().equals(table.getClassName()) ||
                            targetType.getName().equals("this")) {

                        // Method call on current class - look up actual method signature
                        Type returnType = table.getReturnType(methodName);
                        if (returnType != null) {
                            yield returnType;
                        } else {
                            throw new IllegalArgumentException("Method '" + methodName + "' not found in class");
                        }

                    } else {
                        // Method call on some other non-imported type
                        throw new IllegalArgumentException("Cannot call method on unknown type: " + targetType.getName());
                    }
                }
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
                return inferPrimaryType(primaryNode.getChildren().getFirst()); // Recursively handle nested expressions
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

        // Handle object creation (example: new ClassName())
        if (primaryNode.getKind().equals("NewObject")) {
            String className = primaryNode.get("name");
            return new Type(className, false);
        }

        // Handle array creation (example: new int[7])
        if (primaryNode.getKind().equals("NewArray")) {
            String elementType = primaryNode.get("type");
            return new Type(elementType, true);
        }

        // Length
        if (primaryNode.getKind().equals("LengthAccess")) {
            return new Type(primaryNode.get("length"), false);
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
        } else if (value.matches("-?\\d+")) { // Handle integers
            return newIntType();
        } else if (literalNode.getKind().equals("Literal")) {
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

        Type leftType = getExprType(expr.getChild(0));
        Type rightType = getExprType(expr.getChild(1));

        if (op.equals("+") && (leftType.getName().equals("String") || rightType.getName().equals("String"))) {
            return new Type("String", false); // String concatenation
        } else if (op.equals("&&") || op.equals("||") || op.equals("==")) {
            return newBooleanType();
        } else if (op.matches("[+*/-]|(\\+=|-=|\\*=|/=)")) {
            return newIntType();
        } else if (op.matches("[<>=!]=?")) {
            return newBooleanType();
        }

        throw new IllegalArgumentException("Unknown binary operator: " + op);
    }

    /**
     * Infers the type of a unary operation based on the operator.
     */
    private Type inferUnaryOpType(JmmNode expr) {
        Type childType = getExprType(expr.getChild(0));

        // Checks if the unary operator is '!' and if the child type is boolean
        if (!childType.getName().equals("boolean")) {
            throw new IllegalArgumentException(
                    "Unary '!' operator requires boolean type, but found: " + childType.getName()
            );
        }
        return newBooleanType();
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

        // Verifica se é uma classe importada
        if (isImported(varName)) {
            var prov = new Type(varName, false);
            prov.putObject("isImported", true);
            return prov;
        }

        String methodName = variableNode.getAncestor(Kind.METHOD_DECL)
                .map(node -> node.get("name"))
                .orElse(null);

        // First, try to find it as a variable
        Type type = table.getVariableType(varName, methodName);
        if (type != null) {
            return type;
        }

        // Caso o pai seja um AssignStmt, tenta encontrar o VarDecl correspondente
        if (variableNode.getParent() != null && variableNode.getParent().getKind().equals(Kind.ASSIGN_STMT.getNodeName())) {
            JmmNode parent = variableNode.getParent();
            String assignVarName = parent.get("name");

            // Procura o VarDecl correspondente
            JmmNode varDeclNode = parent.getAncestor(Kind.METHOD_DECL)
                    .flatMap(method -> method.getChildren(Kind.VAR_DECL).stream()
                            .filter(varDecl -> varDecl.get("name").equals(assignVarName))
                            .findFirst())
                    .orElse(null);

            System.out.println("varDeclNode: " + varDeclNode);

            if (varDeclNode != null) {
                boolean a = false;
                if(varDeclNode.getChild(0).get("isArray").equals("true")) {
                    a = true;
                }

                return new Type(varDeclNode.getChild(0).get("name"), a);
            }
        }

        // Se não encontrado, verifica se é uma classe conhecida
        boolean imported = isImported(varName);
        if (imported || table.getClassName().equals(varName)) {
            Type types = new Type(varName, false);
            types.putObject("isImported", imported);
            types.putObject("isStatic", imported);
            return types;
        }

        throw new IllegalArgumentException("Unknown variable or class: " + varName);
    }

    /**
     * Infers the type of an array access expression.
     * Ensures that the accessed expression is an array or a valid varargs type.
     */
    private Type inferArrayAccessType(JmmNode arrayNode) {
        // Get the type of the array expression
        Type arrayExprType = getExprType(arrayNode.getChild(0));

        // Check if the expression is an array or varargs
        if (!arrayExprType.isArray() && !isVarargs(arrayExprType)) {
            throw new IllegalArgumentException("Attempted indexing on a non-array or non-varargs type: " + arrayExprType);
        }

        // If it's an array, return the type of the elements
        arrayExprType.isArray();

        // If it's varargs, return the element type
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

        Type type = table.getReturnType(methodName);
        if (type != null) {
            return type;
        }
        return new Type("void", false);
    }

    public void lookupAssignStmt(JmmNode assignStmt) {
        if (!assignStmt.getKind().equals("AssignStmt")) {
            throw new IllegalArgumentException("Expected AssignStmt node, but found: " + assignStmt.getKind());
        }

        // Get LHS (variable) and RHS (expression)
        JmmNode identifierNode = assignStmt.getChild(0); // Variable
        JmmNode expressionNode = assignStmt.getChild(1); // Value being assigned

        // Ensure LHS is an identifier
        if (!identifierNode.getKind().equals("Identifier")) {
            throw new IllegalArgumentException("Expected Identifier node, but found: " + identifierNode.getKind());
        }

        // Get variable name
        String varName = identifierNode.get("name");

        // Retrieve method name
        String methodName = assignStmt.getAncestor(Kind.METHOD_DECL)
                .map(node -> node.get("name"))
                .orElse(null);

        // Look up variable type in symbol table
        Type varType = table.getVariableType(varName, methodName);
        if (varType == null) {
            throw new IllegalArgumentException("Variable '" + varName + "' not declared in method '" + methodName + "'");
        }

        // Get the type of the RHS expression
        Type exprType = getExprType(expressionNode);

        // Type check: Ensure the assigned expression type matches the variable type
        if (!varType.equals(exprType)) {
            throw new IllegalArgumentException(
                    "Type mismatch: Cannot assign " + exprType.getName() + " to " + varType.getName()
            );
        }
    }


}
