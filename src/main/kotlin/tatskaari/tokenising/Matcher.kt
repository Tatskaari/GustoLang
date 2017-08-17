package tatskaari.tokenising

sealed class Matcher {

    abstract fun lex(program: String): String?

    class KeywordMatcher(val tokenText: String): Matcher() {
        override fun lex(program: String): String? {
            if (program.startsWith(tokenText)) {
                return tokenText
            }
            return null
        }
    }

    object NumberMatcher : Matcher() {
        val regex = Regex("^[0-9]+")
        override fun lex(program: String): String? {
            val matchResult = regex.find(program)
            if (matchResult != null) {
                return matchResult.value
            }
            return null
        }
    }

    object IdentifierMatcher : Matcher() {
        val regex = Regex("""^[a-zA-Z]+""")


        override fun lex(program: String): String? {
            val matchResult = regex.find(program)
            if (matchResult != null) {
                return matchResult.value
            }
            return null
        }
    }
}