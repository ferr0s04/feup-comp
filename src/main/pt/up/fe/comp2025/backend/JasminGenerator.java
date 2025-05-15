package pt.up.fe.comp2025.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.inst.*;
import org.specs.comp.ollir.tree.TreeNode;
import org.specs.comp.ollir.type.ArrayType;
import org.specs.comp.ollir.type.ClassType;
import org.specs.comp.ollir.type.Type;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.specs.comp.ollir.OperandType.*;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

    private final JasminUtils types;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        types = new JasminUtils(ollirResult);

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(NewInstruction.class, this::generateNew);
        generators.put(InvokeSpecialInstruction.class, this::generateInvokeSpecial);
        generators.put(InvokeVirtualInstruction.class, this::generateInvokeVirtual);
        generators.put(InvokeStaticInstruction.class, this::generateInvokeStatic);
        generators.put(GetFieldInstruction.class, this::generateGetField);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(OpCondInstruction.class, this::generateOpCond);
        generators.put(SingleOpCondInstruction.class, this::generateSingleOpCond);
        generators.put(GotoInstruction.class, this::generateGoto);
        generators.put(UnaryOpInstruction.class, this::generateUnaryOp);
        generators.put(ArrayLengthInstruction.class, this::generateArrayLength);
    }


    private String apply(TreeNode node) {
        var code = new StringBuilder();

        // Print the corresponding OLLIR code as a comment
        //code.append("; ").append(node).append(NL);

        code.append(generators.apply(node));

        return code.toString();
    }


    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = apply(ollirResult.getOllirClass());
        }

        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {
        var code = new StringBuilder();

        // Gerar nome da classe
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class ").append(className).append(NL);

        // Gerar superclasse
        var superClass = classUnit.getSuperClass();
        var fullSuperClass = superClass != null ? superClass : "java/lang/Object";
        code.append(".super ").append(fullSuperClass).append(NL).append(NL);

        // Gerar construtor padrão
        var defaultConstructor = """
            ;default constructor
            .method public <init>()V
                aload_0
                invokespecial %s/<init>()V
                return
            .end method
            """.formatted(fullSuperClass);
        code.append(defaultConstructor);

        // Gerar código para todos os outros métodos
        for (var method : ollirResult.getOllirClass().getMethods()) {
            // Ignorar construtor, pois já foi gerado anteriormente
            if (method.isConstructMethod()) {
                continue;
            }
            code.append(apply(method));
        }

        return code.toString();
    }


    private String generateMethod(Method method) {
        //System.out.println("STARTING METHOD " + method.getMethodName());
        // set method
        currentMethod = method;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = types.getModifier(method.getMethodAccessModifier());

        var methodName = method.getMethodName();

        // TODO: Hardcoded param types and return type, needs to be expanded
        var params = "I";
        var returnType = "I";

        code.append("\n.method ").append(modifier)
                .append(methodName)
                .append("(" + params + ")" + returnType).append(NL);

        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;
        //System.out.println("ENDING METHOD " + method.getMethodName());
        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // Gerar código para carregar os valores da expressão à direita
        code.append(apply(assign.getRhs()));

        var lhs = assign.getDest();
        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;
        var reg = currentMethod.getVarTable().get(operand.getName());

        // Se não encontrar na tabela de variáveis, pode ser uma variável temporária
        if (reg == null) {
            String name = operand.getName();
            if (name.startsWith("temp")) {
                int tempNumber = Integer.parseInt(name.substring(4));
                // Determinar o prefixo baseado no tipo
                String storePrefix = getStorePrefix(operand.getType());
                return code.append(storePrefix).append(" ").append(tempNumber).append(NL).toString();
            }
            throw new RuntimeException("Variável não encontrada na tabela: " + operand.getName());
        }

        // Determinar instrução de store baseada no tipo
        String storePrefix;
        if (operand.getType() instanceof ArrayType) {
            storePrefix = "astore";
        } else if (operand.getType() instanceof ClassType) {
            storePrefix = "astore";
        } else {
            if (operand.getType().toString().equals("INT32") ||
                    operand.getType().toString().equals("BOOLEAN")) {
                storePrefix = "istore";
            } else {
                storePrefix = "astore";
            }
        }

        code.append(storePrefix).append(" ").append(reg.getVirtualReg()).append(NL);
        return code.toString();
    }

    private String getStorePrefix(Type type) {
        if (type instanceof ArrayType) {
            return "astore";
        } else if (type instanceof ClassType) {
            return "astore";
        } else if (type.toString().equals("INT32") ||
                type.toString().equals("BOOLEAN")) {
            return "istore";
        } else {
            return "astore";
        }
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // Special case for 'this'
        if (operand.getName().equals("this")) {
            return "aload_0" + NL;
        }

        // Lookup the descriptor
        Descriptor reg = currentMethod.getVarTable().get(operand.getName());

        // If the operand is not found in the varTable, throw an informative error
        if (reg == null) {
            throw new RuntimeException("Variable '" + operand.getName() + "' not found in varTable.");
        }

        // Determine the appropriate load instruction prefix
        String loadPrefix;
        if (operand.getType() instanceof ArrayType || operand.getType() instanceof ClassType) {
            loadPrefix = "aload";
        } else {
            switch (operand.getType().toString()) {
                case "INT32":
                case "BOOLEAN":
                    loadPrefix = "iload";
                    break;
                default:
                    loadPrefix = "aload";
                    break;
            }
        }

        // Emit the load instruction with the virtual register number
        return loadPrefix + " " + reg.getVirtualReg() + NL;
    }


    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // Carregar os operandos esquerdo e direito
        code.append(apply(binaryOp.getLeftOperand()));
        code.append(apply(binaryOp.getRightOperand()));

        // Determinar o prefixo do tipo (int ou boolean)
        var typePrefix = "i";

        // Determinar a operação
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "add";
            case SUB -> "sub";
            case MUL -> "mul";
            case DIV -> "div";
            case AND -> "and";
            case OR -> "or";
            case LTH -> "if_icmplt";
            case GTH -> "if_icmpgt";
            case EQ -> "if_icmpeq";
            case NEQ -> "if_icmpne";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        // Adicionar a operação ao código
        code.append(typePrefix).append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        // Se houver operando para retornar
        if (returnInst.hasReturnValue()) {
            // Gerar código para carregar o valor de retorno
            code.append(apply(returnInst.getOperand().get()));

            // Determinar instrução de retorno baseada no tipo
            Type returnType = returnInst.getOperand().get().getType();
            if (returnType instanceof ArrayType || returnType instanceof ClassType) {
                code.append("areturn");
            } else if (returnType.toString().equals("INT32") ||
                    returnType.toString().equals("BOOLEAN")) {
                code.append("ireturn");
            } else {
                // Para outros tipos de referência
                code.append("areturn");
            }
        } else {
            // Método void
            code.append("return");
        }

        code.append(NL);
        return code.toString();
    }
    private String generateNew(NewInstruction newInst) {
        var code = new StringBuilder();

        // Se for um array
        if (newInst.getReturnType() instanceof ArrayType) {
            // Gerar código para o tamanho do array
            code.append(apply(newInst.getOperands().get(0)));
            // Criar novo array de inteiros
            code.append("newarray int").append(NL);
        }
        // Se for um novo objeto
        else {
            // Nome da classe a ser instanciada
            String className = ((ClassType) newInst.getReturnType()).getName();
            // Criar nova instância
            code.append("new ").append(className).append(NL);
            // Duplicar referência no topo da pilha
            code.append("dup").append(NL);
            // Chamar construtor
            code.append("invokespecial ").append(className).append("/<init>()V").append(NL);
        }

        return code.toString();
    }

    private String generateInvokeSpecial(InvokeSpecialInstruction specialInst) {
        var code = new StringBuilder();

        // Carregar o this
        code.append("aload_0").append(NL);
        // Chamar o construtor da superclasse
        code.append("invokespecial java/lang/Object/<init>()V").append(NL);

        return code.toString();
    }

    private String generateGetField(GetFieldInstruction getFieldInst) {
        var code = new StringBuilder();

        return code.toString();
    }

    private String generatePutField(PutFieldInstruction putFieldInst) {
        var code = new StringBuilder();

        return code.toString();
    }

    private String generateInvokeVirtual(InvokeVirtualInstruction virtualInst) {
        var code = new StringBuilder();

        // Carregar this ou referência do objeto
        code.append(apply(virtualInst.getArguments().get(0)));

        // Carregar argumentos do método
        for (Element arg : virtualInst.getOperands()) {
            code.append(apply(arg));
        }

        // Gerar invokevirtual com classe, método e assinatura
        var className = ((ClassType) virtualInst.getArguments().get(0).getType()).getName();
        var methodName = virtualInst.getMethodName();
        code.append("invokevirtual ").append(className)
                .append("/").append(methodName).append("(I)I").append(NL);

        return code.toString();
    }

    private String generateInvokeStatic(InvokeStaticInstruction staticInst) {
        var code = new StringBuilder();

        // Carregar os argumentos do método na ordem
        for (Element arg : staticInst.getOperands()) {
            code.append(apply(arg));
        }

        // Gerar invokestatic com classe, método e assinatura
        var className = ((ClassType) staticInst.getReturnType()).getName();
        var methodName = staticInst.getMethodName();

        // Gerar a chamada do método estático
        code.append("invokestatic ")
                .append(className)
                .append("/")
                .append(methodName)
                .append("(")
                // TODO: adicionar tipos dos parâmetros dinamicamente
                .append("I)I")
                .append(NL);

        return code.toString();
    }

    private String generateSingleOpCond(SingleOpCondInstruction singleOpCondInst) {
        var code = new StringBuilder();

        // Carregar operando
        code.append(apply(singleOpCondInst.getOperands().getFirst()));

        String label = singleOpCondInst.getLabel();
        code.append("ifne ").append(label).append(NL);

        return code.toString();
    }

    private String generateOpCond(OpCondInstruction opCondInst) {
        var code = new StringBuilder();

        // Carregar operandos
        code.append(apply(opCondInst.getOperands().getFirst()));
        code.append(apply(opCondInst.getOperands().getLast()));

        // Determinar instrução de comparação baseada no operador
        String instruction = switch (opCondInst.getCondition().getOperation().getOpType()) {
            case LTH -> "if_icmplt";
            case GTH -> "if_icmpgt";
            case LTE -> "if_icmple";
            case GTE -> "if_icmpge";
            case EQ -> "if_icmpeq";
            case NEQ -> "if_icmpne";
            default -> throw new NotImplementedException("Operador não suportado: " + opCondInst.getCondition().getOperation().getOpType());
        };

        // Adicionar instrução de salto com o label
        code.append(instruction).append(" ").append(opCondInst.getLabel()).append(NL);

        return code.toString();
    }

    private String generateGoto(GotoInstruction gotoInst) {
        // Gerar instrução de salto incondicional
        return "goto " + gotoInst.getLabel() + NL;
    }

    private String generateUnaryOp(UnaryOpInstruction unaryOpInst) {
        var code = new StringBuilder();

        // Carregar operando
        code.append(apply(unaryOpInst.getOperand()));

        // Aplicar operação unária
        if (unaryOpInst.getOperation().getOpType() == OperationType.NOTB) {
            code.append("iconst_1").append(NL);
            code.append("ixor").append(NL);
        }

        return code.toString();
    }

    private String generateArrayLength(ArrayLengthInstruction arrayLengthInst) {
        var code = new StringBuilder();

        // Carregar referência do array
        code.append(apply(arrayLengthInst.getOperands().get(0)));
        // Obter comprimento do array
        code.append("arraylength").append(NL);

        return code.toString();
    }
}