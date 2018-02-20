package tatskaari.tokenising

sealed class Matcher {

  abstract fun lex(program: String): String?
  abstract fun getTokenDescription(): String

  class KeywordMatcher(private val tokenText: String) : Matcher() {
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

  object IntMatcher : Matcher() {
    private val regex = Regex("^[0-9]+")
    override fun lex(program: String): String? {
      val matchResult = regex.find(program)
      if (matchResult != null) {
        return matchResult.value
      }
      return null
    }

    override fun getTokenDescription(): String {
      return "integer"
    }
  }

  object NumMatcher : Matcher() {
    private val regex = Regex("^[0-9]+\\.[0-9]+")
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


  object CommentMatcher : Matcher() {
    override fun lex(program: String): String? {
      if (program.startsWith("(*")){
        var rest = program.substring(2)
        var comment = "(*"
        var nesting = 1
        if (rest.contains("*)")){
          while(nesting > 0){
            when {
              rest.startsWith("(*") -> {
                nesting++
                comment+="(*"
                rest = rest.substring(2)
              }
              rest.startsWith("*)") -> {
                nesting--
                comment+="*)"
                rest = rest.substring(2)
              }
              else -> {
                comment += rest.first()
                rest = rest.substring(1)
              }
            }
          }
          return comment
        }
      }
      return null
    }

    override fun getTokenDescription(): String {
      return "comment"
    }
  }

  object IdentifierMatcher : Matcher() {
    private val regex = Regex("""^[a-z][a-zA-Z0-9_']*""")

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
  object ConstructorMatcher : Matcher() {
    private val regex = Regex("""^[A-Z][a-zA-Z0-9_']*""")

    override fun lex(program: String): String? {
      val matchResult = regex.find(program)
      if (matchResult != null) {
        return matchResult.value
      }
      return null
    }

    override fun getTokenDescription(): String {
      return "constructor"
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