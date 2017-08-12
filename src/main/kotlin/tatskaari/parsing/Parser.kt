package tatskaari.parsing

import tatskaari.tokenising.Lexer
import tatskaari.tokenising.Token
import java.util.*
import kotlin.reflect.full.cast
import kotlin.reflect.full.isSubclassOf

object Parser {
  class UnexpectedEndOfFile : RuntimeException("Unexpected end of file")

  fun parse(program: String): List<Statement> {
    return parse(Lexer.lex(program))
  }

  fun parseCodeBlock(tokens: LinkedList<Token>): Statement.CodeBlock {
    if (tokens.isEmpty()) {
      throw UnexpectedEndOfFile()
    }
    val body = parse(tokens)

    return Statement.CodeBlock(body)

  }

  fun parse(tokens: LinkedList<Token>): LinkedList<Statement> {
    val statements = LinkedList<Statement>()
    while (!tokens.isEmpty()) {
      val token = tokens.removeAt(0)
      when (token) {
        is Token.OpenBlock -> {
          statements.add(parseCodeBlock(tokens))
        }
        is Token.CloseBlock -> {
          return statements
        }
        is Token.Val -> {
          val statement = parseAssign(tokens)
          statements.add(statement)
        }
        is Token.If -> {
          val statement = parseIf(tokens)
          statements.add(statement)
        }
        is Token.Input -> {
          val identifier = getNextToken(tokens, Token.Identifier("someVariable"))
          statements.add(Statement.Input(identifier))
        }
        is Token.Output -> {
          val expr = parseExpression(tokens)
          statements.add(Statement.Output(expr))
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

  fun parseIf(tokens: LinkedList<Token>): Statement {
    getNextToken(tokens, Token.OpenParen)

    val condition = parseExpression(tokens)

    getNextToken(tokens, Token.CloseParen)
    getNextToken(tokens, Token.OpenBlock)

    val ifBody = parseCodeBlock(tokens)

    if (checkNextToken(tokens, Token.Else)) {
      getNextToken(tokens, Token.Else)
      getNextToken(tokens, Token.OpenBlock)
      val elseBody = parseCodeBlock(tokens)
      return Statement.IfElse(condition, ifBody.statementList, elseBody.statementList)
    } else {
      return Statement.If(condition, ifBody.statementList)
    }
  }

  fun parseExpression(tokens: LinkedList<Token>): Expression {
    if (!tokens.isEmpty()) {
      val token = tokens.removeFirst()
      when (token) {
        is Token.Num -> return Expression.Num(token.value)
        is Token.True -> return Expression.Bool(true)
        is Token.False -> return Expression.Bool(false)
        is Token.Op -> {
          val operator = token.operator
          val lhs = parseExpression(tokens)
          val rhs = parseExpression(tokens)
          return Expression.Op(operator, lhs, rhs)
        }
        is Token.Identifier -> return Expression.Identifier(token.tokenText)
      }
      throw Lexer.InvalidInputException("Unexpected token $token, expected expression")
    }
    throw UnexpectedEndOfFile()
  }

  fun <T : Token> getNextToken(tokens: LinkedList<Token>, expectedToken: T): T {
    if (tokens.isEmpty()) {
      throw UnexpectedEndOfFile()
    }

    val token = tokens.removeFirst()
    if (token::class.isSubclassOf(expectedToken::class)) {
      return expectedToken::class.cast(token)
    }
    throw Lexer.InvalidInputException("unexpected input '$token', expected '$expectedToken'")
  }

  fun <T : Token> checkNextToken(tokens: LinkedList<Token>, expectedToken: T): Boolean {
    if (tokens.isEmpty()) {
      return false
    }

    val token = tokens[0]
    if (token::class.isSubclassOf(expectedToken::class)) {
      return true
    }
    return false
  }
}