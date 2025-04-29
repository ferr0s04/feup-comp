package pt.up.fe.comp2025.optimization;

import org.specs.comp.ollir.Method;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.HashMap;
import java.util.Map;

public class OllirOptimizer {

    public OllirResult optimize(OllirResult ollirResult) {
        var classUnit = ollirResult.getOllirClass();

        for (var method : classUnit.getMethods()) {
            if (method.isConstructMethod() || method.getMethodName().equals("main")) continue;
            performRegisterAllocation(method);
        }

        return ollirResult;
    }


    private void performRegisterAllocation(Method method) {
        var varTable = method.getVarTable();
        int currentReg = 0;

        System.out.println("Methood: " + method.getMethodName());
        for (var entry : method.getVarTable().entrySet()) {
            System.out.println("Var: " + entry.getKey() + ", Info: " + entry.getValue());
        }

        for (var entry : varTable.entrySet()) {
            String varName = entry.getKey();

            // ✅ Skip synthetic or implicit vars
            if (varName.equals("ret") || varName.equals("this")) continue;
            if (method.isConstructMethod() || method.getMethodName().equals("main")) continue;


            entry.getValue().setVirtualReg(currentReg++);
        }
    }


}
