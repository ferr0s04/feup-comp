package pt.up.fe.comp.cp1;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class SemanticAnalysisTest2 {

    /*
     * Extra semantics tests
     *
     * BAD - test not passing for the correct reason
     * GOOD - test passing for the correct reason
     * ACCEPTABLE - test passing not exactly for the correctest reason, but the test is still valid
     */

    // ----- ArrayTest -------------------------------------------------------------

    /*@Test
    public void ArrayTest() { // BAD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/ArrayTest.jmm"));
        TestUtils.noErrors(result);
    }*/

    @Test
    public void ArrayTest2() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/ArrayTest2.jmm"));
        TestUtils.noErrors(result);
    }

    /*@Test
    public void ArrayTest3() { // BAD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/ArrayTest3.jmm"));
        TestUtils.noErrors(result);
    }*/

    @Test
    public void ArrayTest4() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/ArrayTest4.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void ArrayTest5() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/ArrayTest5.jmm"));
        TestUtils.noErrors(result);
    }

    /*@Test
    public void ArrayTestError() { // BAD
        // Site: ERROR@semantic, line 13, col 8: Argument with index 0 with type incompatible (Type [name=int, isArray=false]) of parameter type (Type [name=int, isArray=true])
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/ArrayTestError.jmm"));
        TestUtils.mustFail(result);
    }*/

    // ----- DuplicatedTest -------------------------------------------------------------

    @Test
    public void DuplicatedTest() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/DuplicatedTest.jmm"));
        TestUtils.noErrors(result);
    }

    /*@Test
    public void DuplicatedTest2() { // BAD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/DuplicatedTest2.jmm"));
        TestUtils.noErrors(result);
    }*/

    @Test
    public void DuplicatedTest3() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/DuplicatedTest3.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void DuplicatedTest4() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/DuplicatedTest4.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void DuplicatedTest5() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/DuplicatedTest5.jmm"));
        TestUtils.noErrors(result);
    }

    /*@Test
    public void DuplicatedTestError() { // BAD
        // Site: ERROR@semantic, line 1, col 0: Duplicated fields
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/DuplicatedTestError.jmm"));
        TestUtils.mustFail(result);
    }*/

    @Test
    public void DuplicatedTestError2() { // GOOD
        // Site: ERROR@semantic, line 1, col 0: Duplicated methods
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/DuplicatedTestError2.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void DuplicatedTestError3() { // GOOD
        // Site: ERROR@semantic, line 1, col 0: Duplicated methods
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/DuplicatedTestError3.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void DuplicatedTestError4() { // GOOD
        // Site: ERROR@semantic, line 2, col 4: Duplicated locals in method exemplo
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/DuplicatedTestError4.jmm"));
        TestUtils.mustFail(result);
    }

    /*@Test
    public void DuplicatedTestError5() { // BAD - Parameters are also being saved as locals (duplication)
        // Site: ERROR@semantic, line 4, col 4: Local variable 'x' redefined a parameter in method x
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/DuplicatedTestError5.jmm"));
        TestUtils.mustFail(result);
    }*/

    /*@Test
    public void DuplicatedTestError6() { // BAD
        // Site: ERROR@semantic, line 1, col 0: Duplicated methods
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/DuplicatedTestError6.jmm"));
        TestUtils.mustFail(result);
    }*/

    // ----- ImportTest -------------------------------------------------------------

    /*@Test
    public void ImportTest() { // BAD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/ImportTest.jmm"));
        TestUtils.noErrors(result);
    }*/

    @Test
    public void ImportTest2() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/ImportTest2.jmm"));
        TestUtils.noErrors(result);
    }

    /*@Test
    public void ImportTest3() { // BAD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/ImportTest3.jmm"));
        TestUtils.noErrors(result);
    }*/

    /*@Test
    public void ImportTest4() { // BAD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/ImportTest4.jmm"));
        TestUtils.noErrors(result);
    }*/

    /*@Test
    public void ImportTest5() { // BAD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/ImportTest5.jmm"));
        TestUtils.noErrors(result);
    }*/

    @Test
    public void ImportTestError() { // GOOD
        // Site: ERROR@semantic, line 6, col 12: Variable 'A' does not exist.
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/ImportTestError.jmm"));
        TestUtils.mustFail(result);
    }

    /*@Test
    public void ImportTestError2() { // BAD
        // Site: ERROR@semantic, line 1, col 0: Duplicated imported class 'c'
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/ImportTestError2.jmm"));
        TestUtils.mustFail(result);
    }*/

    /*@Test
    public void ImportTestError3() { // BAD
        // Site: ERROR@semantic, line 9, col 8: Return value of type incompatible (Type [name=a, isArray=false]) with method return type (Type [name=int, isArray=false])
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/ImportTestError3.jmm"));
        TestUtils.mustFail(result);
    }*/

    @Test
    public void ImportTestError4() { // GOOD
        // Site: ERROR@semantic, line 8, col 12: Variable 'c' does not exist.
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/ImportTestError4.jmm"));
        TestUtils.mustFail(result);
    }

    // ----- MainTest -------------------------------------------------------------

    /*@Test
    public void MainTest() { // BAD - Main parameters not in parameters
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/MainTest.jmm"));
        TestUtils.noErrors(result);
    }*/

    @Test
    public void MainTestError() { // GOOD
        // Site: ERROR@semantic, line 3, col 4: Detected unsupported feature: static methods that are not the 'main' method.
        // Although the provided code might be valid in Java, this compiler does not support it because it is not required by the project specification.
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/MainTestError.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void MainTestError2() { // GOOD - não exatamente o mesmo erro, mas motivo correto e por principio funciona bem
        // Site: ERROR@semantic, line 3, col 4: Detected unsupported feature: static methods that are not the 'main' method.
        // Although the provided code might be valid in Java, this compiler does not support it because it is not required by the project specification.
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/MainTestError2.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void MainTestError3() { // GOOD
        // Site: ERROR@semantic, line 3, col 4: Detected unsupported feature: static methods that are not the 'main' method.
        // Although the provided code might be valid in Java, this compiler does not support it because it is not required by the project specification.
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/MainTestError3.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void MainTestError4() { // GOOD
        // Site: ERROR@semantic, line 3, col 4: Detected unsupported feature: static methods that are not the 'main' method.
        // Although the provided code might be valid in Java, this compiler does not support it because it is not required by the project specification.
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/MainTestError4.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void MainTestError5() { // GOOD
        // Site:ERROR@semantic, line 3, col 4: Detected unsupported feature: static methods that are not the 'main' method.
        // Although the provided code might be valid in Java, this compiler does not support it because it is not required by the project specification.
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/MainTestError5.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void MainTestError6() { // GOOD
        // Site: ERROR@semantic, line 3, col 4: Detected unsupported feature: void methods that are not the 'main' method.
        // Although the provided code might be valid in Java, this compiler does not support it because it is not required by the project specification.
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/MainTestError6.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void MainTestError7() { // GOOD
        // Site: ERROR@semantic, line 3, col 4: Detected unsupported feature: void methods that are not the 'main' method.
        // Although the provided code might be valid in Java, this compiler does not support it because it is not required by the project specification.
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/MainTestError7.jmm"));
        TestUtils.mustFail(result);
    }

    /*@Test
    public void MainTestError8() { // BAD
        // Site: ERROR@semantic, line 3, col 12: Detected unsupported feature: arrays that are not 'int[]' or 'int...' outside of the first parameter of the main method.
        // Although the provided code might be valid in Java, this compiler does not support it because it is not required by the project specification.
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/MainTestError8.jmm"));
        TestUtils.mustFail(result);
    }*/

    @Test
    public void MainTestError9() { // GOOD
        // Site: ERROR@semantic, line 3, col 4: Detected unsupported feature: static methods that are not the 'main' method.
        // Although the provided code might be valid in Java, this compiler does not support it because it is not required by the project specification.
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/MainTestError9.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void MainTestError10() { // GOOD
        // Site: ERROR@semantic, line 3, col 28: Detected unsupported feature: main method parameter that is a vararg parameter.
        // Although the provided code might be valid in Java, this compiler does not support it because it is not required by the project specification.
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/MainTestError10.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void MainTestError11() { // GOOD
        // Site: ERROR@semantic, line 3, col 4: Detected unsupported feature: static methods that are not the 'main' method.
        // Although the provided code might be valid in Java, this compiler does not support it because it is not required by the project specification.
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/MainTestError11.jmm"));
        TestUtils.mustFail(result);
    }

    // ----- MethodCallTest -------------------------------------------------------------

    @Test
    public void MethodCallTest() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/MethodCallTest.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void MethodCallTest2() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/MethodCallTest2.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void MethodCallTestError() { // GOOD
        // Site: ERROR@semantic, line 3, col 8: Could not find method 'undefinedMethod'
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/MethodCallTestError.jmm"));
        TestUtils.mustFail(result);
    }

    /*@Test
    public void MethodCallTestError2() { // BAD
        // Site: ERROR@semantic, line 4, col 8: Could not find method 'undefinedMethod'
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/MethodCallTestError2.jmm"));
        TestUtils.mustFail(result);
    }*/

    /*@Test
    public void MethodCallTestError3() { // BAD
        // Site: ERROR@semantic, line 9, col 12: Expected method to receive 1 arguments, but got 2
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/MethodCallTestError3.jmm"));
        TestUtils.mustFail(result);
    }*/

    /*@Test
    public void MethodCallTestError4() { // BAD
        // Site: ERROR@semantic, line 5, col 12: Could not find method 'expectInt'
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/MethodCallTestError4.jmm"));
        TestUtils.mustFail(result);
    }*/

    /*@Test
    public void MethodCallTestError5() { // BAD
        // Site: ERROR@semantic, line 5, col 17: Found 'this' inside static method
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/MethodCallTestError5.jmm"));
        TestUtils.mustFail(result);
    }*/

    // ----- NArgsTest -------------------------------------------------------------

    /*@Test
    public void NArgsError() { // BAD
        // Site: ERROR@semantic, line 4, col 12: Expected method to receive 1 arguments, but got 0
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/NArgsError.jmm"));
        TestUtils.mustFail(result);
    }*/

    // ----- PublicTest -------------------------------------------------------------

    @Test
    public void PublicTest() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/PublicTest.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void PublicTest2() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/PublicTest2.jmm"));
        TestUtils.noErrors(result);
    }

    // ----- ReturnTest -------------------------------------------------------------

    @Test
    public void ReturnTest() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/ReturnTest.jmm"));
        TestUtils.noErrors(result);
    }

    /*@Test
    public void ReturnTestError() { // BAD
        // Site: ERROR@semantic, line 3, col 4: Detected unsupported feature: methods with more than one return statement.
        // Although the provided code might be valid in Java, this compiler does not support it because it is not required by the project specification.
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/ReturnTestError.jmm"));
        TestUtils.mustFail(result);
    }*/

    /*@Test
    public void ReturnTestError2() { // BAD
        // Site: ERROR@semantic, line 3, col 4: Detected unsupported feature: return statement that is not the last statement in a method.
        // Although the provided code might be valid in Java, this compiler does not support it because it is not required by the project specification.
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/ReturnTestError2.jmm"));
        TestUtils.mustFail(result);
    }*/

    /*@Test
    public void ReturnTestError3() { // BAD
        // Site: ERROR@semantic, line 3, col 4: Detected unsupported feature: return statement that is not the last statement in a method.
        // Although the provided code might be valid in Java, this compiler does not support it because it is not required by the project specification.
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/ReturnTestError3.jmm"));
        TestUtils.mustFail(result);
    }*/

    // ----- ThisTest -------------------------------------------------------------

    @Test
    public void ThisTest() { // GOOD - a little bit assumption
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/ThisTest.jmm"));
        TestUtils.noErrors(result);
    }

    /*@Test
    public void ThisTestError() { // BAD
        // Site: ERROR@semantic, line 4, col 12: Found 'this' inside static method
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/ThisTestError.jmm"));
        // Print AST
        // System.out.println(result.getRootNode().toTree());
        TestUtils.mustFail(result);
    }*/

    // ----- VarargsTest -------------------------------------------------------------

    /*@Test
    public void VarargsTest() { // BAD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/VarargsTest.jmm"));
        TestUtils.noErrors(result);
    }*/

    @Test
    public void VarargsTest2() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/VarargsTest2.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void VarargsTest3() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/VarargsTest3.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void VarargsTest4() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/VarargsTest4.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void VarargsTest5() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/VarargsTest5.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void VarargsTest6() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/VarargsTest6.jmm"));
        TestUtils.noErrors(result);
    }

    /*@Test
    public void VarargsTestError() { // BAD
        // Site: ERROR@semantic, line 2, col 4: Found varargs outside of method parameters
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/VarargsTestError.jmm"));
        TestUtils.mustFail(result);
    }*/

    /*@Test
    public void VarargsTestError2() { // BAD
        // Site: ERROR@semantic, line 3, col 4: Found varargs outside of method parameters
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/VarargsTestError2.jmm"));
        TestUtils.mustFail(result);
    }*/

    @Test
    public void VarargsTestError3() { // GOOD
        // Site: ERROR@semantic, line 2, col 4: Found 1 varargs before the last parameter of the method
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/VarargsTestError3.jmm"));
        TestUtils.mustFail(result);
    }

    /*@Test
    public void VarargsTestError4() { // BAD
        // Site: ERROR@semantic, line 3, col 8: Found varargs outside of method parameters
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/VarargsTestError4.jmm"));
        TestUtils.mustFail(result);
    }*/

    /*@Test
    public void VarargsTestError5() { // BAD
        // Site: ERROR@semantic, line 2, col 4: Found varargs outside of method parameters
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/VarargsTestError5.jmm"));
        TestUtils.mustFail(result);
    }*/

    @Test
    public void VarargsTestError6() { // GOOD
        // Site: ERROR@semantic, line 3, col 8: Return value of type incompatible (Type [name=int, isArray=true]) with method return type (Type [name=int, isArray=false])
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/VarargsTestError6.jmm"));
        TestUtils.mustFail(result);
    }

    /*@Test
    public void VarargsTestError7() { // BAD
        // Site: ERROR@semantic, line 9, col 9: Expected method to receive 1 arguments, but got 3
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/VarargsTestError7.jmm"));
        TestUtils.mustFail(result);
    }*/

    // IN REVIEW - DO NOT UNCOMMENT
    /*@Test
    public void VarargsTestError8() { // BAD
        // Site:
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis2/VarargsTestError8.jmm"));
        TestUtils.mustFail(result);
    }*/

    // ----- Parsing Errors -------------------------------------------------------------

    /*@Test
    public void error() { // BAD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/parsingerrors/error.jmm"));
        TestUtils.mustFail(result);
    }*/

    @Test
    public void syntatic_Main() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/parsingerrors/syntatic_Main.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void syntatic_MethodCallTestError4() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/parsingerrors/syntatic_MethodCallTestError4.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void syntatic_MethodCallTestError6() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/parsingerrors/syntatic_MethodCallTestError6.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void syntatic_NotInt() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/parsingerrors/syntatic_NotInt.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void syntatic_NotInt2() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/parsingerrors/syntatic_NotInt2.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void syntatic_NotInt3() { // ACCEPTABLE
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/parsingerrors/syntatic_NotInt3.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void syntatic_NotInt4() { // ACCEPTABLE
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/parsingerrors/syntatic_NotInt4.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void syntatic_StaticTestError() { // GOOD
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/parsingerrors/syntatic_StaticTestError.jmm"));
        TestUtils.mustFail(result);
    }

}
