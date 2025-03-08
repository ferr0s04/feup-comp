package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.stream.Collectors;

import static pt.up.fe.comp2025.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;

    private final TypeUtils types;
    private final OptUtils ollirTypes;


    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        this.types = new TypeUtils(table);
        this.ollirTypes = new OptUtils(types);
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(STMT, this::visitStmt);

//        setDefaultVisit(this::defaultVisit);
    }


    private String visitAssignStmt(JmmNode node, Void unused) {

        var rhs = exprVisitor.visit(node.getChild(1));

        StringBuilder code = new StringBuilder();

        // code to compute the children
        code.append(rhs.getComputation());

        // code to compute self
        // statement has type of lhs
        var left = node.getChild(0);
        Type thisType = types.getExprType(left);
        String typeString = ollirTypes.toOllirType(thisType);
        var varCode = left.get("name") + typeString;


        code.append(varCode);
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {
        // TODO: Hardcoded for int type, needs to be expanded
        Type retType = TypeUtils.newIntType();


        StringBuilder code = new StringBuilder();


        var expr = node.getNumChildren() > 0 ? exprVisitor.visit(node.getChild(0)) : OllirExprResult.EMPTY;


        code.append(expr.getComputation());
        code.append("ret");
        code.append(ollirTypes.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {

        var typeCode = ollirTypes.toOllirType(node.getChild(0));
        var id = node.get("name");

        String code = id + typeCode;

        return code;
    }


    private String visitMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = node.getBoolean("isPublic", false);

        if (isPublic) {
            code.append("public ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        // params
        // TODO: Hardcoded for a single parameter, needs to be expanded
        var paramsCode = visit(node.getChild(1));
        code.append("(" + paramsCode + ")");

        // type
        // TODO: Hardcoded for int, needs to be expanded
        var retType = ".i32";
        code.append(retType);
        code.append(L_BRACKET);


        // rest of its children stmts
        var stmtsCode = node.getChildren(STMT).stream()
                .map(this::visit)
                .collect(Collectors.joining("\n   ", "   ", ""));

        code.append(stmtsCode);
        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }


    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(NL);
        code.append(table.getClassName());
        
        code.append(L_BRACKET);
        code.append(NL);
        code.append(NL);

        code.append(buildConstructor());
        code.append(NL);

        for (var child : node.getChildren(METHOD_DECL)) {
            var result = visit(child);
            code.append(result);
        }

        code.append(R_BRACKET);

        return code.toString();
    }

    private String visitStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // Check for different types of statements

        if (node.getNumChildren() == 0) {
            // Empty block (e.g., `{}`)
            return "";
        }

        JmmNode firstChild = node.getChild(0);

        if (firstChild.getKind().equals("IF")) {
            // Handle IF statement
            String condition = String.valueOf(exprVisitor.visit(firstChild.getChild(0))); // Visit the expression
            String ifStmt = visitStmt(firstChild.getChild(1), unused); // Visit the "then" part of the statement

            String elseStmt = "";
            if (node.getNumChildren() > 1) {
                // Handle optional ELSE statement
                JmmNode elseChild = node.getChild(2);
                elseStmt = visitStmt(elseChild, unused);
            }

            code.append("if (").append(condition).append(") ")
                    .append(ifStmt);

            if (!elseStmt.isEmpty()) {
                code.append("else ").append(elseStmt);
            }

            code.append(END_STMT);
        } else if (firstChild.getKind().equals("WHILE")) {
            // Handle WHILE statement
            String condition = String.valueOf(exprVisitor.visit(firstChild.getChild(0))); // Visit the expression
            String whileStmt = visitStmt(firstChild.getChild(1), unused); // Visit the loop body

            code.append("while (").append(condition).append(") ")
                    .append(whileStmt)
                    .append(END_STMT);
        } else if (firstChild.getKind().equals("ID") && node.getNumChildren() == 3) {
            // Handle assignment statements: name = expr;
            String varName = firstChild.getKind();  // Get the kind of the node (ID in this case)
            String expr = String.valueOf(exprVisitor.visit(node.getChild(1))); // Visit the expression

            code.append(varName).append(" = ").append(expr)
                    .append(END_STMT);
        } else if (firstChild.getKind().equals("ID") && node.getNumChildren() == 4) {
            // Handle array assignment: name[expr] = expr;
            String varName = firstChild.getKind();  // Get the kind of the node (ID)
            String index = String.valueOf(exprVisitor.visit(node.getChild(1))); // Visit the index expression
            String value = String.valueOf(exprVisitor.visit(node.getChild(2))); // Visit the value expression

            code.append(varName).append("[").append(index).append("] = ").append(value)
                    .append(END_STMT);
        } else if (firstChild.getKind().equals("EXPR")) {
            // Handle expression statement: expr;
            String expr = String.valueOf(exprVisitor.visit(firstChild)); // Visit the expression

            code.append(expr)
                    .append(END_STMT);
        } else {
            // Unknown statement, handle accordingly or throw an error
            throw new UnsupportedOperationException("Unknown statement type: " + firstChild.getKind());
        }

        return code.toString();
    }


    private String buildConstructor() {

        return """
                .construct %s().V {
                    invokespecial(this, "<init>").V;
                }
                """.formatted(table.getClassName());
    }


    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
