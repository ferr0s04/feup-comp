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
        System.out.println("==== INITIALIZING OLLIR GENERATOR ====");
        System.out.println("Symbol table imports: " + table.getImports());
        System.out.println("Symbol table fields: " + table.getFields());
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
        addVisit(ARRAY_ASSIGN_STMT, this::visitAssignStmt);
        addVisit(LITERAL,      this::visitLiteral);
        // fallback for ExprStmt, IfStmt, WhileStmt...
    }

    private String visitProgram(JmmNode node, Void unused) {
        return node.getChildren().stream()
                .map(this::visit)
                .collect(Collectors.joining());
    }

    private String visitClass(JmmNode node, Void unused) {
        StringBuilder sb = new StringBuilder();

        // Add imports first
        for (JmmNode imp : node.getChildren(Kind.IMPORT_DECL)) {
            sb.append(visit(imp, null));
        }

        // Class declaration
        String className = node.get("name");
        String superClass = node.getOptional("extended").orElse(null);
        sb.append(className);
        if (superClass != null) {
            sb.append(" extends ").append(superClass);
        }
        sb.append(L_BRACKET).append(NL).append(NL);  // Use constants

        // Add fields - MODIFIED to include public and proper type suffixes
        for (Symbol field : table.getFields()) {
            sb.append("    .field public ")  // Fixed indentation and added public
                    .append(field.getName())
                    .append(ollirTypes.toOllirType(field.getType()))
                    .append(";\n");
        }
        sb.append(NL);

        // Add constructor
        sb.append(buildConstructor()).append(NL);

        // Process methods
        for (JmmNode method : node.getChildren(Kind.METHOD_DECL)) {
            sb.append(visit(method, null));
        }

        sb.append(R_BRACKET).append(NL);
        return sb.toString();
    }

    private String visitMethodDecl(JmmNode node, Void unused) {
        System.out.println("==== DEBUG visitMethodDecl ====");
        System.out.println("Method name: " + node.get("name"));
        System.out.println("Is main: " + node.get("isMain"));

        var sb = new StringBuilder(".method ");

        // Handle 'public' modifier
        if (node.hasAttribute("isPublic") && node.get("isPublic").equals("true")) {
            sb.append("public ");
        }

        // Handle 'static' modifier (for main method)
        if (node.hasAttribute("isMain") && node.get("isMain").equals("true")) {
            sb.append("static ");
        }

        sb.append(node.get("name")).append("(");

        // Special case for main method
        if (node.hasAttribute("isMain") && node.get("isMain").equals("true")) {
            sb.append("args.array.String");
        } else {
            // Normal parameters
            sb.append(node.getChildren(PARAM).stream()
                    .map(p -> visit(p, null))
                    .collect(Collectors.joining(", ")));
        }

        sb.append(")");

        // Return type
        var maybeType = node.getChildren().stream()
                .filter(c -> c.getKind().equals("Type"))
                .findFirst();

        if (maybeType.isPresent() && maybeType.get().hasAttribute("name")) {
            sb.append(ollirTypes.toOllirType(types.convertType(maybeType.get())));
        } else {
            sb.append(".V"); // Default void return
        }

        sb.append(L_BRACKET);  // Use constant for " {\n"

        // Method body with proper indentation
        for (var stmt : node.getChildren(STMT)) {
            sb.append("   ").append(visit(stmt, null));  // 3 spaces indent
        }

        // Add return statement for main method, with specific formatting to match expected output
        if(node.hasAttribute("isMain") && node.get("isMain").equals("true")){
            sb.append("\nret.V;}");
        } else {
            sb.append(R_BRACKET).append(NL);  // Normal formatting for other methods
        }

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
        System.out.println("==== DEBUG visitAssignStmt ====");
        System.out.println("Assignment target: " + node.get("name"));
        System.out.println("Assignment type: " + types.getExprType(node.getChild(0)));
        var rhs = exprVisitor.visit(node.getChild(0));  // Right-hand side expression
        var lhs = node.get("name");  // Left-hand side variable or array
        Type t = types.getExprType(node.getChild(0));  // Type of the right-hand side expression
        String ollirT = ollirTypes.toOllirType(t);  // OLLIR type of the right-hand side expression

        // Check if the left-hand side is an array (Array assignment)
        if (node.getKind().equals("ArrayAssignStmt")) {
            return visitArrayAssignStmt(node, rhs, ollirT);  // Delegate to array-specific handling
        }

        // Handle regular variable assignments
        String methodName = getEnclosingMethod(node);
        if (methodName != null) {
            boolean isLocalOrParam = false;

            // Check parameters first
            for (Symbol param : table.getParameters(methodName)) {
                if (param.getName().equals(lhs)) {
                    isLocalOrParam = true;
                    break;
                }
            }

            // If it's not a parameter, check local variables
            if (!isLocalOrParam) {
                for (Symbol local : table.getLocalVariables(methodName)) {
                    if (local.getName().equals(lhs)) {
                        isLocalOrParam = true;
                        break;
                    }
                }
            }

            // If it's not a local variable or parameter, then it's a field
            if (!isLocalOrParam) {
                // Field assignment
                String fieldWithType = lhs + ollirT;
                return rhs.getComputation() +
                        "putfield(this, " + fieldWithType + ", " + rhs.getCode() + ").V" +
                        END_STMT;
            }
        }

        // Local variable or parameter assignment
        return rhs.getComputation() +
                lhs + ollirT + SPACE + ASSIGN + ollirT + SPACE + rhs.getCode() +
                END_STMT;
    }

    private String visitArrayAssignStmt(JmmNode node, OllirExprResult rhs, String ollirT) {
        String arrayName = node.get("name");

        JmmNode indexNode = node.getChild(0);
        String indexValue = indexNode.get("value");

        JmmNode valueNode = node.getChild(1);
        String value = valueNode.get("value");

        return arrayName + "[" + indexValue + ollirT + "]" + ollirT +
                " :=" + ollirT + " " + value + ollirT + END_STMT;
    }

    // Método auxiliar para obter o nome do método que contém o nó atual
    private String getEnclosingMethod(JmmNode node) {
        JmmNode current = node;
        while (current != null) {
            if (current.getKind().equals("MethodDecl") && current.hasAttribute("name")) {
                return current.get("name");
            }
            current = current.getParent();
        }
        return null;
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
        System.out.println("==== DEBUG visitIfStmt ====");
        System.out.println("If statement node: " + node.toTree());
        System.out.println("Number of children: " + node.getNumChildren());

        // Access the condition and both branches
        var cond = exprVisitor.visit(node.getChild(0));
        StringBuilder sb = new StringBuilder();

        // Use separate counters for then and endif labels
        String thenLabel = "then" + getThenLabelCounter();
        String endifLabel = "endif" + getEndifLabelCounter();


        // 1) Add condition computation
        sb.append(cond.getComputation());

        // 2) Check condition and goto thenLabel if true
        sb.append("if (")
                .append(cond.getCode())
                .append(") goto ")
                .append(thenLabel)
                .append(";")
                .append("\n");

        // 3) Handle the "else" branch first (node.getChild(2))
        if (node.getNumChildren() > 2 && node.getChild(2).getKind().equals("BlockStmt")) {
            for (int i = 0; i < node.getChild(2).getNumChildren(); i++) {
                sb.append(visit(node.getChild(2).getChild(i), null));
            }
        }

        // 4) Add goto to skip over "then" branch
        sb.append("goto ")
                .append(endifLabel)
                .append(";")
                .append("\n");

        // 5) Add the "then" label
        sb.append(thenLabel)
                .append(":")
                .append("\n");

        // 6) Handle the "then" branch (node.getChild(1))
        if (node.getChild(1).getKind().equals("BlockStmt")) {
            for (int i = 0; i < node.getChild(1).getNumChildren(); i++) {
                sb.append(visit(node.getChild(1).getChild(i), null));
            }
        } else {
            sb.append(visit(node.getChild(1), null));
        }

        // 7) Add the endif label
        sb.append(endifLabel)
                .append(":")
                .append("\n");

        return sb.toString();
    }

    private String visitWhileStmt(JmmNode node, Void unused) {
        // Ensure the node has the correct structure for a while statement
        if (node.getNumChildren() < 2) {
            throw new IllegalArgumentException("While statement must have at least two children: condition and body");
        }

        // Extract condition expression and body statement
        var cond = exprVisitor.visit(node.getChild(0));  // Visit the condition of the while
        var body = node.getChild(1);  // Body of the while loop

        // Generate unique labels for start and end of the loop
        String startLabel = "while" + (getWhile_start_labelCounter());
        String endLabel = "endif" + (getWhile_end_labelCounter());

        StringBuilder sb = new StringBuilder();

        sb.append(startLabel).append(":").append(NL);
        sb.append(cond.getComputation());
        sb.append("if (").append(cond.getCode()).append(") goto ").append(endLabel).append(";").append(NL);
        sb.append(visit(body, null));
        sb.append("goto ").append(startLabel).append(";").append(NL);
        sb.append(endLabel).append(":").append(NL);

        return sb.toString();
    }



    private String visitImportDecl(JmmNode node, Void unused) {
        //System.out.println("Import node attributes: " + node.getAttributes());
        //System.out.println("Import node children: " + node.getChildren());
        String importName = node.get("name").replace(";", "").replaceAll("[\\[\\]]", "");
        return "import " + importName + ";\n";
    }

    private String visitLiteral(JmmNode node, Void unused) {
        // Retorna o valor do literal com o sufixo apropriado
        return node.get("value") + ".i32";
    }


    private String buildConstructor() {
        return "    .construct %s().V {\n".formatted(table.getClassName()) +
                "        invokespecial(this, \"<init>\").V;\n" +
                "    }\n";
    }

    public int getThenLabelCounter() {
        return exprVisitor.getThenLabelCounter();
    }

    public int getEndifLabelCounter() {
        return exprVisitor.getEndifLabelCounter();
    }

    public int getWhile_start_labelCounter() {
        return exprVisitor.getWhile_start_labelCounter();
    }

    public int getWhile_end_labelCounter() {
        return exprVisitor.getWhile_end_labelCounter();
    }
}
