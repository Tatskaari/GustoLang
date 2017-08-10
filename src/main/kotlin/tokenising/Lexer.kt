package tokenising

import java.util.*

object Lexer {
  class InvalidInputException(reason: String) : RuntimeException(reason)

  fun lex(program: String): LinkedList<Token> {
    var rest = program.trim()
    val tokens = LinkedList<Token>()
    while (rest.isNotEmpty()) {
      val tokenResult = getNextToken(rest)
      if (tokenResult != null) {
        rest = tokenResult.program.trim()
        tokens.add(tokenResult.token)
      } else {
        throw InvalidInputException("Unexpected character: '" + program.substring(10) + "...'")
      }
    }

    return tokens
  }

  fun getNextToken(program: String): Tokenisers.ProgramTokenResult? {
    return Tokenisers.values()
      .map { it.lex(program) }
      .filter { it != null }.map { it!! }
      .minBy { it.program.length }
  }
}