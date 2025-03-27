package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.comp.jmm.analysis.table.Type;

public class TypeCheckingVisitor extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.BINARY_OP, this::visitBinaryExpr);
        addVisit(Kind.STMT, this::visitStmt);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.PRIMARY, this::visitPrimaryExpr);
        addVisit(Kind.ARRAY_LITERAL, this::visitArrayLiteral);
        addVisit(Kind.INCREMENT, this::visitUnaryExpr);
    }

    /**
     * This function checks and reports errors for binary expressions (arithmetic, comparison, etc.).
     */
    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);
        String op = binaryExpr.get("op");

        // Ensure the variables involved in the binary expression are declared
        Type leftType = typeUtils.getExprType(binaryExpr.getChild(0));
        Type rightType = typeUtils.getExprType(binaryExpr.getChild(1));

        if (leftType == null || rightType == null) {
            addReport(newError(binaryExpr, "One or both variables are unknown."));
            return null;
        }

        if (op.matches("[+*/-]|(\\+=|-=|\\*=|/=)")) {

            if (leftType.isArray() || rightType.isArray()) {
                // Handle case where one of the operands is an array
                addReport(newError(binaryExpr, "Cannot sum an array with a primitive type."));
            } else {
                // Both operands must be integers for arithmetic operations
                if (!leftType.getName().equals("int") || !rightType.getName().equals("int")) {
                    addReport(newError(binaryExpr, "Arithmetic operations require integer operands."));
                }
            }

        } else if (op.matches("[<>=!]=?") || op.equals("&&") || op.equals("||")) {

            if (!leftType.getName().equals(rightType.getName())) {
                addReport(newError(binaryExpr, "Operators require operands of the same type."));
            }

        }

        return null;
    }

    /**
     * This function handles statements like assignments, conditionals, and loops by checking type correctness.
     */
    private Void visitStmt(JmmNode stmt, SymbolTable table) {

        if (stmt.getNumChildren() == 0) {
            return null;
        }

        TypeUtils typeUtils = new TypeUtils(table);
        String stmtKind = stmt.getKind();

        // Check for assignment statements
        if (stmtKind.equals(Kind.ASSIGN_STMT.getNodeName())) {
            if (!stmt.hasAttribute("name")) {
                addReport(newError(stmt, "Internal error: ASSIGN_STMT no variable name."));
                return null;
            }

            String varName = stmt.get("name");

            // Retrieve assigned value node (should be the first and only child)
            if (stmt.getNumChildren() != 1) {
                addReport(newError(stmt, "Internal error: ASSIGN_STMT must have exactly one child (assigned expression)."));
                return null;
            }

            JmmNode valueNode = stmt.getChild(0);

            // Retrieve the expected type of the variable
            JmmNode methodNode = stmt.getAncestor(Kind.METHOD_DECL).orElse(null);
            String methodName = (methodNode != null) ? methodNode.get("name") : null;

            Type varType = table.getLocalVariables(methodName).stream()
                    .filter(var -> var.getName().equals(varName))
                    .map(Symbol::getType)
                    .findFirst()
                    .orElse(null);

            if (varType == null) {
                addReport(newError(stmt, "Variable " + varName + " not in scope."));
                return null;
            }

            // Get assigned expression type
            Type assignedType = typeUtils.getExprType(valueNode);

            // Handle varargs assignment
            if (assignedType != null) {
                // If assigned type is the Varargs class, get the type of the element (int)
                if (assignedType.getName().equals("Varargs")) {
                    assignedType = new Type("int", false); // The assigned type should be int, not Varargs
                }

                // Validate type compatibility
                if (!varType.equals(assignedType)) {
                    boolean bothAreClasses = isNotPrimitive(varType) && isNotPrimitive(assignedType);
                    if (bothAreClasses) {
                        if (!isAssignableTo(varType, assignedType, table)) {
                            addReport(newError(stmt, "Error: type " + assignedType.getName() + " cannot be assigned to " + varType.getName() + "."));
                        }
                        return null;
                    }

                    // Handle primitive type mismatch
                    if (varType.getName().equals("boolean") && assignedType.getName().equals("int")) {
                        addReport(newError(stmt, "Error: Cannot assign an integer to a boolean."));
                    } else {
                        addReport(newError(stmt, "Type mismatch: cannot assign "
                                + assignedType.getName() + " to " + varType.getName() + "."));
                    }
                }
            }

            return null;
        }

        // Check if the statement is a conditional expression (for if and while)
        if (stmtKind.equals(Kind.IF_STMT.getNodeName()) || stmtKind.equals(Kind.WHILE_STMT.getNodeName())) {
            Type conditionType = typeUtils.getExprType(stmt.getChild(0));

            if (conditionType == null || (!conditionType.isArray() && !conditionType.getName().equals("boolean"))) {
                addReport(newError(stmt, "Conditional expressions must return a boolean."));
            }
        }

        // While statements
        if (stmtKind.equals(Kind.WHILE_STMT.getNodeName())) {
            Type conditionType = typeUtils.getExprType(stmt.getChild(0));

            // Ensure the condition is boolean
            if (conditionType == null || !conditionType.getName().equals("boolean")) {
                addReport(newError(stmt, "While condition must be of boolean type."));
            }
        }

        // Block statement (set of statements)
        if (stmtKind.equals(Kind.BLOCK_STMT.getNodeName())) {
            for (JmmNode child : stmt.getChildren()) {
                visitStmt(child, table);
            }
        }

        return null;
    }

    /**
     * True if it is not primitive (int or boolean)
     */
    private boolean isNotPrimitive(Type type) {
        return !type.getName().equals("int") && !type.getName().equals("boolean");
    }

    /**
     * Checks if the assigned type is assignable to the variable's type.
     */
    private boolean isAssignableTo(Type varType, Type assignedType, SymbolTable table) {
        if (varType.equals(assignedType)) {
            return true;
        }

        String targetTypeName = varType.getName();
        String assignedTypeName = assignedType.getName();

        if (table.getImports().contains("[" + targetTypeName + "]") && table.getImports().contains("[" + assignedTypeName + "]")) {
            return true;
        }

        while (assignedTypeName != null) {
            if (assignedTypeName.equals(targetTypeName)) {
                return true;
            }

            assignedTypeName = table.getSuper();
        }
        return false;
    }

    /**
     * Handles primary expressions and validates their type and existence in the symbol table.
     */
    private Void visitPrimaryExpr(JmmNode primaryExpr, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);
        Type type = typeUtils.getExprType(primaryExpr);

        // Handle case where the type cannot be determined
        if (type == null) {
            addReport(newError(primaryExpr, "Unknown type for primary expression."));
        }

        // If it's a variable, check if it exists in the symbol table
        if (Kind.check(primaryExpr, Kind.IDENTIFIER)) {
            String varName = primaryExpr.get("name");
            JmmNode methodNode = primaryExpr.getAncestor(Kind.METHOD_DECL).orElse(null);
            String methodName = (methodNode != null) ? methodNode.get("name") : null;

            // Check if the variable exists in the local variables symbol table
            Type varType = null;
            if (methodName != null) {
                varType = table.getLocalVariables(methodName).stream()
                        .filter(var -> var.getName().equals(varName))
                        .map(Symbol::getType)
                        .findFirst()
                        .orElse(null);
            }

            if (varType == null) {
                addReport(newError(primaryExpr, "Variable " + varName + " not in scope."));
            }
        }

        return null;
    }

    /**
     * Validates the return type and expression type for method declarations.
     */
    private Void visitMethodDecl(JmmNode methodDecl, SymbolTable table) {

        if (methodDecl.getNumChildren() == 0) {
            return null;
        }

        TypeUtils typeUtils = new TypeUtils(table);
        String methodName = methodDecl.get("name");
        Type returnType = table.getReturnType(methodName);

        for (JmmNode child : methodDecl.getChildren()) {
            if (child.getKind().equals(Kind.STMT.getNodeName()) && child.getNumChildren() > 0) {
                JmmNode possibleReturnExpr = child.getChild(0);

                if (Kind.check(possibleReturnExpr, Kind.EXPR)) {
                    Type exprType = typeUtils.getExprType(possibleReturnExpr);

                    // Check if returnType or exprType is null and report the error
                    if (returnType == null || exprType == null) {
                        addReport(newError(possibleReturnExpr, "Void return type or expression."));
                    }
                    // Check if returnType does not match exprType
                    else if (!returnType.equals(exprType)) {
                        addReport(newError(possibleReturnExpr, "Type mismatch on return: expected " + returnType.getName() + " but found " + exprType.getName() + "."));
                    }
                }
            }
        }

        return null;
    }

    /**
     * Handles array literal expressions and checks if all elements are of the same type.
     */
    public Void visitArrayLiteral(JmmNode expr, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);
        Type firstElementType = typeUtils.getExprType(expr.getChild(0));

        if (firstElementType == null) {
            addReport(newError(expr, "Type of first element of literal array unknown."));
        }

        for (int i = 1; i < expr.getChildren().size(); i++) {
            JmmNode element = expr.getChild(i);
            Type elementType = typeUtils.getExprType(element);

            if (!elementType.equals(firstElementType)) {
                addReport(newError(element, "Array literal element has incompatible type."));
            }
        }
        return null;
    }

    /**
     * Handles unary expressions and validates the operand type (increment, decrement).
     */
    private Void visitUnaryExpr(JmmNode unaryExpr, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);

        // Check if this is an increment/decrement expression
        if (unaryExpr.getKind().equals("Increment")) {
            Type operandType = typeUtils.getExprType(unaryExpr.getChild(0));
            if (!operandType.getName().equals("int")) {
                addReport(newError(unaryExpr, "Increment/decrement operators require integer operands, but found: " + operandType.getName() + "."));
            }
            return null;
        }

        String op = unaryExpr.get("op");
        Type operandType = typeUtils.getExprType(unaryExpr.getChild(0));

        if (operandType == null) {
            addReport(newError(unaryExpr, "Unknown operand for unary expression."));
            return null;
        }

        if (op.equals("++") || op.equals("--")) {
            if (!operandType.getName().equals("int")) {
                addReport(newError(unaryExpr, "Increment/decrement operators require integer operands."));
            }
        } else {
            addReport(newError(unaryExpr, "Unknown unary operator: " + op));
        }

        return null;
    }
}
