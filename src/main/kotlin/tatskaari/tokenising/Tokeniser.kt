package tatskaari.tokenising

import tatskaari.StringUtils.rest

enum class Tokenisers(val lexer: (String) -> LexResult?) {
  KEYWORD({ tokeniseKeyWord(it) }),
  OPERATOR({ tokeniseOperator(it) }),
  IDENTIFIER({ regexTokeniser(it, """^[a-zA-Z]+""", Token::Identifier) }),
  NUMBER({ regexTokeniser(it, "^[0-9]+", Token::Num, String::toInt) });

  data class LexResult(val restOfProgram: String, val token: IToken)

  companion object {
    fun stringTokeniser(program: String, token: IToken): LexResult? {
      if (program.startsWith(token.getTokenText())) {
        return LexResult(program.rest(token.getTokenText()), token)
      }
      return null
    }

    fun tokeniseKeyWord(program: String): LexResult? {
      return KeyWords.values()
        .map { stringTokeniser(program, it) }
        .filterNotNull()
        .maxBy { it.restOfProgram }
    }

    fun tokeniseOperator(program: String) : LexResult? {
      return Operator.values()
        .map { stringTokeniser(program, Token.Op(it)) }
        .filterNotNull()
        .minBy { it.restOfProgram }
    }

    fun regexTokeniser(program: String, regexString: String, tokenConstructor: (String) -> Token): LexResult? {
      return regexTokeniser(program, regexString, tokenConstructor, { it })
    }

    fun <T> regexTokeniser(program: String, regexString: String, tokenConstructor: (T) -> Token, parser: ((String) -> T)): LexResult? {
      val regex = Regex(regexString)
      val matchResult = regex.find(program)
      if (matchResult != null) {
        val result: T = parser(matchResult.value)
        return LexResult(program.rest(matchResult.value), tokenConstructor(result))
      }
      return null
    }


  }

  fun lex(program: String): LexResult? {
    return lexer(program)
  }
}