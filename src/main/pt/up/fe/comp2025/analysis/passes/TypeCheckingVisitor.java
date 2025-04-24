package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.Arrays;

public class TypeCheckingVisitor extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.BINARY_OP, this::visitBinaryExpr);
        addVisit(Kind.STMT, this::visitStmt);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.PRIMARY, this::visitPrimaryExpr);
        addVisit(Kind.ARRAY_LITERAL, this::visitArrayLiteral);
        addVisit(Kind.INCREMENT, this::visitUnaryExpr);
        addVisit(Kind.LENGTH_STMT, this::visitLengthCall);
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

        if (op.matches("[+]")){
            if (leftType.isArray() || rightType.isArray()) {
                // Handle case where one of the operands is an array
                addReport(newError(binaryExpr, "Cannot make this operations on an array."));
            } else {
                // Both operands must be integers for arithmetic operations
                if ((leftType.getName().equals("int") && rightType.getName().equals("int")) || (leftType.getName().equals("String") && rightType.getName().equals("String"))){
                    // good
                } else {
                    addReport(newError(binaryExpr, "Arithmetic operations require integer operands."));
                }
            }
        } else if (op.matches("[+*/-]|(\\+=|-=|\\*=|/=)|([<>]=)|[<>]")) { // For all this both left and right have to be int

            if (leftType.isArray() || rightType.isArray()) {
                // Handle case where one of the operands is an array
                addReport(newError(binaryExpr, "Cannot make this operations on an array."));
            } else {
                // Both operands must be integers for arithmetic operations
                if (!leftType.getName().equals("int") || !rightType.getName().equals("int")) {
                    addReport(newError(binaryExpr, "Arithmetic operations require integer operands."));
                }
            }

        } else if (op.matches("[=!]=?")) { // Left and right have to be the same type, but not one specific

            if (!leftType.getName().equals(rightType.getName())) {
                addReport(newError(binaryExpr, "Operators require operands of the same type."));
            }

        } else if (op.equals("&&") || op.equals("||")) { // Both left and right have to be boolean
            if (!leftType.getName().equals("boolean") || !rightType.getName().equals("boolean")) {
                addReport(newError(binaryExpr, "&& and || require boolean operands."));
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
                        // ---- IMPORTED‐CLASS BYPASS ----
                        boolean lhsImported = table.getImports().stream()
                                .anyMatch(imp -> imp.equals(varType.getName())
                                        || imp.endsWith("." + varType.getName()));
                        Type finalAssignedType = assignedType;
                        boolean rhsImported = table.getImports().stream()
                                .anyMatch(imp -> imp.equals(finalAssignedType.getName())
                                        || imp.endsWith("." + finalAssignedType.getName()));
                        if (lhsImported && rhsImported) {
                            // both sides are imported classes -> accept unconditionally
                            return null;
                        }
                        // otherwise fall back to your normal inheritance check
                        if (!isAssignableTo(varType, assignedType, table)) {
                            addReport(newError(stmt,
                                    "Error: type " + assignedType.getName() +
                                            " cannot be assigned to " + varType.getName() + "."));
                        }
                        return null;
                    }

                    // Handle primitive type mismatch
                    if (varType.getName().equals("boolean") && assignedType.getName().equals("int")) {
                        addReport(newError(stmt, "Error: Cannot assign an integer to a boolean."));
                    } else if (assignedType.getName().equals("length") && varType.getName().equals("int")) {
                        // Nada, está tudo fixolas
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
        // If it's not an identifier, we don't do the variable‐lookup logic here
        if (!Kind.check(primaryExpr, Kind.IDENTIFIER)) {
            return null;
        }

        String id = primaryExpr.get("name");
        String className = table.getClassName();
        String superName = table.getSuper();

        // 1) If it's the current class or its superclass, skip variable check
        if (id.equals(className) || (superName != null && id.equals(superName))) {
            return null;
        }

        // 2) If it's an imported type (either full or simple name), skip
        boolean imported = table.getImports().stream().anyMatch(imp -> {
            // imp might be "java.util.List" or just "List"
            String simple = imp.contains(".")
                    ? imp.substring(imp.lastIndexOf('.') + 1)
                    : imp;
            return imp.equals(id) || simple.equals(id);
        });
        if (imported) {
            return null;
        }

        // 3) Otherwise it really is a variable—look in locals, then params, then fields
        JmmNode methodNode = primaryExpr.getAncestor(Kind.METHOD_DECL).orElse(null);
        String methodName = methodNode != null ? methodNode.get("name") : null;

        Type varType = table.getLocalVariables(methodName).stream()
                .filter(v -> v.getName().equals(id))
                .map(Symbol::getType)
                .findFirst().orElse(null);

        if (varType == null) {
            varType = table.getParameters(methodName).stream()
                    .filter(p -> p.getName().equals(id))
                    .map(Symbol::getType)
                    .findFirst().orElse(null);
        }

        if (varType == null) {
            varType = table.getFields().stream()
                    .filter(f -> f.getName().equals(id))
                    .map(Symbol::getType)
                    .findFirst().orElse(null);
        }

        if (varType == null) {
            addReport(newError(primaryExpr, "Variable '" + id + "' is not declared."));
        }

        return null;
    }


    /**
     * Validates the return type and expression type for method declarations.
     */
    private Void visitMethodDecl(JmmNode methodDecl, SymbolTable table) {

        TypeUtils typeUtils = new TypeUtils(table);
        String methodName = methodDecl.get("name");
        Type returnType = table.getReturnType(methodName);

        boolean isMain = methodDecl.getBoolean("isMain", false);


        if (isMain) {
            if(!methodName.equals("main")){ // Verificar que se chama Main
                addReport(newError(methodDecl, "Static void needs to be named main."));
            }
            // Verificar que String[] args
            if ((!methodDecl.get("string").equals("String")) || (!methodDecl.get("args").equals("args"))) {
                addReport(newError(methodDecl, "Static void needs to have String[] args."));
            }

            // Verificar que não existe nenhum return startement
            for (JmmNode child : methodDecl.getChildren()) {
                if (child.getKind().equals(Kind.RETURN_STMT.getNodeName())) {
                    addReport(newError(child, "Static void main cannot have a return statement."));
                }
            }


        }

        // 1. Collect all PARAM children
        var params = methodDecl.getChildren().stream()
                .filter(Kind.PARAM::check)
                .toList();

        // 2. Check each for varargs (isArray) and ensure it's the last
        for (int i = 0; i < params.size(); i++) {
            JmmNode paramNode = params.get(i);
            // the type node is the first child of the PARAM
            JmmNode typeNode  = paramNode.getChild(0);
            boolean isArray   = typeNode.hasAttribute("isArray")
                    && Boolean.parseBoolean(typeNode.get("isArray"));

            if (isArray && i != params.size() - 1) {
                // report on the PARAM node (you could also get line/col from typeNode)
                addReport(newError(paramNode,
                        "Varargs parameter must be the last parameter in the list."));
            }
        }

        for (int i = 0; i < methodDecl.getNumChildren(); i++) {
            JmmNode child = methodDecl.getChild(i);

            if (child.getKind().equals(Kind.TYPE.getNodeName())) {
                continue;
            }

            if (child.getKind().equals(Kind.LITERAL.getNodeName()) ||
                    child.getKind().equals(Kind.EXPR.getNodeName()) ||
                    child.getKind().equals(Kind.BINARY_OP.getNodeName()) ||
                    child.getKind().equals(Kind.IDENTIFIER.getNodeName()) ||
                    child.getKind().equals(Kind.ARRAY_LITERAL.getNodeName())) {

                Type exprType = typeUtils.getExprType(child);

                if (returnType != null && exprType != null) {
                    if (returnType.isArray() != exprType.isArray()) {
                        if (returnType.isArray()) {
                            addReport(newError(child, "Cannot return non-array type " + exprType.getName() +
                                    " where array type " + returnType.getName() + "[] is expected."));
                        } else {
                            addReport(newError(child, "Cannot return array type " + exprType.getName() +
                                    "[] where non-array type " + returnType.getName() + " is expected."));
                        }
                        return null;
                    }

                    if (!returnType.equals(exprType)) {
                        boolean bothAreClasses = isNotPrimitive(returnType) && isNotPrimitive(exprType);
                        if (bothAreClasses) {
                            if (!isAssignableTo(returnType, exprType, table)) {
                                addReport(newError(child, "Incompatible return type: expected "
                                        + returnType.getName() + (returnType.isArray() ? "[]" : "")
                                        + " but found " + exprType.getName() + (exprType.isArray() ? "[]" : "") + "."));
                            }
                        } else {
                            addReport(newError(child, "Incompatible return type: expected "
                                    + returnType.getName() + (returnType.isArray() ? "[]" : "")
                                    + " but found " + exprType.getName() + (exprType.isArray() ? "[]" : "") + "."));
                        }
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

    private Void visitLengthCall(JmmNode lengthExpr, SymbolTable table) {
        // Get the target variable of the length operation (the variable before .length)
        if (lengthExpr.getNumChildren() == 0) {
            addReport(newError(lengthExpr, "Length operation missing target expression"));
            return null;
        }

        JmmNode targetExpr = lengthExpr.getChild(0);
        TypeUtils typeUtils = new TypeUtils(table);
        Type targetType = typeUtils.getExprType(targetExpr);

        if (targetType == null) {
            addReport(newError(lengthExpr, "Cannot determine type of expression for length operation"));
            return null;
        }

        // Check if the target type is array or String
        if (targetType.isArray()) {
            // This is valid - arrays have .length property
            return null;
        } else if (targetType.getName().equals("String")) {
            // This is valid - String has .length() method
            return null;
        } else {
            // Invalid type for length operation
            addReport(newError(lengthExpr, "Length operation is only valid for arrays and Strings, but got " +
                    targetType.getName()));
        }

        return null;
    }

}
