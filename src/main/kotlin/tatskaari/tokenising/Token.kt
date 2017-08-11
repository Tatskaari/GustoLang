package tatskaari.tokenising

sealed class Token(val tokenText : String) {
  object OpenBlock : Token("{")
  object CloseBlock : Token("}")
  object Val : Token("val")
  object AssignOp : Token(":=")
  object If : Token("if")
  object OpenParen : Token("(")
  object CloseParen : Token(")")
  data class Identifier(val name: String) : Token(name)
  data class Num(val value: Int) : Token(value.toString())
  data class Op(val operator : Operator) : Token(operator.toString())

  override fun toString(): String {
    return tokenText
  }
}