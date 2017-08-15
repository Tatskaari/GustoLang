package tatskaari.tokenising
interface IToken {
  fun getTokenText() : String

  fun isSameToken(token : IToken) : Boolean
}
sealed class Token(val text: String) : IToken {
  data class Identifier(val name: String) : Token(name)
  data class Num(val value: Int) : Token(value.toString())

  override fun toString(): String {
    return text
  }

  override fun getTokenText(): String {
    return text
  }

  override fun isSameToken(token: IToken): Boolean {
    return token::class == this::class
  }

}

enum class KeyWords(val text : String) : IToken {
  Add("+"),
  Sub("-"),
  Mul("*"),
  Div("/"),
  LessThan("<"),
  GreaterThan(">"),
  LessThanEq("<="),
  GreaterThanEq(">="),
  And("and"),
  Function("function"),
  Return("return"),
  Comma(","),
  Or("or"),
  OpenBlock("{"),
  CloseBlock("}"),
  Val("val"),
  AssignOp(":="),
  Not("!"),
  Equality("="),
  NotEquality("="),
  If("if"),
  Else("else"),
  True("true"),
  False("false"),
  While("while"),
  OpenParen("("),
  CloseParen(")"),
  Input("input"),
  NewLine("\n"),
  Output("output");

  override fun toString(): String {
    return text
  }

  override fun getTokenText(): String {
    return text
  }

  override fun isSameToken(token: IToken): Boolean {
    if(token is KeyWords) {
      return token == this
    } else {
      return false
    }
  }
}