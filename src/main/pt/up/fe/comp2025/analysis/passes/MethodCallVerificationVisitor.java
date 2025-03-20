package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;


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
        // Check if it's an array access
        if (methodCallExpr.getChildren().size() == 2) {
            JmmNode arrayExpr = methodCallExpr.getChildren().get(0);
            JmmNode indexExpr = methodCallExpr.getChildren().get(1);

            String arrayVarName = arrayExpr.get("name");

            // Retrieve the type of the array variable from the symbol table
            String arrayType = null;
            for (Symbol field : table.getFields()) {
                if (field.getName().equals(arrayVarName)) {
                    arrayType = String.valueOf(field.getType());
                    break;
                }
            }

            if (arrayType == null) {
                addReport(newError(methodCallExpr, "Array variable '" + arrayVarName + "' is not declared."));
                return null;
            }

            // Check if it's an array type
            if (!arrayType.endsWith("[]")) {
                addReport(newError(methodCallExpr, "Cannot access an index on a non-array type: " + arrayType));
                return null;
            }

            // Type checking: Ensure the index is of type int
            String indexType = indexExpr.get("name");
            if (!"int".equals(indexType)) {
                addReport(newError(methodCallExpr, "Array index must be of type 'int', found: " + indexType + "."));
            }
        } else {
            // Otherwise, treat it as a method call (single identifier child)
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

            // Perform further argument validation (as before)
        }

        return null;
    }

    private boolean isDeclaredMethod(String methodName, SymbolTable table) {
        boolean methodDeclared = table.getMethods().stream()
                .anyMatch(method -> method.equals(methodName));
        if (methodDeclared) {
            return true;
        }
        String parentClass = table.getSuper();
        return parentClass != null;
    }

    private boolean isDeclared(String varName, SymbolTable table) {
        return table.getLocalVariables(currentMethod).stream().anyMatch(var -> var.getName().equals(varName)) ||
                table.getFields().stream().anyMatch(field -> field.getName().equals(varName)) ||
                table.getImports().contains(varName);
    }
}
