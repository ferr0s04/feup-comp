package pt.up.fe.comp2025.optimization.optimi;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AstOptimizerVisitor extends AJmmVisitor<Void, Void> {

    @Override
    protected void buildVisitor() {
        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("AssignStmt", this::visitAssignment);
        addVisit("Identifier", this::visitIdentifier);
        addVisit("ReturnStmt", this::visitReturn);
        addVisit("WhileStmt", this::visitWhileStmt);
        setDefaultVisit(this::defaultVisit);
    }

    private Void visitBinaryOp(JmmNode node, Void unused) {
        // Recursively visit children first
        for (JmmNode child : node.getChildren()) {
            visit(child);
        }

        String op = node.get("op");
        JmmNode left = node.getChild(0);
        JmmNode right = node.getChild(1);

        // Check if both operands are integer literals
        if (left.getKind().equals("Literal") && right.getKind().equals("Literal")) {
            int leftVal = Integer.parseInt(left.get("value"));
            int rightVal = Integer.parseInt(right.get("value"));
            int result;

            switch (op) {
                case "+" -> result = leftVal + rightVal;
                case "-" -> result = leftVal - rightVal;
                case "*" -> result = leftVal * rightVal;
                case "/" -> result = rightVal != 0 ? leftVal / rightVal : 0; // Prevent division by 0
                default -> {
                    return null; // Unsupported operation
                }
            }

            // Create a new Literal node with the folded result
            JmmNode folded = new JmmNodeImpl(List.of("Literal"));
            folded.put("value", String.valueOf(result));

            // Replace current BinaryOp node with the new folded literal
            node.replace(folded);
        }

        return null;
    }

    private final Map<String, String> constantTable = new HashMap<>();

    private Void visitAssignment(JmmNode node, Void unused) {
        String varName = node.get("name");
        JmmNode valueNode = node.getChild(0);

        // Propaga constantes
        if (valueNode.getKind().equals("Literal")) {
            constantTable.put(varName, valueNode.get("value"));
        } else {
            constantTable.remove(varName);
        }

        visit(valueNode);
        return null;
    }

    private Void visitIdentifier(JmmNode node, Void unused) {
        String varName = node.get("name");

        // Substitui a variável pelo valor constante, se existir
        if (constantTable.containsKey(varName)) {
            JmmNode constantNode = new JmmNodeImpl(Collections.singletonList("Literal"));
            constantNode.put("value", constantTable.get(varName));
            node.replace(constantNode);
        }

        return null;
    }

    private Void visitReturn(JmmNode node, Void unused) {
        JmmNode returnValue = node.getChild(0);

        // Verifica se o retorno é uma variável
        if (returnValue.getKind().equals("Identifier")) {
            String varName = returnValue.get("name");

            // Substitui a variável pelo valor constante, se existir
            if (constantTable.containsKey(varName)) {
                JmmNode constantNode = new JmmNodeImpl(Collections.singletonList("Literal"));
                constantNode.put("value", constantTable.get(varName));
                returnValue.replace(constantNode);
            }
        }

        return null;
    }

    private Void visitWhileStmt(JmmNode node, Void unused) {
        // Otimiza a condição do loop
        JmmNode condition = node.getChild(0);
        visit(condition);

        if (condition.getKind().equals("BinaryOp")) {
            JmmNode left = condition.getChild(0);
            JmmNode right = condition.getChild(1);

            // Substitui variáveis por constantes, se aplicável
            if (left.getKind().equals("Identifier") && constantTable.containsKey(left.get("name"))) {
                left.put("value", constantTable.get(left.get("name")));
                left.put("kind", "Literal");
            }
            if (right.getKind().equals("Identifier") && constantTable.containsKey(right.get("name"))) {
                right.put("value", constantTable.get(right.get("name")));
                right.put("kind", "Literal");
            }

            // Simplifica a condição se ambos os lados forem literais
            if (left.getKind().equals("Literal") && right.getKind().equals("Literal")) {
                int leftVal = Integer.parseInt(left.get("value"));
                int rightVal = Integer.parseInt(right.get("value"));
                boolean result = switch (condition.get("op")) {
                    case "<" -> leftVal < rightVal;
                    case ">" -> leftVal > rightVal;
                    case "<=" -> leftVal <= rightVal;
                    case ">=" -> leftVal >= rightVal;
                    case "==" -> leftVal == rightVal;
                    case "!=" -> leftVal != rightVal;
                    default -> throw new IllegalArgumentException("Operador desconhecido: " + condition.get("op"));
                };

                condition.put("kind", "Literal");
                condition.put("value", result ? "1" : "0");
            }
        }

        // Otimiza o corpo do loop
        JmmNode body = node.getChild(1);
        visit(body);

        return null;
    }

    private Void defaultVisit(JmmNode node, Void unused) {
        for (JmmNode child : node.getChildren()) {
            visit(child);
        }
        return null;
    }
}
