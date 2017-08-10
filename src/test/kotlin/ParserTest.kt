import org.testng.annotations.Test
import parsing.Expression
import parsing.Parser
import parsing.Statement
import tokenising.Lexer
import tokenising.Operator
import tokenising.Token
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

object ParserTest {
  fun compareASTs(expected: List<Statement>, actual: List<Statement>) {
    expected.zip(actual).map { (expectedVal, actualVal) -> assertEquals(expectedVal, actualVal) }
  }

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

    compareASTs(expectedAST, actualAST)
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

    compareASTs(expectedAST, actualAST)
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
}
