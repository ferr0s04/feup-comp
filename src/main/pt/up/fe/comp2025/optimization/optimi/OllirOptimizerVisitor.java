package pt.up.fe.comp2025.optimization.optimi;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.inst.*;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.*;

public class OllirOptimizerVisitor {
    private boolean regAllocEnabled = false;

    public OllirResult optimize(OllirResult ollirResult) {
        var classUnit = ollirResult.getOllirClass();
        var config = ollirResult.getConfig();
        regAllocEnabled = config.containsKey("registerAllocation");

        if (regAllocEnabled) {
            for (var method : classUnit.getMethods()) {
                if (method.isConstructMethod() || method.getMethodName().equals("main"))
                    continue;

                performRegisterAllocation(method);
            }
        }
        return ollirResult;
    }

    private void performRegisterAllocation(Method method) {
        var instructions = method.getInstructions();
        var varTable = method.getVarTable();

        // Liveness analysis
        Map<Integer, Set<String>> liveIn = new HashMap<>();
        Map<Integer, Set<String>> liveOut = new HashMap<>();
        computeLiveness(method, liveIn, liveOut);

        // Build interference graph
        Map<String, Set<String>> interferenceGraph = buildInterferenceGraph(method, liveIn, liveOut);

        // Graph coloring (register allocation)
        Map<String, Integer> colorAssignment = colorGraph(interferenceGraph, method);

        // Update virtual registers
        for (var entry : colorAssignment.entrySet()) {
            var varDesc = varTable.get(entry.getKey());
            if (varDesc != null) {
                varDesc.setVirtualReg(entry.getValue());
            }
        }
    }

    private void computeLiveness(Method method, Map<Integer, Set<String>> liveIn, Map<Integer, Set<String>> liveOut) {
        var instructions = method.getInstructions();
        boolean changed;

        // Initialize
        for (int i = 0; i < instructions.size(); i++) {
            liveIn.put(i, new HashSet<>());
            liveOut.put(i, new HashSet<>());
        }

        // Iterative algorithm for liveness analysis
        do {
            changed = false;
            for (int i = instructions.size() - 1; i >= 0; i--) {
                Set<String> oldLiveIn = new HashSet<>(liveIn.get(i));
                Set<String> oldLiveOut = new HashSet<>(liveOut.get(i));

                // Get used and defined variables for this instruction
                Set<String> used = getUsedVars(instructions.get(i));
                Set<String> defined = getDefinedVars(instructions.get(i));

                // Calculate new liveOut
                Set<String> newLiveOut = new HashSet<>();
                // If not the last instruction, add liveIn of next instruction
                if (i < instructions.size() - 1) {
                    newLiveOut.addAll(liveIn.get(i + 1));
                }

                // Calculate new liveIn: used + (liveOut - defined)
                Set<String> newLiveIn = new HashSet<>(used);
                Set<String> liveOutMinusDef = new HashSet<>(newLiveOut);
                liveOutMinusDef.removeAll(defined);
                newLiveIn.addAll(liveOutMinusDef);

                // Update if changed
                if (!newLiveIn.equals(oldLiveIn) || !newLiveOut.equals(oldLiveOut)) {
                    changed = true;
                    liveIn.put(i, newLiveIn);
                    liveOut.put(i, newLiveOut);
                }
            }
        } while (changed);
    }


    private Map<String, Set<String>> buildInterferenceGraph(Method method,
                                                            Map<Integer, Set<String>> liveIn,
                                                            Map<Integer, Set<String>> liveOut) {
        Map<String, Set<String>> graph = new HashMap<>();

        // Initialize graph
        for (var varName : method.getVarTable().keySet()) {
            graph.put(varName, new HashSet<>());
        }

        // First, consider the overlap between definition points and liveness
        for (int i = 0; i < method.getInstructions().size(); i++) {
            Instruction inst = method.getInstructions().get(i);

            // Variables defined in this instruction
            Set<String> defs = getDefinedVars(inst);

            // Variables live out from this instruction
            Set<String> liveOutVars = new HashSet<>(liveOut.get(i));

            // For each defined variable, add interference with all live-out variables
            for (String def : defs) {
                for (String liveVar : liveOutVars) {
                    if (!def.equals(liveVar)) {
                        graph.get(def).add(liveVar);
                        graph.get(liveVar).add(def);
                    }
                }
            }

            // Ensure the defined variable interferes with any used vars in the same instruction
            Set<String> usedVars = getUsedVars(inst);
            for (String def : defs) {
                for (String used : usedVars) {
                    if (!def.equals(used)) {
                        graph.get(def).add(used);
                        graph.get(used).add(def);
                    }
                }
            }

        }

        // Second, add interferences between variables that are simultaneously live
        for (String var1 : method.getVarTable().keySet()) {
            for (String var2 : method.getVarTable().keySet()) {
                if (var1.equals(var2)) continue;

                // Check if these variables are ever live at the same time
                boolean interfere = false;
                for (int i = 0; i < method.getInstructions().size(); i++) {
                    Set<String> liveAtPoint = new HashSet<>();
                    liveAtPoint.addAll(liveIn.get(i));
                    liveAtPoint.addAll(liveOut.get(i));

                    if (liveAtPoint.contains(var1) && liveAtPoint.contains(var2)) {
                        interfere = true;
                        break;
                    }
                }

                if (interfere) {
                    graph.get(var1).add(var2);
                    graph.get(var2).add(var1);
                }
            }
        }

        return graph;
    }

