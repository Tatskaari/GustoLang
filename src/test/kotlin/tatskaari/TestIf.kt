package tatskaari

import org.testng.annotations.Test
import tatskaari.parsing.BinaryOperators
import tatskaari.parsing.Expression
import tatskaari.parsing.Parser
import tatskaari.parsing.Statement
import tatskaari.tokenising.Token
import tatskaari.tokenising.TokenType
import kotlin.test.assertEquals

object TestIf {
  @Test
  fun testBasicIf() {
    val program = TestUtil.loadProgram("If")
    val expected = listOf(
      Statement.CodeBlock(
        listOf(
          Statement.If(
            Expression.BinaryOperator(BinaryOperators.Equality, Expression.IntLiteral(1), Expression.IntLiteral(1)),
            listOf(
              Statement.IntegerDeclaration(Token.Identifier(TokenType.Identifier, "someVar", 1, 2), Expression.IntLiteral(1))
            )
          )
        )
      )
    )
//    Expected <CodeBlock(statementList=[If(condition=BinaryOperator(operator=Equality, lhs=IntLiteral(value=1), rhs=IntLiteral(value=1)), body=[tatskaari.parsing.Statement$IntegerDeclaration@335eadca])])>,
//    actual   <CodeBlock(statementList=[If(condition=BinaryOperator(operator=Equality, lhs=IntLiteral(value=1), rhs=IntLiteral(value=1)), body=[tatskaari.parsing.Statement$NumberDeclaration@210366b4])])>.


    val actual = Parser().parse(program)

    TestUtil.compareASTs(expected, actual!!)
  }

  @Test
  fun testIfElse() {
    val parser = Parser()
    val program = parser.parse(TestUtil.loadProgram("IfElse"))
    val expectedAST = listOf(
      Statement.Input(Token.Identifier(TokenType.Identifier, "a", 1,2 )),
      Statement.IntegerDeclaration(Token.Identifier(TokenType.Identifier, "someVar", 1, 2), Expression.IntLiteral(0)),
      Statement.IfElse(
        Expression.BinaryOperator(BinaryOperators.Equality, Expression.IntLiteral(10), Expression.Identifier("a")),
        listOf(Statement.Assignment(Token.Identifier(TokenType.Identifier, "someVar", 1, 2), Expression.IntLiteral(1))),
        listOf(Statement.Assignment(Token.Identifier(TokenType.Identifier, "someVar", 1, 2), Expression.IntLiteral(2)))
      )
    )


    TestUtil.compareASTs(expectedAST, program!!)
  }
  @Test
  fun missingBody() {
    val program = "do if ( = 1 1 ) "
    val parser = Parser()
    parser.parse(program)
    assert(parser.parserExceptions.size == 1)
  }

  @Test
  fun missingCloseBlock() {
    val program = "if ( = 1 1 ) do"
    val parser = Parser()
    parser.parse(program)
    assert(parser.parserExceptions.size == 1)
  }

  @Test
  fun missingConditionBlock() {
    val program = "do if"
    val parser = Parser()
    parser.parse(program)
    assertEquals(parser.parserExceptions.size, 1 )

  }

  @Test
  fun invalidCondition() {
    val program = "do if(== 1 2) do end"
    val parser = Parser()
    parser.parse(program)
    assert(parser.parserExceptions.size == 1)
  }

  @Test
  fun invalidBracketsForBody() {
    val program = "if(== 1 2) do end"
    val parser = Parser()
    parser.parse(program)
    assert(parser.parserExceptions.size == 1)
  }
}