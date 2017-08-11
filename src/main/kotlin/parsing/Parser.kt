package parsing

import tokenising.Lexer
import tokenising.Token
import java.util.*

object Parser {
  class UnexpectedEndOfFile : RuntimeException("Unexpected end of file")

  fun parse(program: String): List<Statement> {
    return parse(Lexer.lex(program), LinkedList<Statement>())
  }

  fun parse(program: LinkedList<Token>, statements: LinkedList<Statement>): LinkedList<Statement> {
    var tokens = program
    while (!tokens.isEmpty()) {
      val token = tokens.removeAt(0)
      when (token) {
        is Token.OpenBlock -> {
          val parseResult = parse(tokens, LinkedList<Statement>())
          statements.add(Statement.CodeBlock(parseResult))
        }
        is Token.CloseBlock -> {
          return statements
        }
        is Token.ValDeclaration -> {
          val statement = parseAssign(tokens)
          statements.add(statement)
        }
        else -> throw Lexer.InvalidInputException("Unexpected token $token expecting statement")
      }
    }
    return statements
  }

  fun parseAssign(tokens: LinkedList<Token>): Statement.Assignment {
    if (tokens.size > 2){
      val ident = tokens.removeFirst()
      val assignment = tokens.removeFirst()
      if (ident is Token.Identifier && assignment is Token.AssignmentOperator){
        val expr = parseExpression(tokens)
        return Statement.Assignment(ident, expr)
      } else {
        throw Lexer.InvalidInputException("Unexpected input '$ident $assignment' expected 'variable :='")
      }
    }
    throw UnexpectedEndOfFile()
  }

  fun parseExpression(tokens: LinkedList<Token>): Expression {
    if (!tokens.isEmpty()){
      val token = tokens.removeFirst()
      when (token) {
        is Token.Num -> return Expression.Num(token.value)
        is Token.Op -> {
          val operator = token.operator
          val lhs = parseExpression(tokens)
          val rhs = parseExpression(tokens)
          return Expression.Op(operator, lhs, rhs)
        }
      }
      throw Lexer.InvalidInputException("Unexpected token $token, expected expression")
    }
    throw UnexpectedEndOfFile()
  }
}