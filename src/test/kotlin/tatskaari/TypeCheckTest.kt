package tatskaari

import org.testng.annotations.Test
import tatskaari.parsing.*
import tatskaari.parsing.TypeChecker
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

object TypeCheckTest {
//  }

  //  @Test
//  fun testExpressionTypeCheck(){
//    var expression: Expression = Expression.BinaryOperator(
//      BinaryOperators.Add,
//      Expression.NumLiteral(12.0),
//      Expression.IntLiteral(12)
//    )
//
//    assertEquals(PrimitiveType.Number, TypeChecker().getExpressionType(expression, HashMap()))
//
//    expression = Expression.BinaryOperator(
//      BinaryOperators.Add,
//      Expression.NumLiteral(12.0),
//      Expression.TextLiteral("test")
//    )
//
//    assertEquals(PrimitiveType.Text, TypeChecker().getExpressionType(expression, HashMap()))
//
//    expression = Expression.BinaryOperator(
//      BinaryOperators.Add,
//      Expression.IntLiteral(12),
//      Expression.IntLiteral(12)
//    )
//
//    assertEquals(PrimitiveType.Integer, TypeChecker().getExpressionType(expression, HashMap()))
//
//    expression = Expression.UnaryOperator(
//      UnaryOperators.Not,
//      Expression.BinaryOperator(BinaryOperators.LessThanEq, Expression.IntLiteral(12), Expression.IntLiteral(12))
//    )
//
//    assertEquals(PrimitiveType.Boolean, TypeChecker().getExpressionType(expression, HashMap()))
//  @Test
//  fun testBadExpressions(){
//    assertFailsWith<TypeChecker.TypeMismatch> {
//      TypeChecker().getExpressionType(
//        Expression.BinaryOperator(
//          BinaryOperators.LessThan,
//          Expression.TextLiteral("asf"),
//          Expression.TextLiteral("asf")
//        ),
//        HashMap()
//      )
//    }
//
//    assertFailsWith<TypeChecker.TypeMismatch> {
//      TypeChecker().getExpressionType(
//        Expression.UnaryOperator(
//          UnaryOperators.Not,
//          Expression.TextLiteral("asf")
//        ),
//        HashMap()
//      )
//    }
//
//    assertFailsWith<TypeChecker.TypeMismatch> {
//      TypeChecker().getExpressionType(
//        Expression.UnaryOperator(
//          UnaryOperators.Not,
//          Expression.TextLiteral("asf")
//        ),
//        HashMap()
//      )
//    }
//  }

  @Test
  fun testIfStatementType(){
    val ast = Parser().parse("val a : integer := 1 if a = 1 then return true else return false end")
    val type = TypeChecker().checkStatementListTypes(ast!!, HashMap())
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
}