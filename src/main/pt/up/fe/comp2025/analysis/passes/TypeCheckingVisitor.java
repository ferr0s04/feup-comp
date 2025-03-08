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
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);
        String op = binaryExpr.get("op");

        Type leftType = typeUtils.getExprType(binaryExpr.getChild(0));
        Type rightType = typeUtils.getExprType(binaryExpr.getChild(1));

        if (op.matches("[+\\\\*/]")) {
            if (!leftType.getName().equals("int") || !rightType.getName().equals("int")) {
                addReport(newError(binaryExpr, "Operações aritméticas requerem operandos inteiros."));
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
        JmmNode firstChild = stmt.getChild(0);

        if (Kind.check(firstChild, Kind.IDENTIFIER)) {
            String varName = firstChild.get("name");
            JmmNode methodNode = stmt.getAncestor(Kind.METHOD_DECL).orElse(null);
            String methodName = (methodNode != null) ? methodNode.get("name") : null;

            Type varType = table.getLocalVariables(methodName).stream()
                    .filter(var -> var.getName().equals(varName))
                    .map(Symbol::getType)
                    .findFirst()
                    .orElse(null);

            Type assignedType = typeUtils.getExprType(stmt.getChild(1));

            if (varType != null && !varType.equals(assignedType)) {
                addReport(newError(stmt, "Incompatibilidade de tipos: não é possível atribuir " + assignedType.getName() + " a " + varType.getName() + "."));
            }
        }

        if (stmt.getKind().equals(Kind.STMT.getNodeName()) && stmt.getNumChildren() > 0) {
            Type conditionType = typeUtils.getExprType(stmt.getChild(0));

            if (!conditionType.getName().equals("boolean")) {
                addReport(newError(stmt, "Expressões condicionais devem retornar um booleano."));
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

                    if (!returnType.equals(exprType)) {
                        addReport(newError(possibleReturnExpr, "Incompatibilidade de tipo no retorno: esperado " + returnType.getName() + " mas encontrado " + exprType.getName() + "."));
                    }
                }
            }
        }

        return null;
    }
}