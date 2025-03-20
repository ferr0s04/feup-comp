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
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);
        String op = binaryExpr.get("op");

        // Ensure the variables involved in the binary expression are declared
        Type leftType = typeUtils.getExprType(binaryExpr.getChild(0));
        Type rightType = typeUtils.getExprType(binaryExpr.getChild(1));

        if (leftType == null || rightType == null) {
            addReport(newError(binaryExpr, "Uma ou ambas as variáveis são desconhecidas."));
            return null;
        }

        if (op.matches("[+\\\\*/]")) {
            if (leftType.isArray() || rightType.isArray()) {
                // Handle case where one of the operands is an array
                addReport(newError(binaryExpr, "Não é possível somar um array com um tipo primitivo."));
            } else {
                // Both operands must be integers for arithmetic operations
                if (!leftType.getName().equals("int") || !rightType.getName().equals("int")) {
                    addReport(newError(binaryExpr, "Operações aritméticas requerem operandos inteiros."));
                }
            }
        } else if (op.matches("[<>=!]") || op.equals("&&")) {
            if (!leftType.getName().equals(rightType.getName())) {
                addReport(newError(binaryExpr, "Operadores requerem operandos do mesmo tipo."));
            }
        }

        return null;
    }

    private Void visitStmt(JmmNode stmt, SymbolTable table) {
        if (stmt.getNumChildren() == 0) {
            return null;
        }

        TypeUtils typeUtils = new TypeUtils(table);
        String stmtKind = stmt.getKind();

        // Check for assignment statements (e.g., x = y)
        if (stmtKind.equals(Kind.ASSIGNMENT.getNodeName())) {
            JmmNode firstChild = stmt.getChild(0);
            if (Kind.check(firstChild, Kind.IDENTIFIER)) {
                String varName = firstChild.get("name");
                JmmNode methodNode = stmt.getAncestor(Kind.METHOD_DECL).orElse(null);
                String methodName = (methodNode != null) ? methodNode.get("name") : null;

                // Check if the variable exists in the local variables symbol table
                Type varType = table.getLocalVariables(methodName).stream()
                        .filter(var -> var.getName().equals(varName))
                        .map(Symbol::getType)
                        .findFirst()
                        .orElse(null);

                if (varType == null) {
                    addReport(newError(stmt, "Variável " + varName + " não declarada no escopo."));
                    return null;
                }

                if (stmt.getNumChildren() > 1) {
                    Type assignedType = typeUtils.getExprType(stmt.getChild(1));

                    // Check if the assigned type matches the variable's type
                    if (assignedType != null && !varType.equals(assignedType)) {
                        addReport(newError(stmt, "Incompatibilidade de tipos: não é possível atribuir " + assignedType.getName() + " a " + varType.getName() + "."));
                    }
                } else {
                    addReport(newError(stmt, "A atribuição está mal formada, faltando o valor para a variável."));
                }
            }
        }

        // Check if the statement is a conditional expression (for if and while)
        if (stmtKind.equals(Kind.IF_STMT.getNodeName()) || stmtKind.equals(Kind.WHILE_STMT.getNodeName())) {
            Type conditionType = typeUtils.getExprType(stmt.getChild(0));

            if (conditionType == null || (!conditionType.isArray() && !conditionType.getName().equals("boolean"))) {
                addReport(newError(stmt, "Expressões condicionais devem retornar um booleano."));
            }
        }

        // Special handling for WHILE statement condition check
        if (stmtKind.equals(Kind.WHILE_STMT.getNodeName())) {
            Type conditionType = typeUtils.getExprType(stmt.getChild(0));

            // Ensure the condition is boolean
            if (conditionType == null || !conditionType.getName().equals("boolean")) {
                addReport(newError(stmt, "Condição do while deve ser do tipo booleano."));
            }
        }

        // Handle block statement: block is just a set of statements
        if (stmtKind.equals(Kind.BLOCK_STMT.getNodeName())) {
            for (JmmNode child : stmt.getChildren()) {
                visitStmt(child, table); // Recursively visit statements inside the block
            }
        }

        return null;
    }

    private Void visitPrimaryExpr(JmmNode primaryExpr, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);
        Type type = typeUtils.getExprType(primaryExpr);

        // Handle case where the type cannot be determined
        if (type == null) {
            addReport(newError(primaryExpr, "Tipo desconhecido para expressão primária."));
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
                addReport(newError(primaryExpr, "Variável " + varName + " não declarada no escopo."));
            }
        }

        return null;
    }

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
                        addReport(newError(possibleReturnExpr, "Tipo de retorno ou expressão nulo."));
                    }
                    // Check if returnType does not match exprType
                    else if (!returnType.equals(exprType)) {
                        addReport(newError(possibleReturnExpr, "Incompatibilidade de tipo no retorno: esperado " + returnType.getName() + " mas encontrado " + exprType.getName() + "."));
                    }
                }
            }
        }

        return null;
    }

}
