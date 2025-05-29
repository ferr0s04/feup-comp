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

        // Print the corresponding OLLIR code as a comment
        //code.append("; ").append(node).append(NL);

        return generators.apply(node);
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

        // Generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class ").append(className).append(NL);

        // Generate superclass
        var superClass = classUnit.getSuperClass();
        var fullSuperClass = superClass != null ? superClass : "java/lang/Object";
        code.append(".super ").append(fullSuperClass).append(NL).append(NL);

        // Generate default constructor
        var defaultConstructor = """
            ;default constructor
            .method public <init>()V
                aload_0
                invokespecial %s/<init>()V
                return
            .end method
            """.formatted(fullSuperClass);
        code.append(defaultConstructor);

        // Generate methods
        for (var method : ollirResult.getOllirClass().getMethods()) {
            if (method.isConstructMethod()) {
                continue;
            }
            code.append(apply(method));
        }

        return code.toString();
    }


    private String generateMethod(Method method) {
        currentMethod = method;
        var code = new StringBuilder();

        // Modifier
        var modifier = types.getModifier(method.getMethodAccessModifier());
        modifier += method.isStaticMethod() ? "static " : "";

        var methodName = method.getMethodName();

        // generate parameter list
        var params = method.getParams().stream()
                .map(param -> toJasminType(param.getType()))
                .collect(Collectors.joining());

        // Generate return type
        var returnType = toJasminType(method.getReturnType());

        code.append("\n.method ").append(modifier)
                .append(methodName)
                .append("(").append(params).append(")")
                .append(returnType).append(NL);

        // Calculate stack limit
        int stackLimit = calculateStackLimit(method);
        int localsLimit = calculateLocalsLimit(method);

        // Limits
        code.append(TAB).append(".limit stack ").append(stackLimit).append(NL);
        code.append(TAB).append(".limit locals ").append(localsLimit).append(NL);

        // Instructions
        for (var inst : method.getInstructions()) {
            if (inst instanceof CondBranchInstruction) {
                code.append(((CondBranchInstruction) inst).getLabel()).append(":\n");
            } else if (inst instanceof GotoInstruction) {
                code.append(((GotoInstruction) inst).getLabel()).append(":\n");
            }

            if (inst instanceof ReturnInstruction) {
                String lastLabel = method.getInstructions().stream()
                        .filter(i -> i instanceof GotoInstruction)  // Pega o label do último goto
                        .map(i -> ((GotoInstruction) i).getLabel())
                        .reduce((first, second) -> second)
                        .orElse(null);

                if (lastLabel != null) {
                    lastLabel = lastLabel.toLowerCase();
                    code.append(lastLabel).append(":\n");  // Usa o label original do goto
                }
            }

            var instCode = StringLines.getLines(apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));
            code.append(instCode);
        }

        code.append(".end method\n");
        currentMethod = null;
        return code.toString();
    }

    private int calculateStackLimit(Method method) {
        int maxStack = 0;
        int currentStack = 0;

        // Calculate stack usage
        for (Instruction inst : method.getInstructions()) {
            // Decrements and increments
            currentStack -= getStackConsumption(inst);
            currentStack += getStackProduction(inst);

            // Update max if needed
            maxStack = Math.max(maxStack, currentStack);
        }

        // Add margin for safety
        return maxStack + 3;
    }

    private int calculateLocalsLimit(Method method) {
        int limit = method.isStaticMethod() ? 0 : 1;

        // Space for parameters
        limit += method.getParams().size();

        // Get the highest virtual register number used
        int maxReg = method.getVarTable().values().stream()
                .mapToInt(Descriptor::getVirtualReg)
                .max()
                .orElse(0);

        // Add extra space for any temporary variables and ensure enough space
        return Math.max(maxReg + 1, limit + 10);
    }

    private int getStackConsumption(Instruction inst) {
        return switch (inst.getInstType()) {
            case BINARYOPER -> 2;
            case NOPER -> 0;
            case CALL -> {
                if (inst instanceof CallInstruction call) {
                    int base = (call instanceof InvokeVirtualInstruction) ? 1 : 0;
                    yield base + call.getOperands().size();
                }
                yield 1;
            }
            default -> 1;
        };
    }

    private int getStackProduction(Instruction inst) {
        return switch (inst.getInstType()) {
            case ASSIGN, RETURN -> 0;
            case CALL -> {
                CallInstruction call = (CallInstruction) inst;
                yield call.getReturnType().toString().equals("VOID") ? 0 : 1;
            }
            default -> 1;
        };
    }

    // Auxiliary method: Convert Ollir type to Jasmin type
    private String toJasminType(Type type) {
        if (type instanceof ArrayType) {
            return "[" + toJasminType(((ArrayType) type).getElementType());
        }
        if (type instanceof ClassType) {
            return "L" + ((ClassType) type).getName() + ";";
        }

        return switch (type.toString()) {
            case "INT32" -> "I";
            case "BOOLEAN" -> "Z";
            case "VOID" -> "V";
            case "STRING" -> "Ljava/lang/String;";
            default -> throw new NotImplementedException("Tipo não suportado: " + type);
        };
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // Se for um array, precisamos carregar o array primeiro
        if (assign.getRhs() instanceof NewInstruction newInst &&
                newInst.getReturnType() instanceof ArrayType) {
            // Carregar o tamanho do array primeiro
            code.append("ldc ").append(5).append(NL);  // Valor literal para criar o array
        }

        // Load right-hand side
        code.append(apply(assign.getRhs()));

        var lhs = assign.getDest();
        if (!(lhs instanceof Operand operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var reg = currentMethod.getVarTable().get(operand.getName());

        // Determine store instruction
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

        code.append(storePrefix).append("_").append(reg.getVirtualReg()).append(NL);
        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        String value = literal.getLiteral();

        // Handle string literals
        if (value.startsWith("\"") || isMethodName(value)) {
            return "";
        }

        // Handle boolean literals
        if (value.equals("true")) {
            return "iconst_1" + NL;
        } else if (value.equals("false")) {
            return "iconst_0" + NL;
        }

        // Handle integer literals
        try {
            int intValue = Integer.parseInt(value);
            if (intValue >= -1 && intValue <= 5) {
                return "iconst_" + (intValue == -1 ? "m1" : intValue) + NL;
            } else if (intValue >= -128 && intValue <= 127) {
                return "bipush " + intValue + NL;
            } else if (intValue >= -32768 && intValue <= 32767) {
                return "sipush " + intValue + NL;
            } else {
                return "ldc " + intValue + NL;
            }
        } catch (NumberFormatException e) {
            // For method names and other literals
            return "ldc \"" + value + "\"" + NL;
        }
    }

    // Helper method to check if a value is a method name
    private boolean isMethodName(String value) {
        return value.matches("[a-zA-Z_][a-zA-Z0-9_]*");
    }

    private String generateOperand(Operand operand) {
        // Special case for 'this'
        if (operand.getName().equals("this")) {
            return "aload_0" + NL;
        }

        // Lookup the descriptor
        Descriptor reg = currentMethod.getVarTable().get(operand.getName());

        if (reg == null) {
            // Check if it's a parameter
            for (Element param : currentMethod.getParams()) {
                if (param instanceof Operand && ((Operand) param).getName().equals(operand.getName())) {
                    // Get the index of the parameter and add offset for non-static methods
                    int paramIndex = currentMethod.isStaticMethod() ?
                            currentMethod.getParams().indexOf(param) :
                            currentMethod.getParams().indexOf(param) + 1;

                    // For array parameters, ensure index is > 1
                    if (param.getType() instanceof ArrayType) {
                        paramIndex = Math.max(2, paramIndex);
                    }

                    return getLoadPrefix(operand.getType()) + " " + paramIndex + NL;
                }
            }

            // Check if it's a static field
            if (ollirResult.getOllirClass().getImports().stream()
                    .anyMatch(imp -> imp.equals(operand.getName()) || imp.endsWith("." + operand.getName()))) {
                return "";
            }

            throw new RuntimeException("Variable '" + operand.getName() + "' not found in varTable.");
        }

        // For local variables, ensure index is greater than 1 when dealing with arrays
        int localIndex = reg.getVirtualReg();
        if (operand.getType() instanceof ArrayType) {
            localIndex = Math.max(2, localIndex);
        }

        return getLoadPrefix(operand.getType()) + "_" + localIndex + NL;
    }

    private String getLoadPrefix(Type type) {
        if (type instanceof ArrayType || type instanceof ClassType) {
            return "aload";
        }
        // Primitive types
        if (type.toString().equals("INT32") ||
                type.toString().equals("BOOLEAN")) {
            return "iload";
        }
        return "aload"; // Other types
    }

    private String getStorePrefix(Type type) {
        if (type instanceof ArrayType || type instanceof ClassType) {
            return "astore";
        }
        // Primitive types
        if (type.toString().equals("INT32") ||
                type.toString().equals("BOOLEAN")) {
            return "istore";
        }
        return "astore"; // Other types
    }


    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // Special case for AND operation
        if (binaryOp.getOperation().getOpType() == OperationType.AND) {
            // Load both operands
            code.append(apply(binaryOp.getLeftOperand()));
            code.append(apply(binaryOp.getRightOperand()));
            code.append("iand").append(NL);
            return code.toString();
        }

        if (binaryOp.getOperation().getOpType() == OperationType.ADD) {
            // Check if right operand is constant 1
            if (binaryOp.getRightOperand() instanceof LiteralElement literal &&
                    literal.getLiteral().equals("1")) {
                // Get the variable's register
                if (binaryOp.getLeftOperand() instanceof Operand operand) {
                    var reg = currentMethod.getVarTable().get(operand.getName());
                    return "iinc " + reg.getVirtualReg() + " 1" + NL;
                }
            }
        }

        // Load operands
        code.append(apply(binaryOp.getLeftOperand()));
        code.append(apply(binaryOp.getRightOperand()));

        // Generate operation instruction
        switch (binaryOp.getOperation().getOpType()) {
            case ADD -> code.append("iadd");
            case SUB -> code.append("isub");
            case MUL -> code.append("imul");
            case DIV -> code.append("idiv");
            case AND -> code.append("iand");
            case OR -> code.append("ior");
            case LTH -> code.append("iflt");
            case GTH -> code.append("ifgt");
            case EQ -> code.append("ifeq");
            case NEQ -> code.append("ifne");
            case GTE -> code.append("ifge");
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        }

        code.append(NL);
        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        // Handle return value
        if (returnInst.hasReturnValue()) {
            code.append(apply(returnInst.getOperand().orElseThrow()));

            Type returnType = returnInst.getOperand().get().getType();
            if (returnType instanceof ArrayType || returnType instanceof ClassType) {
                code.append("areturn");
            } else if (returnType.toString().equals("INT32") ||
                    returnType.toString().equals("BOOLEAN")) {
                code.append("ireturn");
            } else {
                code.append("areturn");
            }
        } else {
            code.append("return");
        }

        code.append(NL);
        return code.toString();
    }

    private String generateNew(NewInstruction newInst) {
        var code = new StringBuilder();

        // New array
        if (newInst.getReturnType() instanceof ArrayType) {
            // O tamanho do array deve ter sido carregado antes na pilha
            code.append("newarray int").append(NL);
        }
        // New object
        else {
            String className = ((ClassType) newInst.getReturnType()).getName();
            code.append("new ").append(className).append(NL);
            code.append("invokespecial ").append(className).append("/<init>()V").append(NL);
        }

        return code.toString();
    }

    private String generateInvokeSpecial(InvokeSpecialInstruction specialInst) {
        return "aload_0" + NL +
                // Call super constructor
                "invokespecial java/lang/Object/<init>()V" + NL;
    }

    private String generateGetField(GetFieldInstruction getFieldInst) {
        var code = new StringBuilder();

        // Load object reference
        code.append(apply(getFieldInst.getOperands().getFirst()));

        // Generate instruction
        String className = ((ClassType) getFieldInst.getOperands().getFirst().getType()).getName();
        String fieldName = ((Operand) getFieldInst.getOperands().get(1)).getName();
        String fieldType = toJasminType(getFieldInst.getFieldType());

        code.append("getfield ")
                .append(className)
                .append("/")
                .append(fieldName)
                .append(" ")
                .append(fieldType)
                .append(NL);

        return code.toString();
    }

    private String generatePutField(PutFieldInstruction putFieldInst) {
        var code = new StringBuilder();

        // Load object reference
        code.append(apply(putFieldInst.getOperands().getFirst()));

        // Load value to be assigned
        code.append(apply(putFieldInst.getOperands().get(2)));

        // Generate instruction
        String className = ((ClassType) putFieldInst.getOperands().getFirst().getType()).getName();
        String fieldName = ((Operand) putFieldInst.getOperands().get(1)).getName();
        String fieldType = toJasminType(putFieldInst.getFieldType());

        code.append("putfield ")
                .append(className)
                .append("/")
                .append(fieldName)
                .append(" ")
                .append(fieldType)
                .append(NL);

        return code.toString();
    }

    private String generateInvokeVirtual(InvokeVirtualInstruction virtualInst) {
        var code = new StringBuilder();

        // Load object reference and arguments
        for (Element arg : virtualInst.getOperands()) {
            code.append(apply(arg));
        }

        // Get method name
        String methodName = virtualInst.getOperands().get(1).toString();
        int dotIndex = methodName.indexOf('.');
        int pointsIndex = methodName.lastIndexOf(':');
        if (dotIndex != -1) {
            methodName = methodName.substring(pointsIndex + 2, dotIndex);
        }

        // Determine the class name
        String className = ((ClassType) virtualInst.getOperands().getFirst().getType()).getName();

        // Build parameter signature
        StringBuilder signature = new StringBuilder("(");

        // Skip first operand (the caller object)
        for (int i = 2; i < virtualInst.getOperands().size(); i++) {
            Element operand = virtualInst.getOperands().get(i);
            String jasminType = toJasminType(operand.getType());
            // Split on semicolon and take second part if it exists for array types
            if (jasminType.contains(";")) {
                String[] parts = jasminType.split(";");
                if (parts.length > 1) {
                    jasminType = parts[1];
                }
            }
            signature.append(jasminType);
        }
        signature.append(")");

        // Add return type
        signature.append(toJasminType(virtualInst.getReturnType()));

        // Generate invokevirtual instruction
        code.append("invokevirtual ")
                .append(className)
                .append("/")
                .append(methodName)
                .append(signature)
                .append(NL);

        return code.toString();
    }

    private String generateInvokeStatic(InvokeStaticInstruction staticInst) {
        var code = new StringBuilder();

        // Load method arguments
        for (Element arg : staticInst.getOperands()) {
            code.append(apply(arg));
        }

        String instStr = staticInst.toString();

        // Extract class name
        String className = null;
        int classIndex = instStr.indexOf(".CLASS");
        if (classIndex != -1) {
            int startIdx = instStr.lastIndexOf(' ', classIndex);
            if (startIdx == -1) startIdx = 0; else startIdx += 1;
            className = instStr.substring(startIdx, classIndex);
        }

        // Extract method name
        String methodName = null;
        int idx = instStr.indexOf("methodName");
        if (idx != -1) {
            int start = instStr.indexOf(":", idx);
            int end = instStr.indexOf(".", start);
            if (start != -1 && end != -1) {
                methodName = instStr.substring(start + 1, end).trim();
            }
        }

        if (methodName == null || className == null) {
            throw new RuntimeException("Could not extract class or method name from InvokeStaticInstruction");
        }

        // Generate method signature
        StringBuilder signature = new StringBuilder("(");
        for (int i = 2; i < staticInst.getOperands().size(); i++) {
            Element operand = staticInst.getOperands().get(i);
            String jasminType = toJasminType(operand.getType());
            // Split on semicolon and take second part if it exists for array types
            if (jasminType.contains(";")) {
                String[] parts = jasminType.split(";");
                if (parts.length > 1) {
                    jasminType = parts[1];
                }
            }
            signature.append(jasminType);
        }
        signature.append(")").append(toJasminType(staticInst.getReturnType()));

        // Emit Jasmin instruction
        code.append("invokestatic ")
                .append(className.replace('.', '/'))
                .append("/")
                .append(methodName)
                .append(signature)
                .append(NL);

        return code.toString();
    }

    private String generateSingleOpCond(SingleOpCondInstruction singleOpCondInst) {
        var code = new StringBuilder();

        // Load operands
        code.append(apply(singleOpCondInst.getOperands().getFirst()));

        String label = singleOpCondInst.getLabel();
        code.append("ifne ").append(label).append(NL);

        return code.toString();
    }

    private String generateOpCond(OpCondInstruction opCondInst) {
        var code = new StringBuilder();

        // Load operands
        code.append(apply(opCondInst.getOperands().getFirst()));
        code.append(apply(opCondInst.getOperands().getLast()));

        // Determine the instruction based on the operation type
        String instruction = switch (opCondInst.getCondition().getOperation().getOpType()) {
            case LTH -> "if_icmpge";
            case GTH -> "if_icmple";
            case LTE -> "if_icmpgt";
            case GTE -> "if_icmplt";
            case EQ -> "if_icmpne";
            case NEQ -> "if_icmpeq";
            default -> throw new NotImplementedException("Operador não suportado: " +
                    opCondInst.getCondition().getOperation().getOpType());
        };

        code.append(instruction).append(" ").append(opCondInst.getLabel()).append(NL);

        return code.toString();
    }

    private String generateGoto(GotoInstruction gotoInst) {
        return "goto " + gotoInst.getLabel().toLowerCase() + NL;
    }

    private String generateUnaryOp(UnaryOpInstruction unaryOpInst) {
        var code = new StringBuilder();

        // Load operand
        code.append(apply(unaryOpInst.getOperand()));

        // Apply unary operation
        if (unaryOpInst.getOperation().getOpType() == OperationType.NOTB) {
            code.append("iconst_1").append(NL);
            code.append("ixor").append(NL);
        }

        return code.toString();
    }

    private String generateArrayLength(ArrayLengthInstruction arrayLengthInst) {
        var code = new StringBuilder();

        // Load the array reference first
        code.append(apply(arrayLengthInst.getOperands().getFirst()));
        code.append("arraylength").append(NL);

        return code.toString();
    }
}