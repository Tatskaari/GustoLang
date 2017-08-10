package tokenising

sealed class Token {
  object OpenBlock : Token()
  object CloseBlock : Token()
  object ValDeclaration : Token()
  object AssignmentOperator : Token()
  data class Identifier(val name: String) : Token()
  data class Num(val value: Int) : Token()
  data class Op(val operator : Operator) : Token()
}