package tatskaari

import org.testng.annotations.Test
import tatskaari.parsing.*
import tatskaari.GustoType.*
import tatskaari.parsing.typechecking.*
import kotlin.test.assertEquals

object TypeCheckTest {

  @Test
  fun testIfStatementType(){
    val ast = Parser().parse("do val a : integer := 1 if a = 1 then return true else return false end end")
    val codeblock = TypeChecker().checkStatementListTypes(ast!!, TypeEnv())[0]
    if (codeblock is TypedStatement.CodeBlock){
      assertEquals(PrimitiveType.Boolean, codeblock.returnType)
    }
  }


  @Test
  fun testBiggestList(){
    val ast = Parser().parse(TestUtil.loadProgram("BiggestList"))
    val typeChecker = TypeChecker()
    typeChecker.checkStatementListTypes(ast!!, TypeEnv())
    assertEquals( 0, typeChecker.typeMismatches.size)
  }

  @Test
  fun testApply(){
    val parser = Parser()
    val ast = parser.parse(TestUtil.loadProgram("Apply"))
    val typeChecker = TypeChecker()
    typeChecker.checkStatementListTypes(ast!!, TypeEnv())
    assertEquals( 0, typeChecker.typeMismatches.size)
  }

  @Test
  fun testListTypeChecking(){
    val parser = Parser()
    val typeChecker = TypeChecker()

    typeChecker.checkStatementListTypes(parser.parse("val l : integer list := [1,2,3,4]")!!, TypeEnv())
    assertEquals(0, typeChecker.typeMismatches.size)

    typeChecker.checkStatementListTypes(parser.parse("val l : integer list := [1,2, true, 4]")!!, TypeEnv())
    assertEquals(1, typeChecker.typeMismatches.size)
  }

  @Test
  fun testUnaryOperatorChecking(){
    val parser = Parser()
    val ast = parser.parse("val b : boolean := !(1=1)")!!


    var typeChecker = TypeChecker()
    typeChecker.checkStatementListTypes(ast, TypeEnv())
    assertEquals(0, typeChecker.typeMismatches.size)

    typeChecker = TypeChecker()
    typeChecker.checkStatementListTypes(parser.parse("val b : boolean := !1")!!, TypeEnv())
    assertEquals(1, typeChecker.typeMismatches.size)

    typeChecker = TypeChecker()
    typeChecker.checkStatementListTypes(parser.parse("val b : boolean := -true")!!, TypeEnv())
    assertEquals(1, typeChecker.typeMismatches.size)
  }

  @Test
  fun badReturnType(){
    val parser = Parser()
    val typeChecker = TypeChecker()
    val ast = parser.parse("function add(a: integer, b: integer) : integer do return true end")!!

    typeChecker.checkStatementListTypes(ast, TypeEnv())
    assertEquals(1, typeChecker.typeMismatches.size)
  }

  @Test
  fun undeclaredIdentifier() {
    val parser = Parser()
    val typeChecker = TypeChecker()
    val ast = parser.parse("val a: integer := b")!!

    typeChecker.checkStatementListTypes(ast, TypeEnv())
    assertEquals(1, typeChecker.typeMismatches.size)
  }

  @Test
  fun testInput(){
    val parser = Parser()
    val typeChecker = TypeChecker()
    val ast = parser.parse("input b val a: text := b")!!

    typeChecker.checkStatementListTypes(ast, TypeEnv())
    assertEquals(0, typeChecker.typeMismatches.size)

    typeChecker.checkStatementListTypes(parser.parse("input b val a: integer := b")!!, TypeEnv())
    assertEquals(1, typeChecker.typeMismatches.size)
  }

  @Test
  fun testUnitFunction(){
    val parser = Parser()
    val typeChecker = TypeChecker()
    val ast = parser.parse("function print() do output \"asdf\" end print()")
    typeChecker.checkStatementListTypes(ast!!, TypeEnv())
    assertEquals(0, typeChecker.typeMismatches.size)
  }

  @Test
  fun testAnonymousFunctionAssignmentInference() {
    val parser = Parser()
    val typeChecker = TypeChecker()
    val ast = parser.parse("val a := function(a: integer, b: integer) : integer do return 10 end")
    val env = TypeEnv()
    typeChecker.checkStatementListTypes(ast!!, env)

    assertEquals(FunctionType(listOf(PrimitiveType.Integer, PrimitiveType.Integer), PrimitiveType.Integer), env.getValue("a"))

  }

