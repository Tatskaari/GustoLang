package tatskaari.tokenising

sealed class Matcher {

  abstract fun lex(program: String): String?
  abstract fun getTokenDescription(): String

  class KeywordMatcher(val tokenText: String) : Matcher() {
    override fun lex(program: String): String? {
      if (program.startsWith(tokenText)) {
        return tokenText
      }
      return null
    }

    override fun getTokenDescription(): String{
      return tokenText
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

    override fun getTokenDescription(): String {
      return "number"
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

    override fun getTokenDescription(): String {
      return "identifier"
    }
  }
  object TextMatcher: Matcher() {
    override fun lex(program: String): String? {
      var rest: String
      if (program.startsWith("\"")){
        rest = program.substring(1)
        var stringText = "\""
        while(program.isNotEmpty()){
          if(rest.startsWith("\"\"")){
            rest = rest.substring(2)
            stringText+="\"\""
          } else if(rest.startsWith("\"")){
            return stringText + "\""
          } else {
            stringText+=rest[0]
            rest = rest.substring(1)
          }
        }
      }
      return null
    }

    override fun getTokenDescription(): String {
      return "text literal"
    }
  }
}