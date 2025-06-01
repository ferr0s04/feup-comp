package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;

import java.util.HashSet;
import java.util.Set;

public class IdentifierAndDeclarationVerification extends AnalysisVisitor {

    private String currentMethod;
    private boolean isStaticMethod;
    private String className;

    @Override
    public void buildVisitor() {
        addVisit(Kind.CLASS_DECL, this::visitClassDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.IDENTIFIER, this::visitVarRefExpr);
    }

    /**
     * Stores the class name from the class declaration node for future reference.
     */
    private Void visitClassDecl(JmmNode classNode, SymbolTable table) {
        className = classNode.get("name");

        Set<String> fields = new HashSet<>();
        for (Symbol field : table.getFields()) {
            if (field.getType().isArray()) {
                // If the parameter is an array, it should int
                boolean typeIsVarargs = Boolean.parseBoolean(field.getType().get("isVarargs"));
                if( typeIsVarargs ) {
                    addReport(newError(classNode, "Varargs not allowed outside method parameters."));
                    return null;
                }
                if(!field.getType().getName().equals("int")) {
                    addReport(newError(classNode, "Array must be of type int."));
                    return null;
                }
            }
            System.out.println("Field: " + field.getName());
            if (!fields.add(field.getName())) {
                addReport(newError(classNode, "Duplicate field: " + field.getName()));
                return null;
            }
        }

        return null;
    }

    /**
     * Stores the current method name and static flag when visiting method declarations.
     */
    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        if (method.hasAttribute("isWrong") && method.getBoolean("isWrong", false)) {
            addReport(newError(method, "Method declaration has something wrong."));
            return null;
        }
        currentMethod = method.get("name");
        isStaticMethod = method.getOptional("isStatic").map(Boolean::parseBoolean).orElse(false);
        if(!(method.getBoolean("isMain", false))) {
            if (method.getChild(0).hasAttribute("isArray")) {
                String typeName = method.getChild(0).get("name");
                boolean typeIsArray = Boolean.parseBoolean(method.getChildren().getFirst().get("isArray"));
                boolean typeIsVarargs = Boolean.parseBoolean(method.getChildren().getFirst().get("isVarargs"));
                if (typeIsVarargs) {
                    addReport(newError(method, "Varargs not allowed outside method parameters."));
                    return null;
                }
                if (typeIsArray) {
                    if (!typeName.equals("int")) {
                        addReport(newError(method, "Array must be of type int."));
                        return null;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Checks if a variable is declared and checks for "this" usage in static methods.
     */
    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        var varName = varRefExpr.get("name");

        if ("this".equals(varName)) {
            if (isStaticMethod) {
                addReport(newError(varRefExpr, "Cannot use 'this' in a static method."));
            }
            return null;
        }

        if (!isDeclared(varName, table)) {
            addReport(newError(varRefExpr, "Variable '" + varName + "' is not declared."));
        }

        return null;
    }

    /**
     * Checks if a variable is declared in the method's parameters, local variables, or fields.
     */
    private boolean isDeclared(String varName, SymbolTable table) {
        return table.getParameters(currentMethod).stream().anyMatch(param -> param.getName().equals(varName)) ||
                table.getLocalVariables(currentMethod).stream().anyMatch(var -> var.getName().equals(varName)) ||
                table.getFields().stream().anyMatch(field -> field.getName().equals(varName)) ||
                table.getImports().stream().anyMatch(name -> name.equals(varName) || name.endsWith("." + varName));

    }

}
