package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.comp2025.optimization.OllirGeneratorVisitor;

import java.util.stream.Collectors;

import static pt.up.fe.comp2025.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor
        extends PreorderJmmVisitor<Void, OllirExprResult> {

    private int thenLabelCounter = 0;
    private int endifLabelCounter = 0;
    private int while_start_labelCounter = 0;
    private int while_end_labelCounter = 0;

    private static final String SPACE  = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT     = ";\n";

    private final SymbolTable table;
    private final TypeUtils  types;
    private final OptUtils   ollirTypes;



    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table      = table;
        this.types      = new TypeUtils(table);
        this.ollirTypes = new OptUtils(types);
        buildVisitor();
    }

    @Override
    protected void buildVisitor() {
        // existing
        addVisit(IDENTIFIER, this::visitVarRef);
        addVisit(LITERAL,    this::visitLiteral);
        addVisit(BINARY_OP,  this::visitBinExpr);
        addVisit(NEW_OBJECT,     this::visitNewObject);
        addVisit(NEW_ARRAY,      this::visitNewArray);
        addVisit(METHOD_CALL,    this::visitMethodCall);
        addVisit(ARRAY_ACCESS,   this::visitArrayAccess);
        addVisit(LENGTH_ACCESS,  this::visitLengthAccess);
        addVisit(UNARY_OP,       this::visitUnaryOp);
        addVisit(PRIMARY,        this::visitPrimary);
    }

    private OllirExprResult visitPrimary(JmmNode node, Void unused) {
        return visit(node.getChild(0));
    }

    private OllirExprResult visitLiteral(JmmNode node, Void unused) {
        // Get type
        Type t = types.getExprType(node);
        String ollirType = ollirTypes.toOllirType(t);
        String value = node.get("value");

        // Convert boolean true/false to 1/0
        if (t.getName().equals("boolean")) {
            String boolValue = Boolean.parseBoolean(value) ? "1" : "0";
            return new OllirExprResult(boolValue + ollirType);
        }

        // If it's not boolean (e.g., int, string), it's fine
        return new OllirExprResult(value + ollirType);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        String id = node.get("name");
        Type t = types.getExprType(node);
        String ollirType = ollirTypes.toOllirType(t);

        // FIRST check if this is an imported class (static reference)
        if (table.getImports().contains(id)) {
            return new OllirExprResult(id + ollirType);
        }

        String methodName = getEnclosingMethod(node);

        if (methodName != null) {
            boolean isLocalOrParam = false;

            // Verificar nos parâmetros
            for (Symbol param : table.getParameters(methodName)) {
                if (param.getName().equals(id)) {
                    isLocalOrParam = true;
                    break;
                }
            }

            // Se não for parâmetro, verificar nas variáveis locais
            if (!isLocalOrParam) {
                for (Symbol local : table.getLocalVariables(methodName)) {
                    if (local.getName().equals(id)) {
                        isLocalOrParam = true;
                        break;
                    }
                }
            }

            // Se não for uma variável local ou parâmetro, então é um campo da classe
            if (!isLocalOrParam) {
                // Field access - include type suffix
                String fieldWithType = id + ollirType;  // e.g., "intField.i32"
                String tmp = ollirTypes.nextTemp() + ollirType;
                StringBuilder comp = new StringBuilder();
                comp.append(tmp).append(SPACE)
                        .append(ASSIGN).append(ollirType).append(SPACE)
                        .append("getfield(this, ").append(fieldWithType).append(")")
                        .append(ollirType)
                        .append(END_STMT);
                return new OllirExprResult(tmp, comp);
            }
        }

        return new OllirExprResult(id + ollirType);
    }

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

    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {
        if (node.getNumChildren() != 2) {
            throw new IllegalArgumentException("Binary expression must have exactly 2 children");
        }

        OllirExprResult left = visit(node.getChild(0));
        OllirExprResult right = visit(node.getChild(1));
        String operator = node.get("op");

        if (operator == null) {
            throw new IllegalArgumentException("Binary operator missing");
        }

        StringBuilder comp = new StringBuilder();

        // Handle short-circuiting logical operators
        if ("&&".equals(operator) || "||".equals(operator)) {
            return handleLogicalOperator(node, left, right, operator, comp);
        }

        // Handle regular binary operators
        comp.append(left.getComputation())
                .append(right.getComputation());

        Type resType = types.getExprType(node);
        String resOllir = ollirTypes.toOllirType(resType);
        String resultTemp = ollirTypes.nextTemp() + resOllir;

        comp.append(String.format("%s %s %s %s %s %s%s",
                resultTemp, ASSIGN, resOllir,
                left.getCode(), operator + resOllir,
                right.getCode(), END_STMT));

        return new OllirExprResult(resultTemp, comp);
    }

    private OllirExprResult handleLogicalOperator(JmmNode node,
                                                  OllirExprResult left, OllirExprResult right,
                                                  String operator, StringBuilder comp) {

        String thenLabel = "then" + getThenLabelCounter();
        String endifLabel = "endif" + getEndifLabelCounter();
        String tmpVar = "logicalTmp" + getThenLabelCounter() + ".bool";

        comp.append(left.getComputation());

        // For && we jump if true, for || we jump if false
        String jumpCondition = "&&".equals(operator) ?
                left.getCode() : "!" + left.getCode();

        comp.append(String.format("if (%s) goto %s;\n", jumpCondition, thenLabel))
                .append(String.format("%s :=.bool 0.bool;\n", tmpVar))
                .append(String.format("goto %s;\n", endifLabel))
                .append(String.format("%s:\n\n", thenLabel))
                .append(right.getComputation())
                .append(String.format("%s :=.bool %s;\n", tmpVar, right.getCode()))
                .append(String.format("%s:\n", endifLabel));

        return new OllirExprResult(tmpVar, comp);
    }


    private OllirExprResult visitNewObject(JmmNode node, Void unused) {
        String className = node.get("name");
        Type t = new Type(className, false);
        String ollirT  = ollirTypes.toOllirType(t);
        String tmp     = ollirTypes.nextTemp() + ollirT;

        StringBuilder comp = new StringBuilder();
        comp.append(tmp).append(SPACE)
                .append(ASSIGN).append(ollirT).append(SPACE)
                .append("new(").append(className).append(")").append(ollirT)
                .append(END_STMT);

        return new OllirExprResult(tmp, comp);
    }

    private OllirExprResult visitNewArray(JmmNode node, Void unused) {
        var lenRes = visit(node.getChild(0));

        Type   t        = new Type("int", true);
        String ollirArr = ollirTypes.toOllirType(t);
        String tmp      = ollirTypes.nextTemp() + ollirArr;

        StringBuilder comp = new StringBuilder();
        comp.append(lenRes.getComputation());
        comp.append(tmp).append(SPACE)
                .append(ASSIGN).append(ollirArr).append(SPACE)
                .append("new(array, ").append(lenRes.getCode()).append(")").append(ollirArr)
                .append(END_STMT);

        return new OllirExprResult(tmp, comp);
    }

    private OllirExprResult visitMethodCall(JmmNode node, Void unused) {
        JmmNode recv = node.getChild(0);
        var recvRes = visit(recv);

        StringBuilder comp = new StringBuilder();
        comp.append(recvRes.getComputation());

        String argsCode = node.getChildren().stream()
                .skip(1)
                .map(child -> {
                    var r = visit(child);
                    comp.append(r.getComputation());
                    return r.getCode();
                })
                .collect(Collectors.joining(", "));

        Type retType = types.getExprType(node);
        String retOllir = ollirTypes.toOllirType(retType);

        // Verifica se o receptor é uma classe importada
        if (types.isImported(recv.get("name"))) {
            comp.append("invokestatic(")
                    .append(recv.get("name")).append(", \"")
                    .append(node.get("name")).append("\"");

            if (!argsCode.isEmpty()) {
                comp.append(", ").append(argsCode);
            }

            comp.append(")").append(retOllir);
            return new OllirExprResult("", comp);
        }

        // Determine if this is a static call
        boolean isStatic = table.getImports().contains(recv.get("name"));

        if (retType.getName().equals("void")) {
            if (isStatic) {
                comp.append("invokestatic(")
                        .append(recv.get("name")).append(", \"")
                        .append(node.get("name")).append("\"");

                if (!argsCode.isEmpty()) {
                    comp.append(", ").append(argsCode);
                }

                comp.append(").V");
                return new OllirExprResult("", comp);
            } else {
                comp.append("invokevirtual(")
                        .append(recvRes.getCode()).append(", \"")
                        .append(node.get("name")).append("\"");

                if (!argsCode.isEmpty()) {
                    comp.append(", ").append(argsCode);
                }

                comp.append(").V");
                return new OllirExprResult("", comp);
            }
        }

        String receiverName = recv.get("name");
        // Handle non-void return types (similar logic as above but with temp var)
        String tmp = ollirTypes.nextTemp() + retOllir;
        if (isStatic) {
            comp.append(tmp).append(SPACE)
                    .append(ASSIGN).append(retOllir).append(SPACE)
                    .append("invokestatic(")
                    .append(receiverName).append(", \"")
                    .append(node.get("name")).append("\"");
        } else {
            comp.append(tmp).append(SPACE)
                    .append(ASSIGN).append(retOllir).append(SPACE)
                    .append("invokevirtual(")
                    .append(recvRes.getCode()).append(", \"")
                    .append(node.get("name")).append("\"");
        }

        if (!argsCode.isEmpty()) {
            comp.append(", ").append(argsCode);
        }

        comp.append(")").append(retOllir).append(END_STMT);
        return new OllirExprResult(tmp, comp);
    }

    private OllirExprResult visitArrayAccess(JmmNode node, Void unused) {
        // Visit the array and index expressions
        var arrRes = visit(node.getChild(0)); // array
        var idxRes = visit(node.getChild(1)); // index

        // Prepare the computation part
        StringBuilder comp = new StringBuilder();
        comp.append(arrRes.getComputation()).append(idxRes.getComputation());

        // Get the element type for the array access
        Type elemType = types.getExprType(node);
        String elemO = ollirTypes.toOllirType(elemType);
        String tmp = ollirTypes.nextTemp() + elemO;

        // Generate the array access code
        comp.append(tmp).append(SPACE)
                .append(ASSIGN).append(elemO).append(SPACE)
                .append(arrRes.getCode())  // Array part
                .append("[").append(idxRes.getCode())  // Index part
                .append("]").append(elemO)  // Closing bracket
                .append(END_STMT);

        return new OllirExprResult(tmp, comp);
    }

    private OllirExprResult visitLengthAccess(JmmNode node, Void unused) {
        var target = visit(node.getChild(0));

        StringBuilder comp = new StringBuilder();
        comp.append(target.getComputation());

        // length is always an int
        String ollirI  = ollirTypes.toOllirType(TypeUtils.newIntType());
        String tmp     = ollirTypes.nextTemp() + ollirI;

        comp.append(tmp).append(SPACE)
                .append(ASSIGN).append(ollirI).append(SPACE)
                .append("arraylength(").append(target.getCode())
                .append(")").append(ollirI)
                .append(END_STMT);

        return new OllirExprResult(tmp, comp);
    }

    private OllirExprResult visitUnaryOp(JmmNode node, Void unused) {
        // only '!' for now
        var exprRes = visit(node.getChild(0));

        StringBuilder comp = new StringBuilder();
        comp.append(exprRes.getComputation());

        String ollirBool = ollirTypes.toOllirType(TypeUtils.newBooleanType());
        String tmp       = ollirTypes.nextTemp() + ollirBool;

        comp.append(tmp).append(SPACE)
                .append(ASSIGN).append(ollirBool).append(SPACE)
                .append("! ").append(exprRes.getCode())
                .append(END_STMT);

        return new OllirExprResult(tmp, comp);
    }

    public int getThenLabelCounter(){
        return thenLabelCounter++;
    }

    public int getEndifLabelCounter(){
        return endifLabelCounter++;
    }

    public int getWhile_start_labelCounter(){
        return while_start_labelCounter++;
    }

    public int getWhile_end_labelCounter(){
        return while_end_labelCounter++;
    }

}
