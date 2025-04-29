package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.List;

public class AstOptimizerVisitor extends AJmmVisitor<Void, Void> {

    @Override
    protected void buildVisitor() {
        addVisit("BinaryOp", this::visitBinaryOp);
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
        if (left.getKind().equals("IntegerLiteral") && right.getKind().equals("IntegerLiteral")) {
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

            // Create a new IntegerLiteral node with the folded result
            JmmNode folded = new JmmNodeImpl(List.of("IntegerLiteral"));
            folded.put("value", String.valueOf(result));

            // Replace current BinaryOp node with the new folded literal
            node.replace(folded);
        }

        return null;
    }

    private Void defaultVisit(JmmNode node, Void unused) {
        for (JmmNode child : node.getChildren()) {
            visit(child);
        }
        return null;
    }
}
