package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.stream.Collectors;

import static pt.up.fe.comp2025.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not pure expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private int labelCounter = 0;
    private static final String SPACE     = " ";
    private static final String ASSIGN    = ":=";
    private final        String END_STMT  = ";\n";
    private final        String NL        = "\n";
    private final        String L_BRACKET = " {\n";
    private final        String R_BRACKET = "}\n";

    private final SymbolTable            table;
    private final TypeUtils              types;
    private final OptUtils               ollirTypes;
    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table       = table;
        this.types       = new TypeUtils(table);
        this.ollirTypes  = new OptUtils(types);
        this.exprVisitor = new OllirExprGeneratorVisitor(table);
        buildVisitor();
    }

    @Override
    protected void buildVisitor() {
        addVisit(PROGRAM,       this::visitProgram);
        addVisit(CLASS_DECL,    this::visitClass);
        addVisit(METHOD_DECL,   this::visitMethodDecl);
        addVisit(PARAM,         this::visitParam);
        addVisit(VAR_DECL,      this::visitVarDecl);
        addVisit(ASSIGN_STMT,   this::visitAssignStmt);
        addVisit(RETURN_STMT,   this::visitReturnStmt);
        addVisit(METHOD_CALL,   this::visitMethodCallStmt);
        addVisit(ARRAY_ACCESS,  this::visitArrayAccessStmt);
        addVisit(LENGTH_ACCESS, this::visitLengthStmt);
        addVisit(UNARY_OP,      this::visitUnaryOpStmt);
        addVisit(IF_STMT,       this::visitIfStmt);
        addVisit(WHILE_STMT,    this::visitWhileStmt);
        addVisit(STMT,          this::visitStmt);
        addVisit(IMPORT_DECL,   this::visitImportDecl);
        // fallback for ExprStmt, IfStmt, WhileStmt...
    }

    private String visitProgram(JmmNode node, Void unused) {
        return node.getChildren().stream()
                .map(this::visit)
                .collect(Collectors.joining());
    }

    private String visitClass(JmmNode node, Void unused) {
        String className = node.get("name");
        String superClass = node.getOptional("extended").orElse(null);

        StringBuilder sb = new StringBuilder();

        // Class declaration
        sb.append(className);
        if (superClass != null) {
            sb.append(" extends ").append(superClass);
        }
        sb.append(" {\n\n");

        // Add fields
        for (Symbol field : table.getFields()) {
            sb.append(".field ").append(field.getName())
                    .append(ollirTypes.toOllirType(field.getType()))
                    .append(";\n");
        }
        sb.append("\n");

        // Add constructor
        sb.append(buildConstructor()).append("\n");

        // Process methods
        for (JmmNode method : node.getChildren(Kind.METHOD_DECL)) {
            sb.append(visit(method, null));
        }

        sb.append("}\n");
        return sb.toString();
    }

    private String visitMethodDecl(JmmNode node, Void unused) {
        var sb = new StringBuilder(".method ");
        if (node.hasAttribute("isPublic") && node.get("isPublic").equals("true")) {sb.append("public ");}
        sb.append(node.get("name"))
                .append("(")
                .append(node.getChildren(PARAM).stream()
                        .map(p -> visit(p, null))
                        .collect(Collectors.joining(", ")))
                .append(")");

        var maybeType = node.getChildren().stream()
                .filter(c -> c.getKind().equals("Type"))
                .findFirst();

        if (maybeType.isPresent() && maybeType.get().hasAttribute("name")) {
            sb.append(ollirTypes.toOllirType(types.convertType(maybeType.get())));
        } else {
            sb.append(".V"); // Default void return
        }


        sb.append(L_BRACKET).append("\n");


        // method body
        for (var stmt : node.getChildren(STMT)) {
            sb.append("   ").append(visit(stmt, null));
        }
        sb.append(R_BRACKET).append(NL);
        return sb.toString();
    }

    private String visitParam(JmmNode node, Void unused) {
        System.out.println("aaaa: "+ node.toString());
        System.out.println("bbbb: "+ node.getChild(0));
        System.out.println("aaaa2: "+ unused);
        // name:type
        return node.get("name") +
                ollirTypes.toOllirType(types.convertType(node.getChild(0)));
    }

    private String visitVarDecl(JmmNode node, Void unused) {
        // only if there’s an initializer
        if (node.getNumChildren() == 0) return "";
        var init = exprVisitor.visit(node.getChild(0));
        var type = types.getExprType(node);
        var ollirT = ollirTypes.toOllirType(type);

        return init.getComputation() +
                node.get("name") + ollirT + SPACE + ASSIGN + ollirT + SPACE + init.getCode() +
                END_STMT;
    }

    private String visitAssignStmt(JmmNode node, Void unused) {
        var rhs = exprVisitor.visit(node.getChild(0));
        var lhs = node.get("name");
        Type t = types.getExprType(node.getChild(0));
        String ollirT = ollirTypes.toOllirType(t);

        return rhs.getComputation() +
                lhs + ollirT + SPACE + ASSIGN + ollirT + SPACE + rhs.getCode() +
                END_STMT;
    }

    private String visitReturnStmt(JmmNode node, Void unused) {
        var expr = node.getNumChildren()==0
                ? OllirExprResult.EMPTY
                : exprVisitor.visit(node.getChild(0));

        // deduce return type
        Type retT = node.getNumChildren()==0
                ? TypeUtils.newVoidType()
                : types.getExprType(node.getChild(0));
        String ollirRT = ollirTypes.toOllirType(retT);

        return expr.getComputation() +
                "ret" + ollirRT + SPACE + expr.getCode() +
                END_STMT;
    }

    private String visitMethodCallStmt(JmmNode node, Void unused) {
        // just treat it as an expression plus “;”
        var call = exprVisitor.visit(node);
        return call.getComputation() + call.getCode() + END_STMT;
    }

    private String visitArrayAccessStmt(JmmNode node, Void unused) {
        var acc = exprVisitor.visit(node);
        return acc.getComputation() + acc.getCode() + END_STMT;
    }

    private String visitLengthStmt(JmmNode node, Void unused) {
        var len = exprVisitor.visit(node);
        return len.getComputation() + len.getCode() + END_STMT;
    }

    private String visitUnaryOpStmt(JmmNode node, Void unused) {
        var u = exprVisitor.visit(node);
        return u.getComputation() + u.getCode() + END_STMT;
    }

    private String visitStmt(JmmNode node, Void unused) {
        // fallback for blocks, if/while, expr-stmts...
        // you can pattern-match on node.getKind() and decompose children similarly—
        // or simply delegate any leftover expr children:
        if (!node.getChildren().isEmpty()) {
            var first = node.getChild(0);
            if (first.getKind().equals("Identifier")
                    || first.getKind().equals("BinaryOp")
                    || first.getKind().equals("MethodCall")
                    || first.getKind().equals("ArrayAccess")
                    || first.getKind().equals("LengthAccess")
                    || first.getKind().equals("UnaryOp")) {
                var e = exprVisitor.visit(first);
                return e.getComputation() + e.getCode() + END_STMT;
            }
        }
        // empty or block
        return "";
    }

    private String visitIfStmt(JmmNode node, Void unused) {
        // [ condExpr, thenStmt, (elseStmt)? ]
        var cond   = exprVisitor.visit(node.getChild(0));
        StringBuilder sb = new StringBuilder();
        String thenLabel = "then_"  + (labelCounter++);
        String elseLabel = "else_"  + (labelCounter++);
        String endLabel  = "endif_" + (labelCounter++);

        // 1) compute condition
        sb.append(cond.getComputation());
        // 2) iffalse cond goto elseLabel;
        sb.append("iffalse ")
                .append(cond.getCode())
                .append(" goto ")
                .append(elseLabel)
                .append(";")
                .append("\n");
        // 3) then-block
        sb.append(visit(node.getChild(1), null));
        // 4) jump past the else
        sb.append("goto ")
                .append(endLabel)
                .append(";")
                .append("\n");
        // 5) elseLabel:
        sb.append(elseLabel).append(":").append("\n");
        if (node.getNumChildren() == 3) {
            sb.append(visit(node.getChild(2), null));
        }
        // 6) endLabel:
        sb.append(endLabel).append(":").append("\n");
        return sb.toString();
    }

    private String visitWhileStmt(JmmNode node, Void unused) {
        // [ condExpr, bodyStmt ]
        var cond   = exprVisitor.visit(node.getChild(0));
        StringBuilder sb = new StringBuilder();
        String startLabel = "while_start_" + (labelCounter++);
        String endLabel   = "while_end_"   + (labelCounter++);
        // 1) loop head
        sb.append(startLabel).append(":").append("\n");
        // 2) compute cond
        sb.append(cond.getComputation());
        // 3) iffalse cond goto endLabel;
        sb.append("iffalse ")
                .append(cond.getCode())
                .append(" goto ")
                .append(endLabel)
                .append(";")
                .append("\n");
        // 4) body
        sb.append(visit(node.getChild(1), null));
        // 5) jump back to head
        sb.append("goto ")
                .append(startLabel)
                .append(";")
                .append("\n");
        // 6) endLabel:
        sb.append(endLabel).append(":").append("\n");
        return sb.toString();
    }



    public String visitImportDecl(JmmNode node, Void unused) {
        // Simply return an empty string or handle the import as needed.
        return "";
    }


    private String buildConstructor() {
        return """
            .construct %s().V {
                invokespecial(this, "<init>").V;
            }
            """.formatted(table.getClassName());
    }
}