    private Map<String, Integer> colorGraph(Map<String, Set<String>> graph, Method method) {
        Map<String, Integer> colors = new HashMap<>();
        List<String> nodes = new ArrayList<>(graph.keySet());

        Set<String> skipTemps = new HashSet<>();
        for (String node : nodes) {
            if (node.startsWith("tmp")) {
                boolean usedElsewhere = false;
                for (Instruction inst : method.getInstructions()) {
                    if (getUsedVars(inst).contains(node)) {
                        usedElsewhere = true;
                        break;
                    }
                }
                if (!usedElsewhere) {
                    skipTemps.add(node);
                }
            }
        }

        nodes.removeAll(skipTemps);

        // Direct dependency graph
        Map<String, Set<String>> assignGraph = new HashMap<>();
        for (Instruction inst : method.getInstructions()) {
            if (inst instanceof AssignInstruction assign) {
                Element dest = assign.getDest();
                Instruction rhs = assign.getRhs();
                if (dest instanceof Operand destOp && rhs instanceof SingleOpInstruction singleOp) {
                    Element src = singleOp.getSingleOperand();
                    if (src instanceof Operand srcOp) {
                        assignGraph.computeIfAbsent(destOp.getName(), k -> new HashSet<>()).add(srcOp.getName());
                        assignGraph.computeIfAbsent(srcOp.getName(), k -> new HashSet<>()).add(destOp.getName());
                    }
                }
            }
        }

        // Find chains
        Map<String, Set<String>> chains = new HashMap<>();
        Set<String> visited = new HashSet<>();
        for (String var : assignGraph.keySet()) {
            if (visited.contains(var)) continue;
            Set<String> chain = new HashSet<>();
            Queue<String> queue = new LinkedList<>();
            queue.add(var);
            while (!queue.isEmpty()) {
                String curr = queue.poll();
                if (!visited.add(curr)) continue;
                chain.add(curr);
                for (String neighbor : assignGraph.getOrDefault(curr, Set.of())) {
                    if (!visited.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
            for (String v : chain) {
                chains.put(v, chain);
            }
        }

        // Registers for arg and 'this'
        int nextColor = 0;
        Set<Integer> reservedRegisters = new HashSet<>();
        for (String node : nodes) {
            if (node.startsWith("arg") || node.equals("this")) {
                colors.put(node, nextColor++);
                reservedRegisters.add(nextColor - 1);
            }
        }

        // Same register for variables in the same chain
        Set<Set<String>> usedChains = new HashSet<>();
        for (String node : nodes) {
            if (chains.containsKey(node) && !usedChains.contains(chains.get(node))) {
                for (String v : chains.get(node)) {
                    if (!colors.containsKey(v)) {
                        colors.put(v, nextColor);
                    }
                }
                usedChains.add(chains.get(node));
                nextColor++;
            }
        }

        // Registers for variables not in chains
        for (String node : nodes) {
            if (colors.containsKey(node)) continue;
            Set<Integer> usedColors = new HashSet<>();
            for (String neighbor : graph.get(node)) {
                if (colors.containsKey(neighbor)) {
                    usedColors.add(colors.get(neighbor));
                }
            }
            int color = 0;
            while (usedColors.contains(color) || reservedRegisters.contains(color)) {
                color++;
            }
            colors.put(node, color);
        }

        return colors;
    }


    private Set<String> getUsedVars(Instruction inst) {
        Set<String> used = new HashSet<>();

        if (inst instanceof AssignInstruction assign) {
            // For assignments, the RHS uses variables
            Instruction rhs = assign.getRhs();
            used.addAll(extractOperands(rhs));
        } else {
            // For other instructions, extract all operands
            used.addAll(extractOperands(inst));
        }

        return used;
    }

    private Set<String> extractOperands(Instruction inst) {
        Set<String> used = new HashSet<>();

        if (inst instanceof BinaryOpInstruction binOp) {
            addOperandIfExists(used, binOp.getLeftOperand());
            addOperandIfExists(used, binOp.getRightOperand());

        } else if (inst instanceof OpInstruction opInst) {
            for (Element e : opInst.getOperands()) {
                addOperandIfExists(used, e);
            }

        } else if (inst instanceof CallInstruction callInst) {
            // Caller
            addOperandIfExists(used, callInst.getOperands().getFirst());
            // Arguments
            for (Element arg : callInst.getOperands()) {
                addOperandIfExists(used, arg);
            }

        } else if (inst instanceof SingleOpInstruction singleOp) {
            addOperandIfExists(used, singleOp.getSingleOperand());

        } else if (inst instanceof ReturnInstruction returnInst) {
            if (returnInst.hasReturnValue()) {
                addOperandIfExists(used, returnInst.getOperand().orElseThrow());
            }

        } else if (inst instanceof CondBranchInstruction condBranch) {
            used.addAll(extractOperands(condBranch.getCondition()));
        }

        return used;
    }


    private Set<String> getDefinedVars(Instruction inst) {
        Set<String> defined = new HashSet<>();

        if (inst instanceof AssignInstruction assign) {
            Element dest = assign.getDest();
            if (dest instanceof Operand operand) {
                String varName = operand.getName();
                defined.add(varName);
            }
        }

        return defined;
    }

    private void addOperandIfExists(Set<String> vars, Element element) {
        if (element instanceof Operand op) {
            vars.add(op.getName());
        }
    }
}