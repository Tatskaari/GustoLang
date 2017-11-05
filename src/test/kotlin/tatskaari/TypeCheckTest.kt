package tatskaari

import org.testng.annotations.Test
import tatskaari.parsing.*
import tatskaari.parsing.TypeChecking.Env
import tatskaari.parsing.TypeChecking.TypeChecker
import tatskaari.parsing.TypeChecking.TypedStatement
import tatskaari.GustoType.*
import kotlin.test.assertEquals

object TypeCheckTest {

  @Test
  fun testIfStatementType(){
    val ast = Parser().parse("do val a : integer := 1 if a = 1 then return true else return false end end")
    val codeblock = TypeChecker().checkStatementListTypes(ast!!, HashMap())[0]
    if (codeblock is TypedStatement.CodeBlock){
      assertEquals(PrimitiveType.Boolean, codeblock.returnType)
    }
  }


  @Test
  fun testBiggestList(){
    val ast = Parser().parse(TestUtil.loadProgram("BiggestList"))
    val typeChecker = TypeChecker()
    typeChecker.checkStatementListTypes(ast!!, HashMap())
    assertEquals( 0, typeChecker.typeMismatches.size)
  }

  @Test
  fun testApply(){
    val parser = Parser()
    val ast = parser.parse(TestUtil.loadProgram("Apply"))
    val typeChecker = TypeChecker()
    typeChecker.checkStatementListTypes(ast!!, HashMap())
    assertEquals( 0, typeChecker.typeMismatches.size)
  }

  @Test
  fun testListTypeChecking(){
    val parser = Parser()
    val typeChecker = TypeChecker()

    typeChecker.checkStatementListTypes(parser.parse("val l : integer list := [1,2,3,4]")!!, HashMap())
    assertEquals(0, typeChecker.typeMismatches.size)

    typeChecker.checkStatementListTypes(parser.parse("val l : integer list := [1,2, true, 4]")!!, HashMap())
    assertEquals(1, typeChecker.typeMismatches.size)
  }

  @Test
  fun testUnaryOperatorChecking(){
    val parser = Parser()
    val ast = parser.parse("val b : boolean := !(1=1)")!!


    var typeChecker = TypeChecker()
    typeChecker.checkStatementListTypes(ast, HashMap())
    assertEquals(0, typeChecker.typeMismatches.size)

    typeChecker = TypeChecker()
    typeChecker.checkStatementListTypes(parser.parse("val b : boolean := !1")!!, HashMap())
    assertEquals(1, typeChecker.typeMismatches.size)

    typeChecker = TypeChecker()
    typeChecker.checkStatementListTypes(parser.parse("val b : boolean := -true")!!, HashMap())
    assertEquals(2, typeChecker.typeMismatches.size)
  }

  @Test
  fun badReturnType(){
    val parser = Parser()
    val typeChecker = TypeChecker()
    val ast = parser.parse("function add(a: integer, b: integer) : integer do return true end")!!

    typeChecker.checkStatementListTypes(ast, HashMap())
    assertEquals(1, typeChecker.typeMismatches.size)
  }

  @Test
  fun undeclaredIdentifier() {
    val parser = Parser()
    val typeChecker = TypeChecker()
    val ast = parser.parse("val a: integer := b")!!

    typeChecker.checkStatementListTypes(ast, HashMap())
    assertEquals(1, typeChecker.typeMismatches.size)
  }

  @Test
  fun testInput(){
    val parser = Parser()
    val typeChecker = TypeChecker()
    val ast = parser.parse("input b val a: text := b")!!

    typeChecker.checkStatementListTypes(ast, HashMap())
    assertEquals(0, typeChecker.typeMismatches.size)

    typeChecker.checkStatementListTypes(parser.parse("input b val a: integer := b")!!, HashMap())
    assertEquals(1, typeChecker.typeMismatches.size)
  }

  @Test
  fun testUnitFunction(){
    val parser = Parser()
    val typeChecker = TypeChecker()
    val ast = parser.parse("function print() do output \"asdf\" end print()")
    typeChecker.checkStatementListTypes(ast!!, HashMap())
    assertEquals(0, typeChecker.typeMismatches.size)
  }

  @Test
  fun testAnonymousFunctionAssignmentInference() {
    val parser = Parser()
    val typeChecker = TypeChecker()
    val ast = parser.parse("val a := function(a: integer, b: integer) : integer do return 10 end")
    val env = Env()
    typeChecker.checkStatementListTypes(ast!!, env)

    assertEquals(FunctionType(listOf(PrimitiveType.Integer, PrimitiveType.Integer), PrimitiveType.Integer), env.getValue("a"))

  }

  @Test
  fun testAnonymousFunctionAssignmentInferenceBad() {
    val parser = Parser()
    val typeChecker = TypeChecker()
    val ast = parser.parse("val a := function(p1: integer, p2: integer) : integer do return 1.0 end")
    val env = Env()
    typeChecker.checkStatementListTypes(ast!!, env)

    assertEquals(FunctionType(listOf(PrimitiveType.Integer, PrimitiveType.Integer), PrimitiveType.Integer), env.getValue("a"))
    assertEquals(1, typeChecker.typeMismatches.size)

  }

  @Test
  fun testTypeInference(){
    val parser = Parser()
    val typeChecker = TypeChecker()
    val ast = parser.parse("val a := 1 a := 10.0")
    val env = Env()
    typeChecker.checkStatementListTypes(ast!!, env)
    assertEquals(PrimitiveType.Integer, env.getValue("a"))
    assertEquals(1, typeChecker.typeMismatches.size)
  }

  @Test
  fun testGenericParamsCantBeTreatedAsInts(){
    val parser = Parser()
    val typeChecker = TypeChecker()
    val ast = parser.parse("function outputLine(a: (A) -> B) do output a+1 end")
    val env = Env()
    typeChecker.checkStatementListTypes(ast!!, env)
    assertEquals(1, typeChecker.typeMismatches.size)
  }

  @Test
  fun testGenericAndThen(){
    val parser = Parser()
    val typeChecker = TypeChecker()
    val ast = parser.parse("""
function andThen(first : (A) -> B, second: (B) -> C) : (A) -> C do
    return function(value: A): C do
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
    val env = Env()
    typeChecker.checkStatementListTypes(ast!!, env)
    assertEquals(0, typeChecker.typeMismatches.size)
  }

  @Test
  fun testGenericReturn(){
    val parser = Parser()
    val typeChecker = TypeChecker()
    val ast = parser.parse("""
function justReturn(value : A) : A do
  return value
end

val out: integer := justReturn(10)
""")
    val env = Env()
    typeChecker.checkStatementListTypes(ast!!, env)
    assertEquals(0, typeChecker.typeMismatches.size)
  }
}