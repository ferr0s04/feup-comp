package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
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
        System.out.println(t.getName());
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

        // DEBUG
        System.out.println("==== DEBUG visitVarRef ====");
        System.out.println("ID: " + id);
        System.out.println("Type: " + t.getName() + (t.isArray() ? "[]" : ""));

        String methodName = getEnclosingMethod(node);
        System.out.println("Enclosing method: " + methodName);

        if (methodName != null) {
            // Verificar se é um parâmetro ou variável local do método atual
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

        // Variável local ou parâmetro - apenas retornar o nome com tipo
        return new OllirExprResult(id + ollirType);
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

    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {
        var left  = visit(node.getChild(0));
        var right = visit(node.getChild(1));

        StringBuilder comp = new StringBuilder();
        String operator = node.get("op");

        // Handle logical AND (&&)
        if ("&&".equals(operator)) {
            String tmpVar = ollirTypes.nextTemp() + ollirTypes.toOllirType(types.getExprType(node));
            String endLabel = "end" + (labelCounter++);

            // 1) compute left
            comp.append(left.getComputation());

            // 2) if left goto endLabel;
            comp.append("if (")
                    .append(left.getCode())
                    .append(") goto ")
                    .append(endLabel)
                    .append(";")
                    .append("\n");

            // 3) compute right
            comp.append(right.getComputation());

            // 4) tmpVar := left &&.boolean right;
            comp.append(tmpVar).append(SPACE)
                    .append(ASSIGN).append(ollirTypes.toOllirType(types.getExprType(node))).append(SPACE)
                    .append(left.getCode()).append(" &&.bool ").append(right.getCode())
                    .append(END_STMT);

            // 5) emit the label
            comp.append(endLabel)
                    .append(":")
                    .append("\n");

            return new OllirExprResult(tmpVar, comp);
        }

        // Fallback for other binary ops
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
                .append("new(").append(className).append(")").append(ollirT)
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

        // Return type of the method
        Type retType = types.getExprType(node);
        String retOllir = ollirTypes.toOllirType(retType);

        // If the method returns void, do not store the result in a temporary variable
        if (retType.getName().equals("void")) {
            String recvName = recvRes.getCode().replaceAll("^tmp\\d+\\.", "");
            String methodName = node.get("name");

            boolean isStatic = node.getChildren().stream()
                    .anyMatch(child -> table.getImports().stream()
                            .anyMatch(imp -> imp.equals(child.get("name"))));

            System.out.println("OLEEEE" + table.getImports());
            System.out.println("isStatic: " + isStatic);

            // Ensure we correctly handle static calls with the appropriate class or object
            if (isStatic) {
                comp.append("invokestatic(")
                        .append(recvName).append(", \"")
                        .append(methodName).append("\"");

                if (!argsCode.isEmpty()) {
                    comp.append(", ").append(argsCode);
                }

                comp.append(").V");  // Append .V for void return type
            } else {
                comp.append("invokevirtual(")
                        .append(recvName).append(", \"")
                        .append(methodName).append("\"");

                if (!argsCode.isEmpty()) {
                    comp.append(", ").append(argsCode);
                }

                comp.append(").V");  // Append .V for void return type
            }
            return new OllirExprResult("", comp);
        }

        // If method returns something other than void, handle the result as usual
        String tmp = ollirTypes.nextTemp() + retOllir;
        String recvName = recvRes.getCode().replace("tmp0.", "");
        String methodName = node.get("name");

        boolean isStatic = node.getChildren().stream()
                .filter(child -> !child.getKind().equals("Literal"))
                .anyMatch(child -> table.getImports().stream()
                        .anyMatch(imp -> imp.equals(child.get("name"))));

        comp.append(tmp).append(SPACE)
                .append(ASSIGN).append(retOllir).append(SPACE);

        if (isStatic) {
            comp.append("invokestatic(")
                    .append(recvName).append(", \"")
                    .append(methodName).append("\"");

            if (!argsCode.isEmpty()) {
                comp.append(", ").append(argsCode);
            }

            comp.append(").V");
        } else {
            comp.append("invokevirtual(")
                    .append(recvName).append(", \"")
                    .append(methodName).append("\"");

            if (!argsCode.isEmpty()) {
                comp.append(", ").append(argsCode);
            }

            comp.append(").V");
        }

        comp.append(retOllir).append(END_STMT);

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
                .append("inv ").append(exprRes.getCode())
                .append(ollirBool)
                .append(END_STMT);

        return new OllirExprResult(tmp, comp);
    }
}
