package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;

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
        return null;
    }

    /**
     * Stores the current method name and static flag when visiting method declarations.
     */
    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        isStaticMethod = method.getOptional("isStatic").map(Boolean::parseBoolean).orElse(false);
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
                table.getFields().stream().anyMatch(field -> field.getName().equals(varName));
    }

}
