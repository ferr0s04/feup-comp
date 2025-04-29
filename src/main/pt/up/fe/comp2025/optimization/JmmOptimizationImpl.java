package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;

public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        // Create visitor that will generate the OLLIR code
        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());

        // Visit the AST and obtain OLLIR code
        var ollirCode = visitor.visit(semanticsResult.getRootNode());

        //System.out.println("\nOLLIR:\n\n" + ollirCode);

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        // Check if optimization is enabled
        var optimizeFlag = semanticsResult.getConfig()
                .getOrDefault("optimize", "false")
                .equals("true");

        if (!optimizeFlag) {
            return semanticsResult;
        }

        // Apply AST-level optimizations in-place
        var optimizer = new AstOptimizerVisitor();
        optimizer.visit(semanticsResult.getRootNode());

        // Return the updated semantics result (AST is modified in-place)
        return new JmmSemanticsResult(
                semanticsResult.getRootNode(),
                semanticsResult.getSymbolTable(),
                semanticsResult.getReports(),
                semanticsResult.getConfig()
        );
    }



    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        return new OllirOptimizer().optimize(ollirResult);
    }





}
