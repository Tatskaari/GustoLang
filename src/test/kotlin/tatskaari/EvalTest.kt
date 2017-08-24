package tatskaari

import org.testng.annotations.Test
import tatskaari.eval.*
import tatskaari.parsing.Parser
import tatskaari.tokenising.Lexer
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

object EvalTest {
  @Test
  fun testBasicEval() {
    val program = TestUtil.loadProgram("TestMain")
    val env = MutEnv()
    val parser = Parser()
    val AST = parser.parse(program)!!
    Eval(StdinInputProvider, SystemOutputProvider).eval(AST, env)

    assertEquals(1, env.getValue("b").intVal())
  }

  @Test
  fun numValInCondition() {
    assertFailsWith<Eval.TypeMismatch> {
      val program = "if (1) do end"
      val env = MutEnv()
      Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().parse(program)!!, env)
    }
  }

  @Test
  fun missingIdentifier() {
    assertFailsWith<Eval.UndefinedIdentifier> {
      val program = "if 1 = a do end"
      val env = MutEnv()
      Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().parse(program)!!, env)
    }
  }

  @Test
  fun numIdentifierAssignBoolean() {
    assertFailsWith<Eval.TypeMismatch> {
      val program = "val a := 1 a := 1 = 1"
      val env = MutEnv()
      Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().parse(program)!!, env)
    }
  }

  @Test
  fun testAddBoolRHS() {
    assertFailsWith<Eval.TypeMismatch> {
      val program = "val a := (1 = 1) + 1"
      val env = MutEnv()
      Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().parse(program)!!, env)
    }
  }

  @Test
  fun testAddBoolLHS() {
    assertFailsWith<Eval.TypeMismatch> {
      val program = "val a := 1 + (1 = 1) "
      val env = MutEnv()
      Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().parse(program)!!, env)
    }
  }

  @Test
  fun testAdd() {
    val program = "val a := 1 + 1"
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().parse(program)!!, env)

    assertEquals(2, env.getValue("a").intVal())

  }

  @Test
  fun testSub() {
    val program = "val a := 1 - 1"
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().parse(program)!!, env)

    assertEquals(0, env.getValue("a").intVal())
  }

  @Test
  fun testNumInput() {
    val env = MutEnv()
    Eval(StringInputProvider("1234"), SystemOutputProvider).eval(Parser().parse("input a")!!, env)
    assertEquals(1234, env.getValue("a").intVal())
  }

  @Test
  fun testTrueInput() {
    val env = MutEnv()
    Eval(StringInputProvider("true"), SystemOutputProvider).eval(Parser().parse("input a")!!, env)
    assertEquals(true, env.getValue("a").boolVal())
  }

  @Test
  fun testFalseInput() {
    val env = MutEnv()
    Eval(StringInputProvider("false"), SystemOutputProvider).eval(Parser().parse("input a")!!, env)
    assertEquals(false, env.getValue("a").boolVal())
  }

  @Test
  fun testNullInput() {
    assertFailsWith<Eval.InvalidUserInput> {
      val env = MutEnv()
      Eval(StringInputProvider("\n"), SystemOutputProvider).eval(Parser().parse("input a")!!, env)
    }

    assertFailsWith<Eval.InvalidUserInput> {
      val env2 = MutEnv()
      Eval(StringInputProvider(""), SystemOutputProvider).eval(Parser().parse("input a")!!, env2)
    }
  }

  @Test
  fun outputTest() {
    val output = StringOutputProvider()
    Eval(StdinInputProvider, output).eval(Parser().parse("output 1")!!, MutEnv())

    assertEquals('1', output.outputString[0])

  }

  @Test
  fun ifElseTest() {
    val program = TestUtil.loadProgram("IfElse")

    var env = MutEnv()
    Eval(StringInputProvider("10"), SystemOutputProvider).eval(Parser().parse(program)!!, env)

    assertEquals(1, env.getValue("someVar").intVal())

    env = MutEnv()
    Eval(StringInputProvider("11"), SystemOutputProvider).eval(Parser().parse(program)!!, env)

    assertEquals(2, env.getValue("someVar").intVal())
  }

  @Test
  fun testBoolTrue() {
    val program = Parser().parse("val a := true")!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program, env)
    assertEquals(true, env.getValue("a").boolVal())
  }

  @Test
  fun testBoolFalse() {
    val program = Parser().parse("val a := false")!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program, env)
    assertEquals(false, env.getValue("a").boolVal())
  }

  @Test
  fun testNot() {
    val program = Parser().parse("val a := !false")!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program, env)
    assertEquals(true, env.getValue("a").boolVal())
  }

  @Test
  fun testIfLiteral() {
    val program = Parser().parse("val a := 0 if (true) do a := 1 else a := 2 end")!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program, env)
    assertEquals(1, env.getValue("a").intVal())

    val program2 = Parser().parse("val a := 0 if (false) do a := 1 else a := 2 end")!!
    val env2 = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program2, env2)
    assertEquals(2, env2.getValue("a").intVal())
  }

  @Test
  fun testWhile() {
    val program = Parser().parse(TestUtil.loadProgram("While"))!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program, env)
    assertEquals(20, env.getValue("b").intVal())
  }

  @Test
  fun whileWithNonBoolResult() {
    val program = Parser().parse("while (1) do end")!!
    assertFailsWith<Eval.TypeMismatch> {
      Eval(StdinInputProvider, SystemOutputProvider).eval(program, MutEnv())
    }
  }

  @Test
  fun fibTest() {
    val program = Parser().parse(TestUtil.loadProgram("Fib"))!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program, env)
    assertEquals(13, env.getValue("c").intVal())
  }

  @Test
  fun redeclareEval() {
    val program = Parser().parse("val a := 1 val a := 2")!!
    assertFailsWith<Eval.VariableAlreadyDefined> {
      Eval(StdinInputProvider, SystemOutputProvider).eval(program, MutEnv())
    }
  }

  @Test
  fun testAnd() {
    val program = Parser().parse("val a := 0 if(true and true) do a := 1 else a := 2 end")!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(1, env.getValue("a").intVal())
  }

  @Test
  fun testOr() {
    val program = Parser().parse("val a := 0 if(true or false) do a := 1 else a := 2 end")!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(1, env.getValue("a").intVal())
  }

  @Test
  fun testNotAnd() {
    val program = Parser().parse("val a := 0 if(true and false) do a := 1 else a := 2 end")!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(2, env.getValue("a").intVal())
  }

  @Test
  fun testNotOr() {
    val program = Parser().parse("val a := 0 if(false or false) do a := 1 else a := 2 end")!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(2, env.getValue("a").intVal())
  }

  @Test
  fun testLT() {
    val program = Parser().parse("val a := 0 if(1 < 2) do a := 1 else a := 2 end")!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(1, env.getValue("a").intVal())
  }

  @Test
  fun testGT() {
    val program = Parser().parse("val a := 0 if 2 > 1 do a := 1 else a := 2 end")!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(1, env.getValue("a").intVal())
  }

  @Test
  fun testGTE() {
    val program = Parser().parse("val a := 0 if(1 >= 1) do a := 1 else a := 2 end")!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(1, env.getValue("a").intVal())
  }

  @Test
  fun testLTE() {
    val program = Parser().parse("val a := 0 if(1 <= 1) do a := 1 else a := 2 end")!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(1, env.getValue("a").intVal())
  }


  @Test
  fun testNotLT() {
    val program = Parser().parse("val a := 0 if(2 < 1) do a := 1 else a := 2 end")!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(2, env.getValue("a").intVal())
  }

  @Test
  fun testNotGT() {
    val program = Parser().parse("val a := 0 if(1 > 2) do a := 1 else a := 2 end")!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(2, env.getValue("a").intVal())
  }

  @Test
  fun testNotGTE() {
    val program = Parser().parse("val a := 0 if(1 >= 2) do a := 1 else a := 2 end")!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(2, env.getValue("a").intVal())
  }

  @Test
  fun testNotLTE() {
    val program = Parser().parse("val a := 0 if(2 <= 1) do a := 1 else a := 2 end")!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(2, env.getValue("a").intVal())
  }

  @Test
  fun testMul() {
    val program = Parser().parse("val a := 2 * 3")!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(6, env.getValue("a").intVal())
  }

  @Test
  fun testDiv() {
    val program = Parser().parse("val a := 9 / 3")!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(3.0, env.getValue("a").numVal())
  }

  @Test
  fun testFunctionCall() {
    val program = Parser().parse(TestUtil.loadProgram("Function"))!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program, env)
    assertEquals(3, env.getValue("c").intVal())
  }

  @Test
  fun missingFuncDef(){
    val program = Parser().parse("val a := add(1, 2)")!!
    assertFailsWith<Eval.UndefinedIdentifier> {
      Eval(StdinInputProvider, SystemOutputProvider).eval(program, MutEnv())
    }
  }

  @Test
  fun missingReturnFromFunc(){
    val program = Parser().parse("function add(a, b) do end val c := add(1, 2)")!!
    assertFailsWith<Eval.FunctionExitedWithoutReturn> {
      Eval(StdinInputProvider, SystemOutputProvider).eval(program, MutEnv())
    }
  }

  @Test
  fun callingInt(){
    val program = Parser().parse("val a := 2 val b := a()")!!
    assertFailsWith<Eval.TypeMismatch> {
      Eval(StdinInputProvider, SystemOutputProvider).eval(program, MutEnv())
    }
  }

  @Test
  fun wrongNumParams(){
    val program = Parser().parse("function add(a, b) do return a + b end val b := add(1)")!!
    assertFailsWith<Eval.TypeMismatch> {
      Eval(StdinInputProvider, SystemOutputProvider).eval(program, MutEnv())
    }
  }

  @Test
  fun paramDoesntExist(){
    val program = Parser().parse("function add(a, b) do return a + b end val b := add(1, 2, 3)")!!
    assertFailsWith<Eval.TypeMismatch> {
      Eval(StdinInputProvider, SystemOutputProvider).eval(program, MutEnv())
    }
  }

  @Test
  fun evalFuncFib(){
    val program = Parser().parse(TestUtil.loadProgram("FuncFib"))!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program, env)
    assertEquals(13, env.getValue("out").intVal())
  }

  @Test
  fun whileReturn(){
    val program = Parser().parse(TestUtil.loadProgram("WhileReturn"))!!
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program, env)
    assertEquals(100, env.getValue("out").intVal())
  }

  @Test
  fun testUnaryOperatorTypeMissmatch(){
    assertFailsWith<Eval.TypeMismatch> {
      Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().parse("val a := !1")!!, MutEnv())
    }
    assertFailsWith<Eval.TypeMismatch> {
      Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().parse("val a := -true")!!, MutEnv())
    }
  }

  @Test
  fun testExpressionParsing() {
    assertEquals(14, Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().expression(Lexer.lex("1 + 3*2*2 + 1")), MutEnv()).intVal())
    assertEquals(true, Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().expression(Lexer.lex("1 + 3*2*2 + 1 < 15")), MutEnv()).boolVal())
    assertEquals(17, Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().expression(Lexer.lex("(1 + 3)*2*2 + 1")), MutEnv()).intVal())
    assertEquals(-15, Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().expression(Lexer.lex("-(1 + 3)*2*2 + 1")), MutEnv()).intVal())
    assertEquals(-17, Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().expression(Lexer.lex("-((1 + 3)*2*2 + 1)")), MutEnv()).intVal())
  }

  @Test
  fun listTest(){
    val program = "val a := [10, 232, 31] val out := a[1]"
    val parser = Parser()
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(parser.parse(program)!!, env)
    assertEquals(232, env.getValue("out").intVal())
  }

  @Test
  fun listAssignTest(){
    val program = "val a := [] a[1]:= 232 val out := a[1]"
    val parser = Parser()
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(parser.parse(program)!!, env)
    assertEquals(232, env.getValue("out").intVal())
  }

  @Test
  fun sumListFromIput(){
    val program = TestUtil.loadProgram("SumListInput")
    val env = MutEnv()
    Eval(StringInputProvider("1\n12\n123\n-1\n"), SystemOutputProvider).eval(Parser().parse(program)!!, env)
    assertEquals(136, env.getValue("out").intVal())
  }

  @Test
  fun testText(){
    val program = "val a := \"asdf\""
    val env = MutEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().parse(program)!!, env)
    assertEquals("asdf", env.getValue("a").textVal())
  }

  @Test
  fun testReduce(){
    val program = TestUtil.loadProgram("TestReducer")
    val env = MutEnv()
    val parser = Parser()
    val ast = parser.parse(program)
    Eval(StdinInputProvider, SystemOutputProvider).eval(ast!!, env)

    assertEquals(10, env.getValue("addRes").intVal())
    assertEquals(24, env.getValue("mulRes").intVal())
  }

}