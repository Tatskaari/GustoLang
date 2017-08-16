package tatskaari.tokenising

import java.util.*

object Lexer {
  class InvalidInputException(reason: String) : RuntimeException(reason)

  fun lex(program: String): LinkedList<IToken> {
    var rest = program.trim()
    val tokens = LinkedList<IToken>()
    while (rest.isNotEmpty()) {
      val tokenResult = getNextToken(rest)
      if (tokenResult != null) {
        rest = tokenResult.restOfProgram.trim()
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