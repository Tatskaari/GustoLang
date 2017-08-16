package tatskaari.parsing

import tatskaari.tokenising.IToken
import tatskaari.tokenising.KeyWords

enum class UnaryOperators {
  Not, Negative;

  companion object {
    fun getOperator(token: IToken): UnaryOperators{
      when(token){
        KeyWords.Not -> return Not
        KeyWords.Sub -> return Negative
        else -> throw InvalidOperatorToken(token)
      }
    }
  }
}