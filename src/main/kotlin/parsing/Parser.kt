package parsing

import tokenising.Lexer
import tokenising.Token
import java.util.*

object Parser {
  data class ParseStatementsResult(val statements: LinkedList<Statement>, val rest: LinkedList<Token>)
  data class ParseSingleStatementResult(val statement: Statement, val rest: LinkedList<Token>)
  data class ParseExpressionResult(val expression: Expression, val rest: LinkedList<Token>)

  class UnexpectedEndOfFile : RuntimeException("Unexpected end of file")

  fun parse(program: String): List<Statement> {
    val (statements, _) = parse(Lexer.lex(program), LinkedList<Statement>())
    return statements
  }

  fun parse(program: LinkedList<Token>, statements: LinkedList<Statement>): ParseStatementsResult {
    var tokens = program
    while (!tokens.isEmpty()) {
      val token = tokens.removeAt(0)
      when (token) {
        is Token.OpenBlock -> {
          val parseResult = parse(tokens, LinkedList<Statement>())
          statements.add(Statement.CodeBlock(parseResult.statements))
        }
        is Token.CloseBlock -> {
          return ParseStatementsResult(statements, tokens)
        }
        is Token.ValDeclaration -> {
          val (statement, rest) = parseAssign(tokens)
          statements.add(statement)
          tokens = rest
        }
        else -> throw Lexer.InvalidInputException("Unexpected token $token expecting statement")
      }
    }
    return ParseStatementsResult(statements, tokens)
  }

  fun parseAssign(tokens: LinkedList<Token>): ParseSingleStatementResult {
    if (tokens.size > 2){
      val ident = tokens.removeFirst()
      val assignment = tokens.removeFirst()
      if (ident is Token.Identifier && assignment is Token.AssignmentOperator){
        val (expr, rest) = parseExpression(tokens)
        return ParseSingleStatementResult(Statement.Assignment(ident, expr), rest)
      } else {
        throw Lexer.InvalidInputException("Unexpected input $ident $assignment expected variable :=")
      }
    }
    throw UnexpectedEndOfFile()
  }

  fun parseExpression(tokens: LinkedList<Token>): ParseExpressionResult {
    if (!tokens.isEmpty()){
      val token = tokens.removeFirst()
      when (token) {
        is Token.Num -> return ParseExpressionResult(Expression.Num(token.value), tokens)
        is Token.Op -> {
          val operator = token.operator
          val (lhs, lhsRest) = parseExpression(tokens)
          val (rhs, rhsRest) = parseExpression(lhsRest)
          return ParseExpressionResult(Expression.Op(operator, lhs, rhs), rhsRest)
        }
      }
      throw Lexer.InvalidInputException("Unexpected token $token, expected expression")
    }
    throw UnexpectedEndOfFile()
  }
}