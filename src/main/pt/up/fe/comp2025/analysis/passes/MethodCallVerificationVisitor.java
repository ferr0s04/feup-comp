package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;

public class MethodCallVerificationVisitor extends AnalysisVisitor {

    private String currentMethod;
    private boolean isStaticMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.IDENTIFIER, this::visitVarRefExpr);
        addVisit(Kind.METHOD_CALL, this::visitMethodCallExpr);
        addVisit(Kind.ARRAY_ACCESS, this::visitArrayAccessExpr);
    }

    /**
     * Stores the current method's name and whether it's static.
     */
    private Void visitMethodDecl(JmmNode methodNode, SymbolTable table) {
        currentMethod = methodNode.get("name"); // Method name
        isStaticMethod = Boolean.parseBoolean(methodNode.getOptional("isStatic").orElse("false"));
        return null;
    }

    /**
     * Checks if the variable reference is valid in the current context
     */
    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        String varName = varRefExpr.get("name");

        if ("this".equals(varName) && isStaticMethod) {
            addReport(newError(varRefExpr, "Cannot use 'this' in a static method."));
        } else if (!isDeclared(varName, table)) {
            addReport(newError(varRefExpr, "Variable '" + varName + "' is not declared."));
        }
        return null;
    }

    /**
     * Handles method call expressions by verifying different types of method calls: simple method call, array access, and more complex scenarios.
     */
    private Void visitMethodCallExpr(JmmNode methodCallExpr, SymbolTable table) {
        // Check for imported class methods
        if (methodCallExpr.getNumChildren() > 0) {
            JmmNode receiver = methodCallExpr.getChild(0);
            if (Kind.check(receiver, Kind.IDENTIFIER)) {
                String classOrVar = receiver.get("name");
                boolean imported = table.getImports().stream()
                        .anyMatch(imp ->
                                // full import
                                imp.equals(classOrVar) ||
                                        // import with package qualifier
                                        imp.endsWith("." + classOrVar)
                        );
                if (imported) {
                    return null;
                }
            }
        }

        // One child
        if (methodCallExpr.getChildren().size() == 1) {
            JmmNode identifierNode = methodCallExpr.getChildren().getFirst();

            String methodName;

            if (identifierNode.getKind().equals(Kind.THIS_REFERENCE.getNodeName())) {
                methodName = methodCallExpr.get("name");
            } else if (identifierNode.hasAttribute("name")) {
                methodName = identifierNode.get("name");
            } else {
                addReport(newError(methodCallExpr, "Invalid method call: missing method name."));
                return null;
            }

            if (isNotDeclaredMethod(methodName, table)) {
                addReport(newError(methodCallExpr, "Method '" + methodName + "' is not declared or accessible."));
                return null;
            }
        } else {
            // Method call with receiver and/or arguments
            JmmNode identifierNode = methodCallExpr.getChildren().stream()
                    .filter(child -> (child.getKind().equals(Kind.IDENTIFIER.getNodeName())
                            || child.getKind().equals(Kind.THIS_REFERENCE.getNodeName())))
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

        return null;
    }

    private Void visitArrayAccessExpr(JmmNode arrayAccessExpr, SymbolTable table) {
        if (arrayAccessExpr.getChildren().size() != 2) {
            addReport(newError(arrayAccessExpr, "Array access must have exactly two children: array and index."));
            return null;
        }

        JmmNode arrayExpr = arrayAccessExpr.getChildren().get(0);
        JmmNode indexExpr = arrayAccessExpr.getChildren().get(1);

        // Check if index is of integer type
        TypeUtils typeUtils = new TypeUtils(table);
        Type indexType = typeUtils.getExprType(indexExpr);
        if (!indexType.getName().equals("int")) {
            addReport(newError(arrayAccessExpr, "Array index must be of type 'int'"));
            return null;
        }

        // Check if array variable is declared and is of array type
        Type arrayExpressionType = typeUtils.getExprType(arrayExpr);
        if (!arrayExpressionType.isArray()) {
            addReport(newError(arrayAccessExpr, "Cannot access an index on a non-array type"));
            return null;
        }

        // For the case where array is a variable
        if (arrayExpr.getKind().equals(Kind.IDENTIFIER.getNodeName())) {
            String arrayVarName = arrayExpr.get("name");
            boolean foundVar = false;

            // Check in local variables
            for (Symbol localVar : table.getLocalVariables(currentMethod)) {
                if (localVar.getName().equals(arrayVarName)) {
                    foundVar = true;
                    if (!localVar.getType().isArray()) {
                        addReport(newError(arrayAccessExpr, "Variable '" + arrayVarName + "' is not an array."));
                    }
                    break;
                }
            }

            if (!foundVar) {
                addReport(newError(arrayAccessExpr, "Array variable '" + arrayVarName + "' is not declared."));
                return null;
            }
        }

        return null;
    }

    /**
     * Verify the correctness of method calls in various scenarios (simple method call / array access / complex method call / unexpected children)
     */
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

    /**
     * This function checks if a given variable is declared in the current method’s scope (local variables), in the class (fields), or in any imported classes.
     */
    private boolean isDeclared(String varName, SymbolTable table) {
        return table.getLocalVariables(currentMethod).stream().anyMatch(var -> var.getName().equals(varName)) ||
                table.getFields().stream().anyMatch(field -> field.getName().equals(varName)) ||
                table.getImports().stream().anyMatch(imported -> imported.contains(varName));
    }
}
