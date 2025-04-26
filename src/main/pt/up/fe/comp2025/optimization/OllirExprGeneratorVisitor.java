package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.stream.Collectors;

import static pt.up.fe.comp2025.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor
        extends PreorderJmmVisitor<Void, OllirExprResult> {

    private int labelCounter = 0;
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

        // new ones:
        addVisit(NEW_OBJECT,     this::visitNewObject);
        addVisit(NEW_ARRAY,      this::visitNewArray);
        addVisit(METHOD_CALL,    this::visitMethodCall);
        addVisit(ARRAY_ACCESS,   this::visitArrayAccess);
        addVisit(LENGTH_ACCESS,  this::visitLengthAccess);
        addVisit(UNARY_OP,       this::visitUnaryOp);
    }

    private OllirExprResult visitLiteral(JmmNode node, Void unused) {
        // Get type
        Type t = types.getExprType(node);
        String ollirType = ollirTypes.toOllirType(t);
        String value = node.get("value");

        // If it's boolean, we must assign it to a temp
        if (t.getName().equals("boolean")) {
            String tmp = ollirTypes.nextTemp() + ollirType;
            StringBuilder comp = new StringBuilder();
            comp.append(tmp).append(SPACE)
                    .append(ASSIGN).append(ollirType).append(SPACE)
                    .append(value).append(ollirType)
                    .append(END_STMT);

            return new OllirExprResult(tmp, comp);
        }

        // If it's not boolean (e.g., int, string), it's fine
        return new OllirExprResult(value + ollirType);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        String id = node.get("name");
        Type t = types.getExprType(node);
        String ollirType = ollirTypes.toOllirType(t);
        return new OllirExprResult(id + ollirType);
    }

    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {
        var left  = visit(node.getChild(0));
        var right = visit(node.getChild(1));

        StringBuilder comp = new StringBuilder();
        String operator = node.get("op");

        // Handle logical AND (&&)
        if ("&&".equals(operator)) {
            String leftVar = left.getComputation();
            String rightVar = right.getComputation();
            String tmpVar = ollirTypes.nextTemp() + ollirTypes.toOllirType(types.getExprType(node));

            // generate a fresh label
            String endLabel = "end_and_" + (labelCounter++);

            // Evaluate left operand
            comp.append(left.getComputation());

            // Short-circuit: if left is false, skip evaluating right
            comp.append("iffalse ").append(left.getCode()).append(" goto [").append(endLabel).append("]\n");

            // Evaluate right operand
            comp.append(right.getComputation());

            // tmpVar = left && right
            comp.append(tmpVar).append(SPACE)
                    .append(ASSIGN).append(ollirTypes.toOllirType(types.getExprType(node))).append(SPACE)
                    .append(left.getCode()).append(" &&.boolean ").append(right.getCode())
                    .append(END_STMT);

            // Label
            comp.append("[").append(endLabel).append("]:\n");

            return new OllirExprResult(tmpVar, comp);
        }

        // Handle other operators (e.g., +, -, *, etc.)
        comp.append(left.getComputation());
        comp.append(right.getComputation());

        Type resType = types.getExprType(node);
        String resOllir = ollirTypes.toOllirType(resType);
        String resultTemp = ollirTypes.nextTemp() + resOllir;

        comp.append(resultTemp).append(SPACE)
                .append(ASSIGN).append(resOllir).append(SPACE)
                .append(left.getCode()).append(SPACE)
                .append(operator).append(resOllir).append(SPACE)
                .append(right.getCode())
                .append(END_STMT);

        return new OllirExprResult(resultTemp, comp);
    }




    private OllirExprResult visitNewObject(JmmNode node, Void unused) {
        // new ClassName()
        String className = node.get("name");
        Type t = new Type(className, false);
        String ollirT  = ollirTypes.toOllirType(t);
        String tmp     = ollirTypes.nextTemp() + ollirT;

        StringBuilder comp = new StringBuilder();
        comp.append(tmp).append(SPACE)
                .append(ASSIGN).append(ollirT).append(SPACE)
                .append("new ").append(className).append("()").append(ollirT)
                .append(END_STMT);

        return new OllirExprResult(tmp, comp);
    }

    private OllirExprResult visitNewArray(JmmNode node, Void unused) {
        // new int[expr]
        var lenRes = visit(node.getChild(0));

        Type   t        = new Type("int", true);
        String ollirArr = ollirTypes.toOllirType(t);
        String tmp      = ollirTypes.nextTemp() + ollirArr;

        StringBuilder comp = new StringBuilder();
        comp.append(lenRes.getComputation());
        comp.append(tmp).append(SPACE)
                .append(ASSIGN).append(ollirArr).append(SPACE)
                .append("newarray(").append(lenRes.getCode()).append(")").append(ollirArr)
                .append(END_STMT);

        return new OllirExprResult(tmp, comp);
    }

    private OllirExprResult visitMethodCall(JmmNode node, Void unused) {
        // receiver and args
        JmmNode recv = node.getChild(0);
        var recvRes = visit(recv);

        StringBuilder comp = new StringBuilder();
        comp.append(recvRes.getComputation());

        // arguments (if any)
        String argsCode = node.getChildren().stream()
                .skip(1)
                .map(child -> {
                    var r = visit(child);
                    comp.append(r.getComputation());
                    return r.getCode();
                })
                .collect(Collectors.joining(", "));

        // return type
        Type   retType  = types.getExprType(node);
        String retOllir = ollirTypes.toOllirType(retType);
        String tmp      = ollirTypes.nextTemp() + retOllir;

        comp.append(tmp).append(SPACE)
                .append(ASSIGN).append(retOllir).append(SPACE)
                .append("invokevirtual(")
                .append(recvRes.getCode()).append(", ")
                .append(node.get("name")).append("(")
                .append(argsCode).append("))")
                .append(retOllir)
                .append(END_STMT);

        return new OllirExprResult(tmp, comp);
    }

    private OllirExprResult visitArrayAccess(JmmNode node, Void unused) {
        var arrRes = visit(node.getChild(0));
        var idxRes = visit(node.getChild(1));

        StringBuilder comp = new StringBuilder();
        comp.append(arrRes.getComputation()).append(idxRes.getComputation());

        Type   elemType = types.getExprType(node);
        String elemO    = ollirTypes.toOllirType(elemType);
        String tmp      = ollirTypes.nextTemp() + elemO;

        comp.append(tmp).append(SPACE)
                .append(ASSIGN).append(elemO).append(SPACE)
                .append("arrayload(").append(arrRes.getCode())
                .append(", ").append(idxRes.getCode())
                .append(")").append(elemO)
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
                .append("inv ").append(exprRes.getCode())
                .append(ollirBool)
                .append(END_STMT);

        return new OllirExprResult(tmp, comp);
    }
}
