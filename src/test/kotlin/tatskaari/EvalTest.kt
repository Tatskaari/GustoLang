package tatskaari

import org.testng.annotations.Test
import tatskaari.eval.Eval
import tatskaari.eval.MutEnv
import tatskaari.parsing.Parser
import tatskaari.tokenising.Lexer
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.StringReader
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

object EvalTest {
  @Test
  fun testBasicEval() {
    val program = TestUtil.loadProgram("TestMain")
    val env = MutEnv()
    val parser = Parser()
    val AST = parser.parse(program)!!
    Eval().eval(AST, env)

    assertEquals(1, env.getValue("b").intVal())
  }

  @Test
  fun numValInCondition() {
    assertFailsWith<Eval.TypeMismatch> {
      val program = "{if (1) {}}"
      val env = MutEnv()
      Eval().eval(Parser().parse(program)!!, env)
    }
  }

  @Test
  fun missingIdentifier() {
    assertFailsWith<Eval.UndefinedIdentifier> {
      val program = "{if (1 = a) {}}"
      val env = MutEnv()
      Eval().eval(Parser().parse(program)!!, env)
    }
  }

  @Test
  fun numIdentifierAssignBoolean() {
    assertFailsWith<Eval.TypeMismatch> {
      val program = "{val a := 1 a := 1 = 1}"
      val env = MutEnv()
      Eval().eval(Parser().parse(program)!!, env)
    }
  }

  @Test
  fun testAddBoolRHS() {
    assertFailsWith<Eval.TypeMismatch> {
      val program = "{val a := (1 = 1) + 1}"
      val env = MutEnv()
      Eval().eval(Parser().parse(program)!!, env)
    }
  }

  @Test
  fun testAddBoolLHS() {
    assertFailsWith<Eval.TypeMismatch> {
      val program = "{val a := 1 + (1 = 1) }"
      val env = MutEnv()
      Eval().eval(Parser().parse(program)!!, env)
    }
  }

  @Test
  fun testAdd() {
    val program = "val a := 1 + 1"
    val env = MutEnv()
    Eval().eval(Parser().parse(program)!!, env)

    assertEquals(2, env.getValue("a").intVal())

  }

  @Test
  fun testSub() {
    val program = "val a := 1 - 1"
    val env = MutEnv()
    Eval().eval(Parser().parse(program)!!, env)

    assertEquals(0, env.getValue("a").intVal())
  }

  @Test
  fun testNumInput() {
    val inputReader = BufferedReader(StringReader("1234"))
    val env = MutEnv()
    Eval(inputReader).eval(Parser().parse("input a")!!, env)
    assertEquals(1234, env.getValue("a").intVal())
  }

  @Test
  fun testTrueInput() {
    val inputReader = BufferedReader(StringReader("true"))
    val env = MutEnv()
    Eval(inputReader).eval(Parser().parse("input a")!!, env)
    assertEquals(true, env.getValue("a").boolVal())
  }

  @Test
  fun testFalseInput() {
    val inputReader = BufferedReader(StringReader("false"))
    val env = MutEnv()
    Eval(inputReader).eval(Parser().parse("input a")!!, env)
    assertEquals(false, env.getValue("a").boolVal())
  }

  @Test
  fun testNullInput() {
    assertFailsWith<Eval.InvalidUserInput> {
      val inputReader = BufferedReader(StringReader("\n"))
      val env = MutEnv()
      Eval(inputReader).eval(Parser().parse("{input a}")!!, env)
    }

    assertFailsWith<Eval.InvalidUserInput> {
      val inputReader2 = BufferedReader(StringReader(""))
      val env2 = MutEnv()
      Eval(inputReader2).eval(Parser().parse("{input a}")!!, env2)
    }
  }

  @Test
  fun outputTest() {
    val outStream = ByteArrayOutputStream()
    val printStream = PrintStream(outStream)
    Eval(printStream).eval(Parser().parse("{output 1}")!!, MutEnv())
    val output = String(outStream.toByteArray())

    assertEquals('1', output[0])

  }

  @Test
  fun ifElseTest() {
    val program = TestUtil.loadProgram("IfElse")

    var env = MutEnv()
    var inputReader = BufferedReader(StringReader("10"))
    Eval(inputReader).eval(Parser().parse(program)!!, env)

    assertEquals(1, env.getValue("someVar").intVal())

    env = MutEnv()
    inputReader = BufferedReader(StringReader("11"))
    Eval(inputReader).eval(Parser().parse(program)!!, env)

    assertEquals(2, env.getValue("someVar").intVal())
  }

  @Test
  fun testBoolTrue() {
    val program = Parser().parse("val a := true")!!
    val env = MutEnv()
    Eval().eval(program, env)
    assertEquals(true, env.getValue("a").boolVal())
  }

