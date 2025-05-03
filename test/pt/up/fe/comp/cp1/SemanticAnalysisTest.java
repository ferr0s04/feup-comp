package pt.up.fe.comp.cp1;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class SemanticAnalysisTest {

    @Test
    public void symbolTable() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/SymbolTable.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void varNotDeclared() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/VarNotDeclared.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void classNotImported() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ClassNotImported.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void intPlusObject() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/IntPlusObject.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void boolTimesInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/BoolTimesInt.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void arrayPlusInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayPlusInt.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void arrayAccessOnInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayAccessOnInt.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void arrayIndexNotInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayIndexNotInt.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void assignIntToBool() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/AssignIntToBool.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void objectAssignmentFail() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ObjectAssignmentFail.jmm"));
        //System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void objectAssignmentPassExtends() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ObjectAssignmentPassExtends.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void objectAssignmentPassImports() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ObjectAssignmentPassImports.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void intInIfCondition() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/IntInIfCondition.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void arrayInWhileCondition() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayInWhileCondition.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void callToUndeclaredMethod() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/CallToUndeclaredMethod.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void callToMethodAssumedInExtends() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/CallToMethodAssumedInExtends.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void callToMethodAssumedInImport() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/CallToMethodAssumedInImport.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void incompatibleArguments() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/IncompatibleArguments.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void incompatibleReturn() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/IncompatibleReturn.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void assumeArguments() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/AssumeArguments.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void varargs() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/Varargs.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void varargsWrong() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/VarargsWrong.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayInit() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayInit.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void arrayInitWrong1() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayInitWrong1.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayInitWrong2() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayInitWrong2.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    // Additional tests
    @Test
    public void addition() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/Addition.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void subtraction() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/Subtraction.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void multiplication() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/Multiplication.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void division() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/Division.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void logicalOr() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/LogicalOr.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void postIncrement() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/PostIncrement.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void preIncrement() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/PreIncrement.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void postDecrement() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/PostDecrement.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void preDecrement() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/PreDecrement.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void comparison() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/Comparison.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void methodChaining() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/MethodChaining.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void arrayInitWrong3() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayInitWrong3.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void stringSimilarToName() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/StringSimilarToName.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void arrayIndex() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayIndex.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void returnArrayDirectly() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ReturnArrayDirectly.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void arrayIndexBoolean() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayIndexBoolean.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void wrongTypes() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/WrongTypes.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void wrongTypes2() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/WrongTypes2.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void wrongTypes3() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/WrongTypes3.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void wrongTypes4() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/WrongTypes4.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void wrongTypes5() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/WrongTypes5.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayArithmetic() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayArithmetic.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void thisInStatic() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ThisInStatic.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void expressionConditionBool() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ExpressionConditionBool.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void andWithInts() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/AndWithInts.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void andWithBooleans() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/AndWithBooleans.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void andWithArrays() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/AndWithArrays.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void wrongComparison() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/WrongComparison.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void goodComparison() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/GoodComparison.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void andWithDifType() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/AndWithDifType.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void loopAndIfWrong() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/LoopAndIfWrong.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void stringConcatenation() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/StringConcatenation.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void wrongStringConcatenation() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/WrongStringConcatenation.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void functionTypes() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/FunctionTypes.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void returnWrong() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ReturnWrong.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void returnWrong2() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ReturnWrong2.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void returnWrong3() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ReturnWrong3.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void returnWrong4() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ReturnWrong4.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void returnWrong5() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ReturnWrong5.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void returnWrong6() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ReturnWrong6.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void ReturnWrong7() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ReturnWrong7.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void returnArrayUnDirectly() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ReturnArrayUnDirectly.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void MainIsAVariable() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/MainIsAVariable.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void MainWrong() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/MainWrong.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void MainWrong2() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/MainWrong2.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void LengthWrong() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/LengthWrong.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void LengthGood() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/LengthGood.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void unaryOp() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/UnaryOp.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void newImportTest() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/newImportTest.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void ArrayGood() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayGood.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void DuplicatedSymbols() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/DuplicatedSymbols.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void TwoReturns() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/TwoReturns.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void ThisReference() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ThisReference.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void ArrayAccessNotMethodCall() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayAccessNotMethodCall.jmm"));
        TestUtils.noErrors(result);
    }


}
