package pt.up.fe.comp2025.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pt.up.fe.comp2025.ast.Kind.*;

public class JmmSymbolTableBuilder {

    private List<Report> reports;
    private List<String> imports;
    private String className;
    private String superClass;
    private List<String> methods;
    private Map<String, Type> returnTypes;
    private Map<String, List<Symbol>> parameters;
    private Map<String, List<Symbol>> localVariables;
    private List<Symbol> fields;

    public JmmSymbolTableBuilder() {
        this.reports = new ArrayList<>();
        this.imports = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.returnTypes = new HashMap<>();
        this.parameters = new HashMap<>();
        this.localVariables = new HashMap<>();
        this.fields = new ArrayList<>();
    }

    public List<Report> getReports() {
        return reports;
    }

    private static Report newError(JmmNode node, String message) {
        return Report.newError(Stage.SEMANTIC, node.getLine(), node.getColumn(), message, null);
    }

    public JmmSymbolTable build(JmmNode root) {
        // Extract imports
        for (var importNode : root.getChildren(IMPORT_DECL)) {
            imports.add(extractImport(importNode));
        }

        // Find the class declaration (after imports)
        JmmNode classDecl = null;
        for (var child : root.getChildren()) {
            if (CLASS_DECL.check(child)) {
                classDecl = child;
                break;
            }
        }

        // Ensure a class declaration was found
        SpecsCheck.checkArgument(classDecl != null, () -> "Expected a class declaration but none found");

        this.className = classDecl.get("name");

        // Extract superclass (if any)
        if (classDecl.hasAttribute("extended")) {
            this.superClass = classDecl.get("extended");
        } else {
            this.superClass = null;
        }

        // Extract class fields (variables declared outside methods)
        for (var varDecl : classDecl.getChildren(VAR_DECL)) {
            fields.add(extractSymbol(varDecl));
        }

        // Extract methods
        for (var methodDecl : classDecl.getChildren(METHOD_DECL)) {
            processMethod(methodDecl);
        }

        return new JmmSymbolTable(className, superClass, imports, methods, returnTypes, parameters, localVariables, fields);
    }

    private void processMethod(JmmNode methodNode) {
        // Ensure methodNode is the method declaration
        if (methodNode.hasAttribute("name")) {
            var methodName = methodNode.get("name");
            methods.add(methodName);

            // Debugging: Print the number of children of the method node
            System.out.println("Method " + methodName + " has " + methodNode.getChildren().size() + " children.");

            // Extract return type (the first child should be the return type node)
            Type returnType = null;
            if (methodNode.getChildren().size() > 0) {
                var returnTypeNode = methodNode.getChild(0);  // This should be the return type node
                System.out.println("Method " + methodName + " return type node: " + returnTypeNode);
                System.out.println("Attributes: " + returnTypeNode.getAttributes());
                returnType = extractType(returnTypeNode);
                returnTypes.put(methodName, returnType);
            }

            // Extract parameters (iterate through children excluding the return type)
            var paramList = new ArrayList<Symbol>();
            int paramCount = 0;
            for (int i = 1; i < methodNode.getChildren().size(); i++) { // Start from index 1 to skip return type
                var childNode = methodNode.getChild(i);
                if (PARAM.check(childNode)) {
                    // Only count PARAM nodes as parameters
                    paramList.add(extractSymbol(childNode));
                    paramCount++;
                }
            }
            parameters.put(methodName, paramList);
            System.out.println("Method " + methodName + " has " + paramCount + " parameters.");

            // Extract local variables (e.g., variables declared in the method body)
            var localVars = new ArrayList<Symbol>();
            for (var varNode : methodNode.getChildren(VAR_DECL)) {
                // Debugging: Print each local variable node
                System.out.println("Local variable node: " + varNode);
                localVars.add(extractSymbol(varNode));
            }
            localVariables.put(methodName, localVars);
        } else {
            System.out.println("No method name found in node: " + methodNode);
        }
    }



    private Type extractType(JmmNode typeNode) {
        // Check if the node represents a custom type (like a class or object)
        if (typeNode.hasAttribute("name")) {
            // Custom type (like a class or object)
            String typeName = typeNode.get("name");  // Get the name for custom types (e.g., class name)
            boolean isArray = typeNode.hasAttribute("isArray") && typeNode.get("isArray").equals("true");
            return new Type(typeName, isArray);  // Return custom type with array information
        } else if (typeNode.hasAttribute("value")) {
            // Primitive types (like INT, BOOLEAN)
            String typeName = typeNode.get("value");  // Get the type value for primitive types
            boolean isArray = typeNode.hasAttribute("isArray") && typeNode.get("isArray").equals("true");
            return new Type(typeName, isArray);  // Return primitive type with array information
        } else {
            // Handle cases where the type is unexpected or not handled correctly
            throw new IllegalArgumentException("Unexpected type node structure: " + typeNode);
        }
    }


    private Symbol extractSymbol(JmmNode node) {
        Type type = extractType(node.getChild(0));
        String name = node.get("name");
        return new Symbol(type, name);
    }

    private String extractImport(JmmNode importNode) {
        StringBuilder importPath = new StringBuilder(importNode.get("name"));

        for (var part : importNode.getChildren()) {
            importPath.append(".").append(part.get("name"));
        }

        return importPath.toString();
    }
}
