package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.comp.jmm.analysis.table.Type;

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

    private Void visitClassDecl(JmmNode classNode, SymbolTable table) {
        className = classNode.get("name");
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        isStaticMethod = method.getOptional("isStatic").map(Boolean::parseBoolean).orElse(false);
        return null;
    }

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

    private boolean isDeclared(String varName, SymbolTable table) {
        return table.getParameters(currentMethod).stream().anyMatch(param -> param.getName().equals(varName)) ||
                table.getLocalVariables(currentMethod).stream().anyMatch(var -> var.getName().equals(varName)) ||
                table.getFields().stream().anyMatch(field -> field.getName().equals(varName));
    }

    private boolean isValidThisAssignment(JmmNode assignStmt, SymbolTable table) {
        if (assignStmt.getNumChildren() < 2) {
            return false; // Invalid assignment structure
        }

        JmmNode rhs = assignStmt.getChild(1);
        Type assignedType = new TypeUtils(table).getExprType(rhs);

        String superClass = table.getSuper(); // Assuming this returns a String or null
        return assignedType.getName().equals(className) || (superClass != null && assignedType.getName().equals(superClass));
    }
}
