package pt.up.fe.comp.cp1;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.specs.util.SpecsIo;

import static org.junit.Assert.assertEquals;

/**
 * Test variable lookup.
 */
public class SymbolTableTest {

    static JmmSemanticsResult getSemanticsResult(String filename) {
        return TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/" + filename));
    }

    static JmmSemanticsResult test(String filename, boolean fail) {
        var semantics = getSemanticsResult(filename);
        if (fail) {
            TestUtils.mustFail(semantics.getReports());
        } else {
            TestUtils.noErrors(semantics.getReports());
        }
        return semantics;
    }


    /**
     * Test if fields are not being accessed from static methods.
     */
    @Test
    public void NumImports() {
        var semantics = test("symboltable/Imports.jmm", false);
        assertEquals(2, semantics.getSymbolTable().getImports().size());
    }

    @Test
    public void ClassAndSuper() {
        var semantics = test("symboltable/Super.jmm", false);
        assertEquals("Super", semantics.getSymbolTable().getClassName());
        assertEquals("UltraSuper", semantics.getSymbolTable().getSuper());

    }

    @Test
    public void Fields() {
        var semantics = test("symboltable/MethodsAndFields.jmm", false);
        var fields = semantics.getSymbolTable().getFields();
        assertEquals(3, fields.size());
        var checkInt = 0;
        var checkBool = 0;
        var checkObj = 0;

        for (var f : fields) {
            switch (f.getType().getName()) {
                case "MethodsAndFields":
                    checkObj++;
                    break;
                case "boolean":
                    checkBool++;
                    break;
                case "int":
                    checkInt++;
                    break;
            }
        }
        ;
        assertEquals("Field of type int", 1, checkInt);
        assertEquals("Field of type boolean", 1, checkBool);
        assertEquals("Field of type object", 1, checkObj);

    }

    @Test
    public void Methods() {
        var semantics = test("symboltable/MethodsAndFields.jmm", false);
        var st = semantics.getSymbolTable();
        var methods = st.getMethods();
        assertEquals(5, methods.size());
        var checkInt = 0;
        var checkBool = 0;
        var checkObj = 0;
        var checkAll = 0;

        for (var m : methods) {
            var ret = st.getReturnType(m);
            var numParameters = st.getParameters(m).size();

            System.out.println("Method: " + m + ", Return Type: " + ret.getName() + ", isArray: " + ret.isArray());

            switch (ret.getName()) {
                case "MethodsAndFields":
                    checkObj++;
                    assertEquals("Method " + m + " parameters", 0, numParameters);
                    break;
                case "boolean":
                    checkBool++;
                    assertEquals("Method " + m + " parameters", 0, numParameters);
                    break;
                case "int":
                    if (ret.isArray()) {
                        checkAll++;
                        assertEquals("Method " + m + " parameters", 3, numParameters);
                    } else {
                        checkInt++;
                        assertEquals("Method " + m + " parameters", 0, numParameters);
                    }
                    break;
            }
        }
        ;
        assertEquals("Method with return type int", 1, checkInt);
        assertEquals("Method with return type boolean", 1, checkBool);
        assertEquals("Method with return type object", 1, checkObj);
        assertEquals("Method with three arguments", 1, checkAll);


    }

    @Test
    public void Parameters() {
        var semantics = test("symboltable/Parameters.jmm", false);
        var st = semantics.getSymbolTable();
        var methods = st.getMethods();
        assertEquals(1, methods.size());

        var parameters = st.getParameters(methods.get(0));
        assertEquals(3, parameters.size());
        assertEquals("Parameter 1", "int", parameters.get(0).getType().getName());
        assertEquals("Parameter 2", "boolean", parameters.get(1).getType().getName());
        assertEquals("Parameter 3", "Parameters", parameters.get(2).getType().getName());
    }

    @Test
    public void Combination() {
        var semantics = test("symboltable/Combination.jmm", false);
        var st = semantics.getSymbolTable();

        // Test 3 imports
        assertEquals(3, semantics.getSymbolTable().getImports().size());

        // Test class "Combination" and super "A"
        assertEquals("Combination", semantics.getSymbolTable().getClassName());
        assertEquals("A", semantics.getSymbolTable().getSuper());

        // Test fields
        var fields = semantics.getSymbolTable().getFields();
        assertEquals(4, fields.size());
        var checkInt = 0;
        var checkBool = 0;
        var checkObj = 0;

        for (var f : fields) {
            switch (f.getType().getName()) {
                case "Combination":
                    checkObj++;
                    break;
                case "boolean":
                    checkBool++;
                    break;
                case "int":
                    checkInt++;
                    break;
            }
        }
        ;
        assertEquals("Field of type int", 1, checkInt);
        assertEquals("Field of type boolean", 1, checkBool);
        assertEquals("Field of type object", 2, checkObj);


        var methods = st.getMethods();
        assertEquals(5, methods.size());
        var checkIntM = 0;
        var checkBoolM = 0;
        var checkObjM = 0;
        var checkAllM = 0;

        for (var m : methods) {
            var ret = st.getReturnType(m);
            var numParameters = st.getParameters(m).size();

            System.out.println("Method: " + m + ", Return Type: " + ret.getName() + ", isArray: " + ret.isArray());

            switch (ret.getName()) {
                case "Combination":
                    checkObjM++;
                    assertEquals("Method " + m + " parameters", 1, numParameters);
                    break;
                case "boolean":
                    checkBoolM++;
                    assertEquals("Method " + m + " parameters", 0, numParameters);
                    break;
                case "int":
                    if (ret.isArray()) {
                        checkAllM++;
                        assertEquals("Method " + m + " parameters", 3, numParameters);
                    } else {
                        checkIntM++;
                        assertEquals("Method " + m + " parameters", 0, numParameters);
                    }
                    break;
            }
        }
        ;
        assertEquals("Method with return type int", 1, checkIntM);
        assertEquals("Method with return type boolean", 1, checkBoolM);
        assertEquals("Method with return type object", 1, checkObjM);
        assertEquals("Method with three arguments", 1, checkAllM);


        var parameters = st.getParameters(methods.get(0));
        assertEquals(3, parameters.size());
        assertEquals("Parameter 1", "int", parameters.get(0).getType().getName());
        assertEquals("Parameter 2", "boolean", parameters.get(1).getType().getName());
        assertEquals("Parameter 3", "Combination", parameters.get(2).getType().getName());

        var parameters2 = st.getParameters(methods.get(1));
        assertEquals(0, parameters2.size());

        var parameters3 = st.getParameters(methods.get(3));
        assertEquals(1, parameters3.size());

    }


}
