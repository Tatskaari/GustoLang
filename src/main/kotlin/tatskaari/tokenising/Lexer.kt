package tatskaari.tokenising

import java.util.*

object Lexer {
  class InvalidInputException(reason: String) : RuntimeException(reason)

  fun lex(program: String): LinkedList<IToken> {
    val trimPred : (Char) -> Boolean = {
      when (it) {
        '\r' -> true
        ' ' -> true
        '\t' -> true
        else -> false
      }
    }

    var rest = program.trim (trimPred)
    val tokens = LinkedList<IToken>()
    while (rest.isNotEmpty()) {
      val tokenResult = getNextToken(rest)
      if (tokenResult != null) {
        rest = tokenResult.restOfProgram.trim(trimPred)
        tokens.add(tokenResult.token)
      } else {
        throw InvalidInputException("Unexpected character: '" + program.substring(10) + "...'")
      }
    }

    return tokens
  }

  fun getNextToken(program: String): Tokenisers.LexResult? {
    return Tokenisers.values()
      .map { it.lex(program) }.filterNotNull()
      .minBy { it.restOfProgram.length }
  }
}