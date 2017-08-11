package tatskaari.tokenising

import tatskaari.StringUtils.rest

enum class Tokenisers(val lexer: (String) -> LexResult?) {
  BLOCK_OPEN({ tokeniseKeyWord(it, Token.OpenBlock) }),
  BLOCK_CLOSE({ tokeniseKeyWord(it, Token.CloseBlock) }),
  VAL({ tokeniseKeyWord(it, Token.Val) }),
  ASSIGN({ tokeniseKeyWord(it, Token.AssignOp) }),
  IF({ tokeniseKeyWord(it, Token.If) }),
  OPEN_PAREN({ tokeniseKeyWord(it, Token.OpenParen) }),
  CLOSE_PAREN({ tokeniseKeyWord(it, Token.CloseParen) }),
  ADD({ tokeniseKeyWord(it, Token.Op(Operator.Add)) }),
  SUB({ tokeniseKeyWord(it, Token.Op(Operator.Sub)) }),
  EQUALITY({ tokeniseKeyWord(it, Token.Op(Operator.Equality)) } ),
  IDENTIFIER({ regexTokeniser(it, """^[a-zA-Z]+""", Token::Identifier) }),
  NUMBER({ regexTokeniser(it, "^[0-9]+", Token::Num, String::toInt) });

  data class LexResult(val rest: String, val token: Token)

  companion object {
    fun tokeniseKeyWord(program: String, token: Token): LexResult? {
      if (program.startsWith(token.tokenText)) {
        return LexResult(program.rest(token.tokenText), token)
      }
      return null
    }

    fun regexTokeniser(program : String, regexString: String, tokenConstructor: (String) -> Token) : LexResult?{
      return regexTokeniser(program, regexString, tokenConstructor, { it })
    }

    fun <T> regexTokeniser(program: String, regexString: String, tokenConstructor: (T) -> Token, parser : ((String) -> T)) : LexResult? {
      val regex = Regex(regexString)
      val matchResult = regex.find(program)
      if (matchResult != null) {
        val result : T = parser(matchResult.value)
        return LexResult(program.rest(matchResult.value), tokenConstructor(result))
      }
      return null
    }


  }

  fun lex(program: String): LexResult? {
    return lexer(program)
  }
}