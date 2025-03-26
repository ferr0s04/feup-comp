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
        // One child
        if (methodCallExpr.getChildren().size() == 1 && methodCallExpr.getChildren().get(0).getKind().equals(Kind.IDENTIFIER.getNodeName())) {
            JmmNode identifierNode = methodCallExpr.getChildren().getFirst();

            String methodName = identifierNode.get("name");

            // Verify if the method is declared
            if (isNotDeclaredMethod(methodName, table)) {
                addReport(newError(methodCallExpr, "Method '" + methodName + "' is not declared or accessible."));
            }
        }
        // Two children
        else if (methodCallExpr.getChildren().size() == 2) {
            JmmNode arrayExpr = methodCallExpr.getChildren().get(0); // First child
            JmmNode indexExpr = methodCallExpr.getChildren().get(1); // Second child

            // Array access
            if (!indexExpr.getKind().equals(Kind.IDENTIFIER.getNodeName())) {
                String arrayVarName = arrayExpr.get("name");
                String arrayType = null;

                for (Symbol localVar : table.getLocalVariables(methodCallExpr.getParent().get("name"))) {
                    if (localVar.getName().equals(arrayVarName)) {
                        arrayType = String.valueOf(localVar.getType());
                        break;
                    }
                }

                if (arrayType == null) {
                    addReport(newError(methodCallExpr, "Array variable '" + arrayVarName + "' is not declared."));
                    return null;
                }

                // Check if it's an array type
                if (!arrayType.contains("isArray=true")) {
                    addReport(newError(methodCallExpr, "Cannot access an index on a non-array type: " + arrayType));
                    return null;
                }

                boolean isInt = arrayType.contains("name=int");
                if (!isInt) {
                    addReport(newError(methodCallExpr, "Array index must be of type 'int'"));
                }
            } else {
                // Method Call
                JmmNode identifierNode = methodCallExpr.getChildren().stream()
                        .filter(child -> (child.getKind().equals(Kind.IDENTIFIER.getNodeName()) || child.getKind().equals(Kind.THIS_REFERENCE.getNodeName())))
                        .findFirst()
                        .orElse(null);

                if (identifierNode == null) {
                    addReport(newError(methodCallExpr, "Method call is missing an identifier or this_reference."));
                    return null;
                }

                String methodName;
                if (identifierNode.getKind().equals(Kind.THIS_REFERENCE.getNodeName())) {
                    methodName = methodCallExpr.get("name");
                } else {
                    methodName = identifierNode.get("name");
                }

                // Verify if the method is declared
                if (isNotDeclaredMethod(methodName, table)) {
                    addReport(newError(methodCallExpr, "Method '" + methodName + "' is not declared or accessible."));
                    return null;
                }
            }
        }
        // Method Call
        else if (methodCallExpr.getChildren().size() > 2) {
            JmmNode identifierNode = methodCallExpr.getChildren().stream()
                    .filter(child -> (child.getKind().equals(Kind.IDENTIFIER.getNodeName()) || child.getKind().equals(Kind.THIS_REFERENCE.getNodeName())))
                    .findFirst()
                    .orElse(null);

            if (identifierNode == null) {
                addReport(newError(methodCallExpr, "Method call is missing an identifier or this_reference."));
                return null;
            }

            String methodName;
            if (identifierNode.getKind().equals(Kind.THIS_REFERENCE.getNodeName())) {
                // 'this' reference
                methodName = methodCallExpr.get("name");
            } else {
                methodName = identifierNode.get("name");
            }

            // Verify if the method is declared
            if (isNotDeclaredMethod(methodName, table)) {
                addReport(newError(methodCallExpr, "Method '" + methodName + "' is not declared or accessible."));
                return null;
            }
        }
        else {
            addReport(newError(methodCallExpr, "Unexpected number of children for method call."));
        }

        return null;
    }

    private boolean isNotDeclaredMethod(String methodName, SymbolTable table) {
        // Check if the method exists in the current class
        boolean methodDeclared = table.getMethods().stream()
                .anyMatch(method -> method.equals(methodName));

        if (methodDeclared) {
            return false;
        }

        for (String importedClass : table.getImports()) {
            String className = importedClass.replace("[", "").replace("]", "");

            for (Symbol symbol : table.getLocalVariables(currentMethod)) {
                String variableType = symbol.getType().getName();

                if (className.equals(variableType) || className.equals(table.getSuper())) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isDeclared(String varName, SymbolTable table) {
        return table.getLocalVariables(currentMethod).stream().anyMatch(var -> var.getName().equals(varName)) ||
                table.getFields().stream().anyMatch(field -> field.getName().equals(varName)) ||
                table.getImports().stream().anyMatch(imported -> imported.contains(varName));
    }
}
