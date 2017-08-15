package tatskaari

import org.testng.annotations.Test
import tatskaari.parsing.Expression
import tatskaari.parsing.ParseExpression
import tatskaari.tokenising.IToken
import tatskaari.tokenising.KeyWords
import tatskaari.tokenising.Lexer
import tatskaari.tokenising.Token
import java.util.*



object ExprParseTest {





  @Test
  fun testExpr(){
    val program = Lexer.lex("(1 + 2) * 3 var a := 5")
    val expr = ParseExpression.expression(program)

  }

}