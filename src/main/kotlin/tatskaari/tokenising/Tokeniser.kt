package tatskaari.tokenising

import tatskaari.StringUtils.rest

data class LexResult(val restOfProgram: String, val token: Token)

sealed class Tokeniser {

    abstract fun lex(program: String): LexResult?

    class KeywordTokeniser(val constructor: (String) -> Token, val tokenText: String): Tokeniser() {
        override fun lex(program: String): LexResult? {
            if (program.startsWith(tokenText)) {
                return LexResult(program.rest(tokenText), constructor(tokenText))
            }
            return null
        }
    }

    object NumberTokeniser: Tokeniser() {
        val regex = Regex("^[0-9]+")


        override fun lex(program: String): LexResult? {
            val matchResult = regex.find(program)
            if (matchResult != null) {
                val result = matchResult.value
                return LexResult(program.rest(matchResult.value), Token.Num(result.toInt()))
            }
            return null
        }
    }

    object IdentifierTokeniser: Tokeniser() {
        val regex = Regex("""^[a-zA-Z]+""")


        override fun lex(program: String): LexResult? {
            val matchResult = regex.find(program)
            if (matchResult != null) {
                val result = matchResult.value
                return LexResult(program.rest(matchResult.value), Token.Identifier(result))
            }
            return null
        }
    }
}