  @Test
  fun testBoolFalse() {
    val program = Parser().parse("val a := false")!!
    val env = MutEnv()
    Eval().eval(program, env)
    assertEquals(false, env.getValue("a").boolVal())
  }

  @Test
  fun testNot() {
    val program = Parser().parse("val a := !false")!!
    val env = MutEnv()
    Eval().eval(program, env)
    assertEquals(true, env.getValue("a").boolVal())
  }

  @Test
  fun testIfLiteral() {
    val program = Parser().parse("val a := 0 if (true) {a := 1} else {a := 2}")!!
    val env = MutEnv()
    Eval().eval(program, env)
    assertEquals(1, env.getValue("a").intVal())

    val program2 = Parser().parse("val a := 0 if (false) {a := 1} else { a := 2}")!!
    val env2 = MutEnv()
    Eval().eval(program2, env2)
    assertEquals(2, env2.getValue("a").intVal())
  }

  @Test
  fun testWhile() {
    val program = Parser().parse(TestUtil.loadProgram("While"))!!
    val env = MutEnv()
    Eval().eval(program, env)
    assertEquals(20, env.getValue("b").intVal())
  }

  @Test
  fun whileWithNonBoolResult() {
    val program = Parser().parse("while (1) { }")!!
    assertFailsWith<Eval.TypeMismatch> {
      Eval().eval(program, MutEnv())
    }
  }

  @Test
  fun fibTest() {
    val program = Parser().parse(TestUtil.loadProgram("Fib"))!!
    val env = MutEnv()
    Eval().eval(program, env)
    assertEquals(13, env.getValue("c").intVal())
  }

  @Test
  fun redeclareVal() {
    val program = Parser().parse("val a := 1 val a := 2")!!
    assertFailsWith<Eval.VariableAlreadyDefined> {
      Eval().eval(program, MutEnv())
    }
  }

  @Test
  fun testAnd() {
    val program = Parser().parse("val a := 0 if(true and true) {a := 1} else {a := 2}")!!
    val env = MutEnv()
    Eval().eval(program,env)
    assertEquals(1, env.getValue("a").intVal())
  }

  @Test
  fun testOr() {
    val program = Parser().parse("val a := 0 if(true or false) {a := 1} else {a := 2}")!!
    val env = MutEnv()
    Eval().eval(program,env)
    assertEquals(1, env.getValue("a").intVal())
  }

  @Test
  fun testNotAnd() {
    val program = Parser().parse("val a := 0 if(true and false) {a := 1} else {a := 2}")!!
    val env = MutEnv()
    Eval().eval(program,env)
    assertEquals(2, env.getValue("a").intVal())
  }

  @Test
  fun testNotOr() {
    val program = Parser().parse("val a := 0 if(false or false) {a := 1} else {a := 2}")!!
    val env = MutEnv()
    Eval().eval(program,env)
    assertEquals(2, env.getValue("a").intVal())
  }

  @Test
  fun testLT() {
    val program = Parser().parse("val a := 0 if(1 < 2) {a := 1} else {a := 2}")!!
    val env = MutEnv()
    Eval().eval(program,env)
    assertEquals(1, env.getValue("a").intVal())
  }

  @Test
  fun testGT() {
    val program = Parser().parse("val a := 0 if(2 > 1) {a := 1} else {a := 2}")!!
    val env = MutEnv()
    Eval().eval(program,env)
    assertEquals(1, env.getValue("a").intVal())
  }

  @Test
  fun testGTE() {
    val program = Parser().parse("val a := 0 if(1 >= 1) {a := 1} else {a := 2}")!!
    val env = MutEnv()
    Eval().eval(program,env)
    assertEquals(1, env.getValue("a").intVal())
  }

  @Test
  fun testLTE() {
    val program = Parser().parse("val a := 0 if(1 <= 1) {a := 1} else {a := 2}")!!
    val env = MutEnv()
    Eval().eval(program,env)
    assertEquals(1, env.getValue("a").intVal())
  }


  @Test
  fun testNotLT() {
    val program = Parser().parse("val a := 0 if(2 < 1) {a := 1} else {a := 2}")!!
    val env = MutEnv()
    Eval().eval(program,env)
    assertEquals(2, env.getValue("a").intVal())
  }

  @Test
  fun testNotGT() {
    val program = Parser().parse("val a := 0 if(1 > 2) {a := 1} else {a := 2}")!!
    val env = MutEnv()
    Eval().eval(program,env)
    assertEquals(2, env.getValue("a").intVal())
  }

  @Test
  fun testNotGTE() {
    val program = Parser().parse("val a := 0 if(1 >= 2) {a := 1} else {a := 2}")!!
    val env = MutEnv()
    Eval().eval(program,env)
    assertEquals(2, env.getValue("a").intVal())
  }