  @Test
  fun testAnonymousFunctionAssignmentInferenceBad() {
    val parser = Parser()
    val typeChecker = TypeChecker()
    val ast = parser.parse("val a := function(p1: integer, p2: integer) : integer do return 1.0 end")
    val env = TypeEnv()
    typeChecker.checkStatementListTypes(ast!!, env)

    assertEquals(FunctionType(listOf(PrimitiveType.Integer, PrimitiveType.Integer), PrimitiveType.Integer), env.getValue("a"))
    assertEquals(1, typeChecker.typeMismatches.size)
  }

  @Test
  fun testTypeInference(){
    val parser = Parser()
    val typeChecker = TypeChecker()
    val ast = parser.parse("val a := 1 a := 10.0")
    val env = TypeEnv()
    typeChecker.checkStatementListTypes(ast!!, env)
    assertEquals(PrimitiveType.Integer, env.getValue("a"))
    assertEquals(1, typeChecker.typeMismatches.size)
  }

  @Test
  fun testGenericParamsCantBeTreatedAsInts(){
    val parser = Parser()
    val typeChecker = TypeChecker()
    val ast = parser.parse("function outputLine(a: (a) -> b) do output a+1 end")
    val env = TypeEnv()
    typeChecker.checkStatementListTypes(ast!!, env)
    assertEquals(1, typeChecker.typeMismatches.size)
  }

  @Test
  fun testGenericAndThen(){
    val parser = Parser()
    val typeChecker = TypeChecker()
    val ast = parser.parse("""
function andThen(first : (a) -> b, second: (b) -> c) : (a) -> c do
    return function(value: a): c do
        return second(first(value))
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
    val env = TypeEnv()
    typeChecker.checkStatementListTypes(ast!!, env)
    assertEquals(0, typeChecker.typeMismatches.size)
  }

  @Test
  fun testGenericReturn(){
    val parser = Parser()
    val typeChecker = TypeChecker()
    val ast = parser.parse("""
function justReturn(value : a) : a do
  return value
end

val out: integer := justReturn(10)
""")
    val env = TypeEnv()
    typeChecker.checkStatementListTypes(ast!!, env)
    assertEquals(0, typeChecker.typeMismatches.size)
  }

  @Test
  fun testReturnTypeCheckerFalseFlags(){
    val checker = ReturnTypeChecker(Errors())
    val parser = Parser()
    val ast = parser.parse("""
function returnSomething(value : integer) : integer do
  if value < 10 then
    return 10
  else
    return value
  end
end
    """)
    val typeChecker = TypeChecker()
    val env = TypeEnv()
    checker.codeblock((typeChecker.checkStatementListTypes(ast!!, env).first() as TypedStatement.FunctionDeclaration).body, true)

    assertEquals(0, checker.typeErrors.size)
  }

  @Test
  fun testReturnTypeCheckerIfStatementMissingReturn(){
    val checker = ReturnTypeChecker(Errors())
    val parser = Parser()
    val ast = parser.parse("""
function returnSomething(value : integer) : integer do
  if value < 10 then
    return 10
  end
end
  """)
    val typeChecker = TypeChecker()
    val env = TypeEnv()
    checker.codeblock((typeChecker.checkStatementListTypes(ast!!, env).first() as TypedStatement.FunctionDeclaration).body, true)
    assertEquals(1, checker.typeErrors.size)

  }

  @Test
  fun testIfElseReturn(){
    val checker = ReturnTypeChecker(Errors())
    val parser = Parser()
    val ast = parser.parse("""
function doIf(a: boolean, doer: () -> integer) : integer do
    if a then
        return 10
    else
        output 5
    end
    return 1
end
  """)
    val typeChecker = TypeChecker()
    val env = TypeEnv()
    checker.codeblock((typeChecker.checkStatementListTypes(ast!!, env).first() as TypedStatement.FunctionDeclaration).body, true)
    assertEquals(0, checker.typeErrors.size)
  }

  @Test
  fun testUnitFunctionReturnCheck(){
    val checker = ReturnTypeChecker(Errors())
    val parser = Parser()
    val ast = parser.parse("""
function doIf(a: boolean, doer: () -> integer) : integer do

end
  """)
    val typeChecker = TypeChecker()
    val env = TypeEnv()
    checker.codeblock((typeChecker.checkStatementListTypes(ast!!, env).first() as TypedStatement.FunctionDeclaration).body, true)
    assertEquals(0, checker.typeErrors.size)
  }

  @Test
  fun testEmptyListDeclaration(){
    val program = "val inputList : integer list := []"
    val parser = Parser()
    val typeChecker = TypeChecker()
    val ast = parser.parse(program)
    typeChecker.checkStatementListTypes(ast!!, TypeEnv())
    assertEquals(0, typeChecker.typeMismatches.size)
  }

}