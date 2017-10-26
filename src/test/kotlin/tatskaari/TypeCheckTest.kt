package tatskaari

import org.testng.annotations.Test
import tatskaari.parsing.*
import tatskaari.parsing.TypeChecking.TypeChecker
import tatskaari.parsing.TypeChecking.TypeCheckerExpressionVisitor
import tatskaari.tokenising.Lexer
import kotlin.test.assertEquals

object TypeCheckTest {

  @Test
  fun testIfStatementType(){
    val ast = Parser().parse("val a : integer := 1 if a = 1 then return true else return false end")
    val (_, type) = TypeChecker().checkStatementListTypes(ast!!, HashMap())
    assertEquals(PrimitiveType.Boolean, type)
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

}