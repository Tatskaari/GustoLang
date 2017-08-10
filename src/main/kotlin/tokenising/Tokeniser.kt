package tokenising

import StringUtils.rest

enum class Tokenisers(val lexer: (String) -> ProgramTokenResult?) {
  BLOCK_OPEN({ program -> tokeniseKeyWord(program, "{", Token.OpenBlock) }),
  BLOCK_CLOSE({ program -> tokeniseKeyWord(program, "}", Token.CloseBlock) }),
  VAL({ program -> tokeniseKeyWord(program, "val", Token.ValDeclaration) }),
  ASSIGN({ program -> tokeniseKeyWord(program, ":=", Token.AssignmentOperator) }),
  ADD({ program -> tokeniseKeyWord(program, "+", Token.Op(Operator.Add)) }),
  SUB({ program -> tokeniseKeyWord(program, "-", Token.Op(Operator.Sub)) }),
  IDENTIFIER({ regexTokeniser(it, """^[a-zA-Z]+""", Token::Identifier) }),
  NUMBER({ regexTokeniser(it, "^[0-9]+", { Token.Num(it.toInt()) }) });

  data class ProgramTokenResult(val program: String, val token: Token)

  companion object {
    fun tokeniseKeyWord(program: String, keyword: String, token: Token): ProgramTokenResult? {
      if (program.startsWith(keyword)) {
        return ProgramTokenResult(program.rest(keyword), token)
      }
      return null
    }

    fun regexTokeniser(program: String, regexString: String, tokenConstructor: (String) -> Token): ProgramTokenResult? {
      val regex = Regex(regexString)
      val matchResult = regex.find(program)
      if (matchResult != null) {
        return ProgramTokenResult(program.rest(matchResult.value), tokenConstructor(matchResult.value))
      }
      return null
    }


  }

  fun lex(program: String): ProgramTokenResult? {
    return lexer(program)
  }
}