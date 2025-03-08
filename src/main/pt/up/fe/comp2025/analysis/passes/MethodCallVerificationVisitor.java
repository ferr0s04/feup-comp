package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;

import java.util.List;

public class MethodCallVerificationVisitor extends AnalysisVisitor {

    private String currentMethod;
    private boolean isStaticMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.IDENTIFIER, this::visitVarRefExpr);
        addVisit(Kind.ACCESS_OR_CALL, this::visitMethodCallExpr);  // Fixed method call kind
    }

    private Void visitMethodDecl(JmmNode methodNode, SymbolTable table) {
        currentMethod = methodNode.get("name"); // Method name
        isStaticMethod = Boolean.parseBoolean(methodNode.getOptional("isStatic").orElse("false"));
        return null;
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        String varName = varRefExpr.get("name");

        if ("this".equals(varName) && isStaticMethod) {
            addReport(newError(varRefExpr, "Cannot use 'this' in a static method."));
        } else if (!isDeclared(varName, table)) {
            addReport(newError(varRefExpr, "Variable '" + varName + "' is not declared."));
        }
        return null;
    }

    private Void visitMethodCallExpr(JmmNode methodCallExpr, SymbolTable table) {
        // Ensure it's actually a method call
        Kind.checkOrThrow(methodCallExpr, Kind.ACCESS_OR_CALL);

        // Retrieve the method name (assuming it's stored in an IDENTIFIER child)
        JmmNode identifierNode = methodCallExpr.getChildren().stream()
                .filter(child -> child.getKind().equals(Kind.IDENTIFIER.getNodeName()))
                .findFirst()
                .orElse(null);

        if (identifierNode == null) {
            addReport(newError(methodCallExpr, "Method call is missing an identifier."));
            return null;
        }

        String methodName = identifierNode.get("name");

        // Verify if the method is declared
        if (!isDeclaredMethod(methodName, table)) {
            addReport(newError(methodCallExpr, "Method '" + methodName + "' is not declared or accessible."));
            return null;
        }

        // Retrieve arguments
        List<JmmNode> arguments = methodCallExpr.getChildren().stream()
                .filter(child -> child.getKind().equals(Kind.EXPR.getNodeName()))
                .toList();

        int expectedArgCount = getExpectedArgumentCount(methodName, table);

        if (arguments.size() != expectedArgCount) {
            addReport(newError(methodCallExpr, "Method '" + methodName + "' expects " + expectedArgCount + " arguments, but got " + arguments.size() + "."));
        }

        // Type checking for arguments
        for (int i = 0; i < arguments.size(); i++) {
            Type expectedType = getExpectedArgumentType(methodName, i, table);
            String argType = arguments.get(i).get("type");

            if (!argType.equals(expectedType.getName())) {
                addReport(newError(methodCallExpr, "Argument type mismatch for parameter " + (i + 1) + " in method '" + methodName + "'. Expected: " + expectedType + ", found: " + argType + "."));
            }
        }

        return null;
    }

    private boolean isDeclaredMethod(String methodName, SymbolTable table) {
        return table.getMethods().contains(methodName);
    }

    private int getExpectedArgumentCount(String methodName, SymbolTable table) {
        return table.getParameters(methodName).size();
    }

    private Type getExpectedArgumentType(String methodName, int index, SymbolTable table) {
        List<Type> paramTypes = table.getParameters(methodName)
                .stream()
                .map(Symbol::getType) // Assuming each parameter has a 'getType' method
                .toList();
        if (index >= paramTypes.size()) {
            throw new IllegalArgumentException("Invalid parameter index for method: " + methodName);
        }
        return paramTypes.get(index);
    }

    private boolean isDeclared(String varName, SymbolTable table) {
        return table.getLocalVariables(currentMethod).stream().anyMatch(var -> var.getName().equals(varName)) ||
                table.getFields().stream().anyMatch(field -> field.getName().equals(varName)) ||
                table.getImports().contains(varName);
    }
}
