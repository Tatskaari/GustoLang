package tatskaari

import org.testng.annotations.Test
import tatskaari.parsing.Expression
import tatskaari.parsing.Parser
import tatskaari.parsing.Statement
import tatskaari.tokenising.Lexer
import tatskaari.tokenising.Operator
import tatskaari.tokenising.Token
import kotlin.test.assertFailsWith

object ParserTest {
  @Test
  fun testNestedBlocks() {
    val program = TestUtil.loadProgram("NestedCodeBlock")
    val expectedAST = listOf(
      Statement.CodeBlock(
        listOf(
          Statement.CodeBlock(
            listOf(Statement.Assignment(Token.Identifier("someVar"), Expression.Num(5)))
          ),
          Statement.Assignment(Token.Identifier("someOtherVar"), Expression.Num(5))
        )
      )
    )

    val actualAST = Parser.parse(program)

    TestUtil.compareASTs(expectedAST, actualAST)
  }

  @Test
  fun testSimpleExpressions() {
    val program = "{val someVariable := + 12 12}"
    val expectedAST = listOf(
      Statement.CodeBlock(
        listOf(
          Statement.Assignment(
            Token.Identifier("someVariable"),
            Expression.Op(Operator.Add, Expression.Num(12), Expression.Num(12))
          )
        )
      )
    )

    val actualAST = Parser.parse(program)

    TestUtil.compareASTs(expectedAST, actualAST)
  }

  @Test
  fun TestExpressionWithIdentifier(){
    val program = "{val someVariable := 12 val someVar := + someVariable 1 }"
    val expectedAST = listOf(
      Statement.CodeBlock(
        listOf(
          Statement.Assignment(Token.Identifier("someVariable"), Expression.Num(12)),
          Statement.Assignment(
            Token.Identifier("someVar"),
            Expression.Op(Operator.Add, Expression.Identifier("someVariable"), Expression.Num(1))
          )
        )
      )
    )

    TestUtil.compareASTs(expectedAST, Parser.parse(program))
  }

  @Test
  fun testInputOutput(){
    val program = TestUtil.loadProgram("InputOutput")
    val expectedAST = listOf(
      Statement.CodeBlock(
        listOf(
          Statement.Input(Token.Identifier("a")),
          Statement.Output(Expression.Identifier("a"))
        )
      )
    )

    TestUtil.compareASTs(expectedAST, Parser.parse(program))
  }

  @Test
  fun testInvalidTokenInExpression(){
    val program = "{val someVariable := + 12 val}"
    assertFailsWith<Lexer.InvalidInputException> {
      Parser.parse(program)
    }
  }

  @Test
  fun testEOFInExpression(){
    val program = "{val someVariable := + 12"
    assertFailsWith<Parser.UnexpectedEndOfFile> {
      Parser.parse(program)
    }
  }

  @Test
  fun testMissingAssignInExpression(){
    val program = "{val someVariable + 12"
    assertFailsWith<Lexer.InvalidInputException> {
      Parser.parse(program)
    }
  }

  @Test
  fun testEOFAfterVal(){
    val program = "{val"
    assertFailsWith<Parser.UnexpectedEndOfFile> {
      Parser.parse(program)
    }
  }

  @Test
  fun testInvalidInputMidBlock() {
    val program = "{val a := 5 1234}"
    assertFailsWith<Lexer.InvalidInputException> {
      Parser.parse(program)
    }
  }

  @Test
  fun testMissingIdentifier() {
    val program = "{val := 5}"
    assertFailsWith<Lexer.InvalidInputException> {
      Parser.parse(program)
    }
  }

  @Test
  fun testIfElse(){
    val program = Parser.parse(TestUtil.loadProgram("IfElse"))
    val expectedAST = listOf(
        Statement.CodeBlock(
            listOf(
                Statement.Input(Token.Identifier("a")),
                Statement.IfElse(
                    Expression.Op(Operator.Equality, Expression.Num(10), Expression.Identifier("a")),
                    listOf(Statement.Assignment(Token.Identifier("someVar"), Expression.Num(1))),
                    listOf(Statement.Assignment(Token.Identifier("someVar"), Expression.Num(2)))
                )
            )
        )
    )

    TestUtil.compareASTs(expectedAST, program)
  }

  @Test
  fun temp(){
    val program = Parser.parse("{if (= 1 1) { } output 1}")
    val expectedAST = listOf(
        Statement.CodeBlock(
            listOf(
                Statement.If(
                    Expression.Op(Operator.Equality, Expression.Num(1), Expression.Num(1)),
                    listOf()
                ),
                Statement.Output(Expression.Num(1))
            )
        )
    )

    TestUtil.compareASTs(expectedAST, program)
  }
}
