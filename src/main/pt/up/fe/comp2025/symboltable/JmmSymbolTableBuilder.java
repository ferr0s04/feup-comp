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
    private final TypeUtils typeUtils;

    public JmmSymbolTableBuilder() {
        this.reports = new ArrayList<>();
        this.imports = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.returnTypes = new HashMap<>();
        this.parameters = new HashMap<>();
        this.localVariables = new HashMap<>();
        this.fields = new ArrayList<>();
        this.typeUtils = new TypeUtils(new JmmSymbolTable(className, superClass, imports, methods, returnTypes, parameters, localVariables, fields));
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

        // Find the class declaration
        JmmNode classDecl = null;
        for (var child : root.getChildren()) {
            if (CLASS_DECL.check(child)) {
                classDecl = child;
                break;
            }
        }

        SpecsCheck.checkArgument(classDecl != null, () -> "Expected a class declaration but none found");

        this.className = classDecl.get("name");

        // Extract superclass (if any)
        this.superClass = classDecl.getOptional("extended").orElse(null);

        // Extract class fields
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
        if (methodNode.hasAttribute("name")) {
            var methodName = methodNode.get("name");
            methods.add(methodName);

            // Extract return type (it could be in the first child or elsewhere)
            Type returnType = null;
            var returnTypeNode = methodNode.getChildren().stream()
                    .filter(child -> TYPE.check(child)) // assuming TYPE is the rule for return types
                    .findFirst().orElse(null);

            if (returnTypeNode != null) {
                returnType = extractType(returnTypeNode);
                returnTypes.put(methodName, returnType);
            } else {
                // Handle missing return type (perhaps void or error)
                returnTypes.put(methodName, new Type("void", false));  // Assuming void if no return type
            }

            // Extract parameters
            var paramList = new ArrayList<Symbol>();
            for (int i = 1; i < methodNode.getChildren().size(); i++) {
                var childNode = methodNode.getChild(i);
                if (PARAM.check(childNode)) {
                    paramList.add(extractSymbol(childNode));
                }
            }

            // Extract local variables
            var localVars = new ArrayList<Symbol>();
            for (var varNode : methodNode.getChildren(VAR_DECL)) {
                localVars.add(extractSymbol(varNode));
            }

            // Merge parameters and local variables into the method's scope
            var allVars = new ArrayList<>(paramList);
            allVars.addAll(localVars);

            // Add to symbol table maps
            parameters.put(methodName, paramList);
            localVariables.put(methodName, allVars);
        } else {

        }
    }

    private Type extractType(JmmNode typeNode) {
        if (!typeNode.hasAttribute("name")) {
            throw new IllegalArgumentException("Type node is missing 'name' attribute: " + typeNode);
        }

        String typeName = typeNode.get("name");
        boolean isArray = typeNode.hasAttribute("isArray") && typeNode.get("isArray").equals("true");

        Type type = new Type(typeName, isArray);

        boolean imported = typeUtils.isImported(typeName);
        type.putObject("isImported", imported);
        type.putObject("isStatic", imported);
        return type;
    }

    private Symbol extractSymbol(JmmNode node) {
        if (node.getNumChildren() == 0) {
            throw new IllegalArgumentException("Variable declaration node has no children.");
        }
        // Ensure the first child is a valid type node
        JmmNode typeNode = node.getChild(0);
        Type type = extractType(typeNode);

        String name = node.get("name");
        return new Symbol(type, name);
    }

    private String extractImport(JmmNode importNode) {
        // Get the import name directly from the node's children (ID tokens)
        StringBuilder importName = new StringBuilder();
        importName.append(importNode.get("name").replaceAll("[\\[\\]\\s]", ""));
        return importName.toString();
    }

}
