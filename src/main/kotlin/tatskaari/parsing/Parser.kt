package tatskaari.parsing

import tatskaari.tokenising.Lexer
import tatskaari.tokenising.Token
import java.util.*

object Parser {
  class UnexpectedEndOfFile : RuntimeException("Unexpected end of file")

  fun parse(program: String): List<Statement> {
    return parse(Lexer.lex(program), LinkedList<Statement>())
  }

  fun parse(program: LinkedList<Token>, statements: LinkedList<Statement>): LinkedList<Statement> {
    while (!program.isEmpty()) {
      val token = program.removeAt(0)
      when (token) {
        is Token.OpenBlock -> {
          val parseResult = parse(program, LinkedList<Statement>())
          statements.add(Statement.CodeBlock(parseResult))
        }
        is Token.CloseBlock -> {
          return statements
        }
        is Token.Val -> {
          val statement = parseAssign(program)
          statements.add(statement)
        }
        is Token.If -> {
          val statement = parseIf(program)
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
      if (ident is Token.Identifier && assignment is Token.AssignOp){
        val expr = parseExpression(tokens)
        return Statement.Assignment(ident, expr)
      } else {
        throw Lexer.InvalidInputException("Unexpected input '$ident $assignment' expected 'variable :='")
      }
    }
    throw UnexpectedEndOfFile()
  }

  fun parseIf(tokens : LinkedList<Token>) : Statement.If {
    if (!tokens.isEmpty()) {
      val openParen = tokens.removeFirst()
      if (openParen is Token.OpenParen){
        val condition = parseExpression(tokens)
        val closeParen = tokens.removeFirst()
        if(closeParen is Token.CloseParen){
          val body = parse(tokens, LinkedList())
          if (body.size == 1 && body[0] is Statement.CodeBlock){
            return Statement.If(condition, (body[0] as Statement.CodeBlock).statementList)
          }
          //TODO this should check what happened
          throw UnexpectedEndOfFile()
        }
        throw Lexer.InvalidInputException("Unexpected input '$closeParen' expected ')'")
      }
      throw Lexer.InvalidInputException("Unexpected input '$openParen' expected '('")
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