  @Test
  fun testNotLTE() {
    val program = Parser().parse("val a := 0 if(2 <= 1) {a := 1} else {a := 2}")!!
    val env = MutEnv()
    Eval().eval(program,env)
    assertEquals(2, env.getValue("a").intVal())
  }

  @Test
  fun testMul() {
    val program = Parser().parse("val a := 2 * 3")!!
    val env = MutEnv()
    Eval().eval(program,env)
    assertEquals(6, env.getValue("a").intVal())
  }

  @Test
  fun testDiv() {
    val program = Parser().parse("val a := 9 / 3")!!
    val env = MutEnv()
    Eval().eval(program,env)
    assertEquals(3, env.getValue("a").intVal())
  }

  @Test
  fun testFunctionCall() {
    val program = Parser().parse(TestUtil.loadProgram("Function"))!!
    val env = MutEnv()
    Eval().eval(program, env)
    assertEquals(3, env.getValue("c").intVal())
  }

  @Test
  fun missingFuncDef(){
    val program = Parser().parse("val a := add(1, 2)")!!
    assertFailsWith<Eval.UndefinedIdentifier> {
      Eval().eval(program, MutEnv())
    }
  }

  @Test
  fun missingReturnFromFunc(){
    val program = Parser().parse("function add(a, b) { } val c := add(1, 2)")!!
    assertFailsWith<Eval.FunctionExitedWithoutReturn> {
      Eval().eval(program, MutEnv())
    }
  }

  @Test
  fun callingInt(){
    val program = Parser().parse("val a := 2 val b := a()")!!
    assertFailsWith<Eval.TypeMismatch> {
      Eval().eval(program, MutEnv())
    }
  }

  @Test
  fun wrongNumParams(){
    val program = Parser().parse("function add(a, b) { return a + b } val b := add(1)")!!
    assertFailsWith<Eval.TypeMismatch> {
      Eval().eval(program, MutEnv())
    }
  }

  @Test
  fun paramDoesntExist(){
    val program = Parser().parse("function add(a, b) { return a + b } val b := add(1, 2, 3)")!!
    assertFailsWith<Eval.TypeMismatch> {
      Eval().eval(program, MutEnv())
    }
  }

  @Test
  fun evalFuncFib(){
    val program = Parser().parse(TestUtil.loadProgram("FuncFib"))!!
    val env = MutEnv()
    Eval().eval(program, env)
    assertEquals(13, env.getValue("out").intVal())
  }

  @Test
  fun whileReturn(){
    val program = Parser().parse(TestUtil.loadProgram("WhileReturn"))!!
    val env = MutEnv()
    Eval().eval(program, env)
    assertEquals(100, env.getValue("out").intVal())
  }

  @Test
  fun testUnaryOperatorTypeMissmatch(){
    assertFailsWith<Eval.TypeMismatch> {
      Eval().eval(Parser().parse("val a := !1")!!, MutEnv())
    }
    assertFailsWith<Eval.TypeMismatch> {
      Eval().eval(Parser().parse("val a := -true")!!, MutEnv())
    }
  }

  @Test
  fun testExpressionParsing() {
    assertEquals(14, Eval().eval(Parser().expression(Lexer.lex("1 + 3*2*2 + 1")), MutEnv()).intVal())
    assertEquals(true, Eval().eval(Parser().expression(Lexer.lex("1 + 3*2*2 + 1 < 15")), MutEnv()).boolVal())
    assertEquals(17, Eval().eval(Parser().expression(Lexer.lex("(1 + 3)*2*2 + 1")), MutEnv()).intVal())
    assertEquals(-15, Eval().eval(Parser().expression(Lexer.lex("-(1 + 3)*2*2 + 1")), MutEnv()).intVal())
    assertEquals(-17, Eval().eval(Parser().expression(Lexer.lex("-((1 + 3)*2*2 + 1)")), MutEnv()).intVal())
  }

  @Test
  fun listTest(){
    val program = "val a := [10, 232, 31] val out := a[1]"
    val parser = Parser()
    val env = MutEnv()
    Eval().eval(parser.parse(program)!!, env)
    assertEquals(232, env.getValue("out").intVal())
  }

  @Test
  fun listAssignTest(){
    val program = "val a := [] a[1]:= 232 val out := a[1]"
    val parser = Parser()
    val env = MutEnv()
    Eval().eval(parser.parse(program)!!, env)
    assertEquals(232, env.getValue("out").intVal())
  }

  @Test
  fun sumListFromIput(){
    val program = TestUtil.loadProgram("SumListInput")
    val env = MutEnv()
    val inputReader = BufferedReader(StringReader("1\n12\n123\n-1\n"))
    Eval(inputReader).eval(Parser().parse(program)!!, env)
    assertEquals(136, env.getValue("out").intVal())
  }
}