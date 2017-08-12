package tatskaari.parsing

import tatskaari.tokenising.Lexer
import tatskaari.tokenising.Token
import java.util.*

object Parser {
  class UnexpectedEndOfFile : RuntimeException("Unexpected end of file")

  fun parse(program: String): List<Statement> {
    return parse(Lexer.lex(program))
  }

  fun parseCodeBlock(tokens : LinkedList<Token>) : Statement.CodeBlock {
    if (tokens.isEmpty()){
      throw UnexpectedEndOfFile()
    }
    val body = parse(tokens)

    return Statement.CodeBlock(body)

  }

  fun parse(program: LinkedList<Token>): LinkedList<Statement> {
    val statements = LinkedList<Statement>()
    while (!program.isEmpty()) {
      val token = program.removeAt(0)
      when (token) {
        is Token.OpenBlock -> {
          statements.add(parseCodeBlock(program))
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
      val ident = getNextToken(tokens, Token.Identifier("someVariable"))
      getNextToken(tokens, Token.AssignOp)
      val expr = parseExpression(tokens)
      return Statement.Assignment(ident, expr)
  }

  fun parseIf(tokens : LinkedList<Token>) : Statement.If {
    getNextToken(tokens, Token.OpenParen)

    val condition = parseExpression(tokens)

    getNextToken(tokens, Token.CloseParen)
    getNextToken(tokens, Token.OpenBlock)

    val body = parseCodeBlock(tokens)

    return Statement.If(condition, body.statementList)
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
        is Token.Identifier -> return Expression.Ident(token.tokenText)
      }
      throw Lexer.InvalidInputException("Unexpected token $token, expected expression")
    }
    throw UnexpectedEndOfFile()
  }

  // Normally type parameters are not instantiated into objects so doing as? with them can cause funny behavior
  // inlining the function and marking the type as reified will make this work by creating a specialised
  // inlined function every time this is called
  inline fun <reified T : Token> getNextToken(tokens : LinkedList<Token>, expectedToken : T) : T {
    if (tokens.isEmpty()){
      throw UnexpectedEndOfFile()
    }

    val token = tokens.removeFirst()
    if (token is T) {
      return token
    }
    throw Lexer.InvalidInputException("unexpected input '$token', expected '$expectedToken'")
  }
}