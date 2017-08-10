import org.testng.annotations.Test
import tokenising.Lexer
import tokenising.Operator
import tokenising.Token
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

object LexerTest {


  @Test
  fun testLexer() {
    val program = TestUtil.loadProgram("BasicBlocks")
    val tokenList = Lexer.lex(program)
    val expectedTokens = listOf(
      Token.OpenBlock,
      Token.ValDeclaration,
      Token.Identifier("someVariable"),
      Token.AssignmentOperator,
      Token.Num(12),
      Token.CloseBlock
    )

    assert(tokenList.size == expectedTokens.size)
    tokenList.zip(expectedTokens)
      .forEach { (actual, expect) -> assertEquals(actual, expect) }
  }

  @Test
  fun testInvalidToken() {
    assertFailsWith<Lexer.InvalidInputException> {Lexer.lex("{ var a := 123 } []';")}
  }

  @Test
  fun testExpression() {
    val program = "{val someVariable := + 12 12}"
    val tokenList = Lexer.lex(program)
    val expectedTokens = listOf(
      Token.OpenBlock,
      Token.ValDeclaration,
      Token.Identifier("someVariable"),
      Token.AssignmentOperator,
      Token.Op(Operator.Add),
      Token.Num(12),
      Token.Num(12),
      Token.CloseBlock
    )

    assert(tokenList.size == expectedTokens.size)
    tokenList.zip(expectedTokens)
      .forEach { (actual, expect) -> assertEquals(actual, expect) }
  }
}