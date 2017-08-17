package tatskaari.parsing

import tatskaari.tokenising.Token
import tatskaari.tokenising.TokenType

enum class UnaryOperators {
  Not, Negative;

  companion object {
    fun getOperator(token: Token): UnaryOperators{
      when(token.tokenType){
        TokenType.Not -> return Not
        TokenType.Sub -> return Negative
        else -> throw InvalidOperatorToken(token)
      }
    }
  }
}