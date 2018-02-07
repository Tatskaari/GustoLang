package tatskaari

import org.testng.annotations.Test
import tatskaari.eval.*
import tatskaari.eval.values.Value
import tatskaari.parsing.Parser
import tatskaari.parsing.typechecking.TypeChecker
import tatskaari.tokenising.Lexer
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

object EvalTest {
  @Test
  fun testBasicEval() {
    val program = TestUtil.loadProgram("TestMain")
    val env = EvalEnv()
    val parser = Parser()
    val AST = parser.parse(program)!!
    Eval(StdinInputProvider, SystemOutputProvider).eval(AST, env)

    assertEquals(1, env.getValue("b").intVal())
  }

  @Test
  fun numValInCondition() {
    assertFailsWith<Eval.TypeMismatch> {
      val program = "if (1) then end"
      val env = EvalEnv()
      Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().parse(program)!!, env)
    }
  }

  @Test
  fun missingIdentifier() {
    assertFailsWith<Eval.UndefinedIdentifier> {
      val program = "if 1 = a then end"
      val env = EvalEnv()
      Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().parse(program)!!, env)
    }
  }

  @Test
  fun numIdentifierAssignBoolean() {
    assertFailsWith<Eval.TypeMismatch> {
      val program = "val a: integer := 1 a := 1 = 1"
      val env = EvalEnv()
      Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().parse(program)!!, env)
    }
  }

  @Test
  fun testAddBoolRHS() {
    assertFailsWith<Eval.TypeMismatch> {
      val program = "val a: integer := (1 = 1) + 1"
      val env = EvalEnv()
      Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().parse(program)!!, env)
    }
  }

  @Test
  fun testAddBoolLHS() {
    assertFailsWith<Eval.TypeMismatch> {
      val program = "val a: integer := 1 + (1 = 1) "
      val env = EvalEnv()
      Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().parse(program)!!, env)
    }
  }

  @Test
  fun testAdd() {
    val program = "val a: integer := 1 + 1"
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().parse(program)!!, env)

    assertEquals(2, env.getValue("a").intVal())

  }

  @Test
  fun testSub() {
    val program = "val a: integer := 1 - 1"
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().parse(program)!!, env)

    assertEquals(0, env.getValue("a").intVal())
  }

  @Test
  fun testNullInput() {
    assertFailsWith<Eval.InvalidUserInput> {
      val env = EvalEnv()
      Eval(StringInputProvider("\n"), SystemOutputProvider).eval(Parser().parse("input a")!!, env)
    }

    assertFailsWith<Eval.InvalidUserInput> {
      val env2 = EvalEnv()
      Eval(StringInputProvider(""), SystemOutputProvider).eval(Parser().parse("input a")!!, env2)
    }
  }

  @Test
  fun outputTest() {
    val output = StringOutputProvider()
    Eval(StdinInputProvider, output).eval(Parser().parse("output 1")!!, EvalEnv())

    assertEquals('1', output.outputString[0])

  }

  @Test
  fun ifElseTest() {
    val program = TestUtil.loadProgram("IfElse")

    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().parse(program)!!, env)

    assertEquals(1, env.getValue("someVar").intVal())

  }

  @Test
  fun testBoolTrue() {
    val program = Parser().parse("val a: integer := true")!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program, env)
    assertEquals(true, env.getValue("a").boolVal())
  }

  @Test
  fun testBoolFalse() {
    val program = Parser().parse("val a: integer := false")!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program, env)
    assertEquals(false, env.getValue("a").boolVal())
  }

  @Test
  fun testNot() {
    val program = Parser().parse("val a: boolean := !false")!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program, env)
    assertEquals(true, env.getValue("a").boolVal())
  }

  @Test
  fun testIfLiteral() {
    val program = Parser().parse("val a: integer := 0 if (true) then a := 1 else a := 2 end")!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program, env)
    assertEquals(1, env.getValue("a").intVal())

    val program2 = Parser().parse("val a: integer := 0 if (false) then a := 1 else a := 2 end")!!
    val env2 = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program2, env2)
    assertEquals(2, env2.getValue("a").intVal())
  }

  @Test
  fun testWhile() {
    val program = Parser().parse(TestUtil.loadProgram("While"))!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program, env)
    assertEquals(20, env.getValue("b").intVal())
  }

  @Test
  fun whileWithNonBoolResult() {
    val program = Parser().parse("while (1) do end")!!
    assertFailsWith<Eval.TypeMismatch> {
      Eval(StdinInputProvider, SystemOutputProvider).eval(program, EvalEnv())
    }
  }

  @Test
  fun fibTest() {
    val program = Parser().parse(TestUtil.loadProgram("Fib"))!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program, env)
    assertEquals(13, env.getValue("c").intVal())
  }

  @Test
  fun redeclareEval() {
    val program = Parser().parse("val a: integer := 1 val a: integer := 2")!!
    assertFailsWith<Eval.VariableAlreadyDefined> {
      Eval(StdinInputProvider, SystemOutputProvider).eval(program, EvalEnv())
    }
  }

  @Test
  fun testAnd() {
    val program = Parser().parse("val a: integer := 0 if(true and true) then a := 1 else a := 2 end")!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(1, env.getValue("a").intVal())
  }

  @Test
  fun testOr() {
    val program = Parser().parse("val a : integer := 0 if(true or false) then a := 1 else a := 2 end")!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(1, env.getValue("a").intVal())
  }

  @Test
  fun testNotAnd() {
    val program = Parser().parse("val a: integer := 0 if(true and false) then a := 1 else a := 2 end")!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(2, env.getValue("a").intVal())
  }

  @Test
  fun testNotOr() {
    val program = Parser().parse("val a: integer := 0 if(false or false) then a := 1 else a := 2 end")!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(2, env.getValue("a").intVal())
  }

  @Test
  fun testLT() {
    val program = Parser().parse("val a: integer := 0 if(1 < 2) then a := 1 else a := 2 end")!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(1, env.getValue("a").intVal())
  }

  @Test
  fun testGT() {
    val program = Parser().parse("val a: integer := 0 if 2 > 1 then a := 1 else a := 2 end")!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(1, env.getValue("a").intVal())
  }

  @Test
  fun testGTE() {
    val program = Parser().parse("val a: integer := 0 if(1 >= 1) then a := 1 else a := 2 end")!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(1, env.getValue("a").intVal())
  }

  @Test
  fun testLTE() {
    val program = Parser().parse("val a: integer := 0 if(1 <= 1) then a := 1 else a := 2 end")!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(1, env.getValue("a").intVal())
  }


  @Test
  fun testNotLT() {
    val program = Parser().parse("val a: integer := 0 if(2 < 1) then a := 1 else a := 2 end")!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(2, env.getValue("a").intVal())
  }

  @Test
  fun testNotGT() {
    val program = Parser().parse("val a: integer := 0 if(1 > 2) then a := 1 else a := 2 end")!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(2, env.getValue("a").intVal())
  }

  @Test
  fun testNotGTE() {
    val program = Parser().parse("val a: integer := 0 if(1 >= 2) then a := 1 else a := 2 end")!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(2, env.getValue("a").intVal())
  }

  @Test
  fun testNotLTE() {
    val program = Parser().parse("val a: integer := 0 if(2 <= 1) then a := 1 else a := 2 end")!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(2, env.getValue("a").intVal())
  }

  @Test
  fun testMul() {
    val program = Parser().parse("val a: integer := 2 * 3")!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(6, env.getValue("a").intVal())
  }

  @Test
  fun testDiv() {
    val program = Parser().parse("val a: integer := 9 / 3")!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(3.0, env.getValue("a").doubleVal())
  }

  @Test
  fun testFunctionCall() {
    val program = Parser().parse(TestUtil.loadProgram("Function"))!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program, env)
    assertEquals(3, env.getValue("c").intVal())
  }

  @Test
  fun missingFuncDef(){
    val program = Parser().parse("val a: integer := add(1, 2)")!!
    assertFailsWith<Eval.UndefinedIdentifier> {
      Eval(StdinInputProvider, SystemOutputProvider).eval(program, EvalEnv())
    }
  }


  @Test
  fun callingInt(){
    val program = Parser().parse(" val a: integer := 2  val b: integer := a()")!!
    assertFailsWith<Eval.TypeMismatch> {
      Eval(StdinInputProvider, SystemOutputProvider).eval(program, EvalEnv())
    }
  }

  @Test
  fun wrongNumParams(){
    val program = Parser().parse("function add(a: integer, b: integer) do return a + b end val b : integer := add(1)")!!
    assertFailsWith<Eval.TypeMismatch> {
      Eval(StdinInputProvider, SystemOutputProvider).eval(program, EvalEnv())
    }
  }

  @Test
  fun paramDoesntExist(){
    val program = Parser().parse("function add( a: integer,  b: integer) do return a + b end val b: integer := add(1, 2, 3)")!!
    assertFailsWith<Eval.TypeMismatch> {
      Eval(StdinInputProvider, SystemOutputProvider).eval(program, EvalEnv())
    }
  }

  @Test
  fun evalFuncFib(){
    val program = Parser().parse(TestUtil.loadProgram("FuncFib"))!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program, env)
    assertEquals(13, env.getValue("out").intVal())
  }

  @Test
  fun whileReturn(){
    val program = Parser().parse(TestUtil.loadProgram("WhileReturn"))!!
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program, env)
    assertEquals(100, env.getValue("out").intVal())
  }

  @Test
  fun testUnaryOperatorTypeMissmatch(){
    assertFailsWith<Eval.TypeMismatch> {
      Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().parse("val a: integer := !1")!!, EvalEnv())
    }
    assertFailsWith<Eval.TypeMismatch> {
      Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().parse("val a: integer := -true")!!, EvalEnv())
    }
  }

  @Test
  fun testExpressionParsing() {
    assertEquals(14, Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().expression(Lexer.lex("1 + 3*2*2 + 1")), EvalEnv()).intVal())
    assertEquals(true, Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().expression(Lexer.lex("1 + 3*2*2 + 1 < 15")), EvalEnv()).boolVal())
    assertEquals(17, Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().expression(Lexer.lex("(1 + 3)*2*2 + 1")), EvalEnv()).intVal())
    assertEquals(-15, Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().expression(Lexer.lex("-(1 + 3)*2*2 + 1")), EvalEnv()).intVal())
    assertEquals(-17, Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().expression(Lexer.lex("-((1 + 3)*2*2 + 1)")), EvalEnv()).intVal())
  }

  @Test
  fun listTest(){
    val program = "val a: integer list := [10, 232, 31] val out: integer := a[1]"
    val parser = Parser()
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(parser.parse(program)!!, env)
    assertEquals(232, env.getValue("out").intVal())
  }

  @Test
  fun listAssignTest(){
    val program = "val a: integer list := [] a[1]:= 232 val out: integer := a[1]"
    val parser = Parser()
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(parser.parse(program)!!, env)
    assertEquals(232, env.getValue("out").intVal())
  }

  @Test
  fun testText(){
    val program = "val a: integer := \"asdf\""
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(Parser().parse(program)!!, env)
    assertEquals("asdf", env.getValue("a").textVal())
  }

  @Test
  fun testReduce(){
    val program = TestUtil.loadProgram("TestReducer")
    val env = EvalEnv()
    val parser = Parser()
    val ast = parser.parse(program)
    Eval(StdinInputProvider, SystemOutputProvider).eval(ast!!, env)

    assertEquals(10, env.getValue("addRes").intVal())
    assertEquals(24, env.getValue("mulRes").intVal())
  }

  @Test
  fun testStringConcat(){
    val program = """val out: text := 1 + "test" + 1 + true + 1.1 val outt: text := true + "test" """
    val env = EvalEnv()
    val parser = Parser()
    val ast = parser.parse(program)
    Eval(StdinInputProvider, SystemOutputProvider).eval(ast!!, env)

    assertEquals("1test1true1.1", env.getValue("out").textVal())
    assertEquals("truetest", env.getValue("outt").textVal())
  }

  @Test
  fun testMulDiv(){
    val program = "val out: integer := (1.0*1)*1.0/1 + 10 * 2.1 val outt: integer := 3.0/1.5 val outtt: integer := 1/2.0"
    val env = EvalEnv()
    val parser = Parser()
    val ast = parser.parse(program)
    Eval(StdinInputProvider, SystemOutputProvider).eval(ast!!, env)
    assertEquals(22.0, env.getValue("out").doubleVal())
    assertEquals(2.0, env.getValue("outt").doubleVal())
    assertEquals(0.5, env.getValue("outtt").doubleVal())
  }

  @Test
  fun testAddSub(){
    val program = "val out: integer := 1.0 + 1 - 1.0 val outt: integer := 1-1.0 val outtt: integer := 0.5+1"
    val env = EvalEnv()
    val parser = Parser()
    val ast = parser.parse(program)
    Eval(StdinInputProvider, SystemOutputProvider).eval(ast!!, env)
    assertEquals(1.0, env.getValue("out").doubleVal())
    assertEquals(0.0, env.getValue("outt").doubleVal())
    assertEquals(1.5, env.getValue("outtt").doubleVal())
  }

  @Test
  fun testComparisons(){
    val program = "val out := 1.0 <= 1 val outt := \"1\" = \"1\" val outtt := 10 = 10.0 val outttt := 10.0 = 10"
    val env = EvalEnv()
    val parser = Parser()
    val ast = parser.parse(program)
    Eval(StdinInputProvider, SystemOutputProvider).eval(ast!!, env)
    assertEquals(true, env.getValue("out").boolVal())
    assertEquals(true, env.getValue("outt").boolVal())
    assertEquals(false, env.getValue("outtt").boolVal())
    assertEquals(false, env.getValue("outttt").boolVal())
  }

  @Test
  fun numberComparison(){
    val intNum: Number = 10
    val doubleNum: Number = 10.0
    val byteNum: Byte = 10

    assert(intNum <= doubleNum)
    assert(doubleNum <= intNum)
    assert(intNum <= intNum)
    assert(doubleNum <= doubleNum)
    assertFailsWith<Eval.TypeMismatch> { doubleNum <= byteNum }
    assertFailsWith<Eval.TypeMismatch> { intNum <= byteNum }
  }

  @Test
  fun testAnonymousFunctions() {
    val parser = Parser()
    val program = parser.parse("val add : (integer, integer) -> integer := function(a: integer, b: integer) : integer do return a + b end " +
      "val out : integer := add(10, 11)")
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program!!, env)
    assertEquals(21, env.getValue("out").intVal())
  }

  @Test
  fun testInput(){
    val parser = Parser()
    val program = parser.parse("input a")
    val env = EvalEnv()
    Eval(StringInputProvider("Hello, world!"), SystemOutputProvider).eval(program!!, env)
    assertEquals("Hello, world!", env.getValue("a").textVal())
  }

  @Test
  fun testUnitFunction(){
    val parser = Parser()
    val program = parser.parse("function doNothing() do end " +
      "val a: unit := doNothing()")
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program!!, env)
    assertEquals(Unit, env.getValue("a").value)
  }

  @Test
  fun testBIF(){
    val parser = Parser()
    val program = parser.parse("val a : integer list := [1,2,3] val out : integer := size(a)")
    val typeChecker = TypeChecker()
    typeChecker.checkStatementListTypes(program!!, TypeChecker.envOf("size" to BuiltInFunction.SizeOfList.type))
    val env = EvalEnv(hashMapOf(Pair("size", Value.BifVal(BuiltInFunction.SizeOfList))), HashMap())
    Eval(StdinInputProvider, SystemOutputProvider).eval(program,env)
    assertEquals(3, env.getValue("out").intVal())
    assertEquals(0, typeChecker.typeMismatches.size)
  }

  @Test
  fun testBIFBadArg(){
    val parser = Parser()
    val program = parser.parse(" val out : integer := size(12)")
    val typeChecker = TypeChecker()
    typeChecker.checkStatementListTypes(program!!, TypeChecker.envOf(Pair("size", BuiltInFunction.SizeOfList.type)))
    assertEquals(1, typeChecker.typeMismatches.size)
  }

  @Test
  fun testAnyListType(){
    val parser = Parser()
    val program = parser.parse(" val out : list := [1,2] out := [true, false]")
    val typeChecker = TypeChecker()
    typeChecker.checkStatementListTypes(program!!, TypeChecker.envOf(Pair("size", BuiltInFunction.SizeOfList.type)))
    assertEquals(0, typeChecker.typeMismatches.size)
  }

  @Test
  fun testCastingText(){
    val parser = Parser()
    val program = parser.parse("""val int : integer := toInteger("123") val num : number := toNumber("123.123") val bool : boolean := toBoolean("true")""")

    val typeChecker = TypeChecker()
    val typeEnv = BuiltInFunction.getTypeEnv()
    typeChecker.checkStatementListTypes(program!!, typeEnv)
    assertEquals(0, typeChecker.typeMismatches.size)

    val evalEnv = BuiltInFunction.getEvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program, evalEnv)

    assertEquals(123, evalEnv.getValue("int").intVal())
    assertEquals(123.123, evalEnv.getValue("num").numVal())
    assertEquals(true, evalEnv.getValue("bool").boolVal())
  }

  @Test
  fun testRandom(){
    val parser = Parser()
    val program = parser.parse("val a: number := 0.0 a := random() ")
    val typeChecker = TypeChecker()
    val typeEnv = BuiltInFunction.getTypeEnv()
    typeChecker.checkStatementListTypes(program!!, typeEnv)
    assertEquals(0, typeChecker.typeMismatches.size)

    val evalEnv = BuiltInFunction.getEvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(program, evalEnv)

    assertNotEquals(0.0, evalEnv.getValue("a").numVal())
  }

  @Test
  fun testStatementExpressions(){
    val parser = Parser()
    val ast = parser.parse("val out: integer := 0 function print() do out := 5 end print()")
    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(ast!!, env)

    assertEquals(5, env.getValue("out").intVal())

  }

  @Test
  fun testCompose() {
    val parser = Parser()
    val ast = parser.parse("""
function andThen(first: (integer) -> integer, second: (integer) -> integer): (integer) -> integer do
    return function(a: integer) : integer do
        return second(first(a))
    end
end

function double(a: integer) : integer do
    return a * 2
end

function square(a: integer) : integer do
    return a * a
end


val out := double.andThen(square)(10)
    """)

    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(ast!!, env)
    assertEquals(400, env.getValue("out").intVal())

  }

  @Test
  fun testComposeAssociation() {
    val parser = Parser()
    val ast = parser.parse("""
function andThen(first: (integer) -> integer, second: (integer) -> integer): (integer) -> integer do
    return function(a: integer) : integer do
        return second(first(a))
    end
end

function double(a: integer) : integer do
    return a * 2
end

function square(a: integer) : integer do
    return a * a
end

val doubleAndSquare := double.andThen(square).andThen(double)

val out := 10.doubleAndSquare()
    """)

    val env = EvalEnv()
    Eval(StdinInputProvider, SystemOutputProvider).eval(ast!!, env)
    assertEquals(800, env.getValue("out").intVal())

  }

  @Test
  fun testGenericMapFunction(){
    val parser = Parser()
    val ast = parser.parse("""
function map(oldList: a list, mapper: (a) -> b) : b list do
    val i := 0
    val newList : b list := []

    while i < size(oldList) do
        newList[i] := mapper(oldList[i])
        i := i + 1
    end

    return newList
end

val a : integer list := [1, 2, 3]

function convertToString(entry: integer): text do
    return "val: " + entry
end

val newList := a.map(convertToString)

val out: text := newList[1]
    """)
    val typeChecker = TypeChecker()

    val evalEnv = BuiltInFunction.getEvalEnv()
    val typeCheckerEnv = BuiltInFunction.getTypeEnv()
    typeChecker.checkStatementListTypes(ast!!, typeCheckerEnv)

    Eval(StdinInputProvider, SystemOutputProvider).eval(ast, evalEnv)
    assertEquals("val: 2", evalEnv.getValue("out").textVal())
    assertEquals(0, typeChecker.typeMismatches.size)

  }
}