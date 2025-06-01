package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MethodCallVerificationVisitor extends AnalysisVisitor {

    private String currentMethod;
    private boolean isStaticMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.IDENTIFIER, this::visitVarRefExpr);
        addVisit(Kind.METHOD_CALL, this::visitMethodCallExpr);
        addVisit(Kind.ARRAY_ACCESS, this::visitArrayAccessExpr);
        addVisit(Kind.IMPORT_DECL, this::visitImportDecl);
        addVisit(Kind.THIS_REFERENCE, this::visitThisReference);
    }

    private Void visitThisReference(JmmNode thisRef, SymbolTable table) {
        // Check if 'this' is used in a static method
        // go back to method declaration to check if it is main
        String a = thisRef.getAttributes().toString();
        JmmNode current = thisRef;
        while (current != null && !current.getKind().equals("MethodDecl")) {
            current = current.getParent();
        }
        if (current.get("isMain").equals("true")) {
            addReport(newError(thisRef, "Found 'this' inside static method"));
        }
        return null;
    }


    private Void visitImportDecl(JmmNode methodNode, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);
        if(typeUtils.hasDoubleImports()){
            addReport(newError(methodNode, "Double import with same name detected."));
            return null;
        }
        return null;
    }

    /**
     * Stores the current method's name and whether it's static.
     */
    private Void visitMethodDecl(JmmNode methodNode, SymbolTable table) {
        if (methodNode.hasAttribute("isWrong") && methodNode.getBoolean("isWrong", false)) {
            addReport(newError(methodNode, "Method declaration has something wrong."));
            return null;
        }
        currentMethod = methodNode.get("name"); // Method name
        isStaticMethod = Boolean.parseBoolean(methodNode.getOptional("isStatic").orElse("false"));
        return null;
    }

    /**
     * Checks if the variable reference is valid in the current context
     */
    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        String varName = varRefExpr.get("name");
        JmmNode parent = varRefExpr.getParent();

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
        TypeUtils typeutils = new TypeUtils(table);
        // Check for imported class methods
        if (methodCallExpr.getNumChildren() > 0) {
            JmmNode receiver = methodCallExpr.getChild(0);
            if (Kind.check(receiver, Kind.IDENTIFIER)) {
                String classOrVar = receiver.get("name");
                Type typeclassOrVar = typeutils.getExprType(receiver);;
                boolean imported = typeutils.isImported(classOrVar) || typeutils.isImported(typeclassOrVar);
                System.out.println("Imported: " + imported);
                if (imported) {
                    return null;
                }
            }
        }

        String methodName = null;
        JmmNode receiver;
        List<JmmNode> arguments = new ArrayList<>();

        // Parse method call structure
        if (methodCallExpr.getChildren().size() == 1) {
            receiver = null;
            JmmNode identifierNode = methodCallExpr.getChildren().getFirst();

            if (identifierNode.getKind().equals(Kind.THIS_REFERENCE.getNodeName())) {
                methodName = methodCallExpr.get("name");
            } else if (methodCallExpr.hasAttribute("name")) {
                methodName = methodCallExpr.get("name");
            } else if (identifierNode.hasAttribute("name")) {
                methodName = identifierNode.get("name");
            } else {
                addReport(newError(methodCallExpr, "Invalid method call: missing method name."));
                return null;
            }
        } else {
            // Method call with receiver and/or arguments
            receiver = methodCallExpr.getChildren().stream()
                    .filter(child -> (child.getKind().equals(Kind.IDENTIFIER.getNodeName())
                            || child.getKind().equals(Kind.THIS_REFERENCE.getNodeName())))
                    .findFirst()
                    .orElse(null);

            if (receiver == null) {
                addReport(newError(methodCallExpr, "Method call is missing an identifier or this_reference."));
                return null;
            }

            // Get method name
            if (receiver.getKind().equals(Kind.THIS_REFERENCE.getNodeName())) {
                methodName = methodCallExpr.get("name");
            } else if (methodCallExpr.hasAttribute("name")) {
                methodName = methodCallExpr.get("name");
            } else {
                methodName = receiver.get("name");
            }

            // Collect arguments (all children that are not the receiver)
            arguments = methodCallExpr.getChildren().stream()
                    .filter(child -> !child.equals(receiver))
                    .collect(Collectors.toList());
        }

        // Verify if the method is declared
        if (isNotDeclaredMethod(methodName, table)) {
            addReport(newError(methodCallExpr, "Method '" + methodName + "' is not declared or accessible."));
            return null;
        }


        // Get the method declaration to check parameter types
        List<Symbol> methodParams = table.getParameters(methodName);

        boolean isVarargs;
        if(!methodParams.isEmpty()) {
            isVarargs = (methodParams.getLast().getType().hasAttribute("isVarargs") && methodParams.getLast().getType().get("isVarargs").equals("true"));
        } else {
            isVarargs = false;
        }


        if(isVarargs){
            if(methodParams.size() > 1){ // has others besides the varargs
                for (int i = 0; i < methodParams.size(); i++) {
                    Symbol param = methodParams.get(i);
                    if(param.getType().hasAttribute("isVarargs") && param.getType().get("isVarargs").equals("true")){
                        List<JmmNode> prov = arguments.subList(i, arguments.size());
                        for (JmmNode a : prov){
                            if(!typeutils.getExprType(a).getName().equals("int")){
                                addReport(newError(methodCallExpr,
                                        "Method '" + methodName + "' expects int.. but got " + typeutils.getExprType(a).getName()));
                                return null;
                            }
                        }
                        return null;
                    }
                    JmmNode argument = arguments.get(i);
                    Type actualType = typeutils.getExprType(argument);

                    if (!isTypeCompatible(actualType, param.getType())) {
                        addReport(newError(argument,
                                "Argument " + (i + 1) + " of method '" + methodName +
                                        "' expects type '" + param.getType().getName() +
                                        "' but got '" + actualType.getName() + "'"));
                        return null;
                    }

                }
            } else {
                for (JmmNode a : arguments){
                    if(!typeutils.getExprType(a).getName().equals("int")){
                        addReport(newError(methodCallExpr,
                                "Method '" + methodName + "' expects int.. but got " + typeutils.getExprType(a).getName()));
                        return null;
                    }
                }
            }
            return null;
        }

        if (methodParams.size() != arguments.size()) {
            addReport(newError(methodCallExpr,
                    "Method '" + methodName + "' expects " + methodParams.size() +
                            " arguments but got " + arguments.size()));
            return null;
        }


        // Check each argument type against expected parameter type
        for (int i = 0; i < arguments.size(); i++) {
            JmmNode argument = arguments.get(i);
            Symbol expectedParam = methodParams.get(i);

            Type actualType = typeutils.getExprType(argument);
            Type expectedType = expectedParam.getType();


            if (!isTypeCompatible(actualType, expectedType)) {
                addReport(newError(argument,
                        "Argument " + (i + 1) + " of method '" + methodName +
                                "' expects type '" + expectedType.getName() +
                                "' but got '" + actualType.getName() + "'"));
            }
        }

        return null;
    }

    // Helper method to check type compatibility
    private boolean isTypeCompatible(Type actual, Type expected) {
        if (actual.getName().equals(expected.getName()) &&
                actual.isArray() == expected.isArray()) {
            return true;
        }

        // Add inheritance/subtyping rules here if needed - TODO
        // For now, exact match required
        return false;
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

            // Check in parameters
            for (Symbol parameter : table.getParameters(currentMethod)) {
                if (parameter.getName().equals(arrayVarName)) {
                    foundVar = true;
                    if (!parameter.getType().isArray()) {
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
        TypeUtils typeUtils = new TypeUtils(table);
        return table.getLocalVariables(currentMethod).stream().anyMatch(var -> var.getName().equals(varName)) ||
                table.getParameters(currentMethod).stream().anyMatch(var -> var.getName().equals(varName)) ||
                table.getFields().stream().anyMatch(field -> field.getName().equals(varName)) ||
                typeUtils.isImported(varName);
    }
}
