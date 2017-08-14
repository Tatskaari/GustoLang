package tatskaari.parsing

import tatskaari.tokenising.IToken
import tatskaari.tokenising.KeyWords
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

  fun parseCodeBlock(tokens: LinkedList<IToken>): Statement.CodeBlock {
    if (tokens.isEmpty()) {
      throw UnexpectedEndOfFile()
    }
    val body = parse(tokens)

    return Statement.CodeBlock(body)

  }

  fun parse(tokens: LinkedList<IToken>): LinkedList<Statement> {
    val statements = LinkedList<Statement>()
    while (!tokens.isEmpty()) {
      val token = tokens.removeAt(0)
      when (token) {
        KeyWords.OpenBlock -> {
          statements.add(parseCodeBlock(tokens))
        }
        KeyWords.CloseBlock -> {
          return statements
        }
        is Token.Identifier -> {
          val statement = parseAssign(tokens, token)
          statements.add(statement)
        }
        KeyWords.Val -> {
          val identifier = getNextToken(tokens, Token.Identifier("someVariable"))
          getNextToken(tokens, KeyWords.AssignOp)
          val expression = parseExpression(tokens)
          statements.add(Statement.ValDeclaration(identifier, expression))
        }
        KeyWords.If -> {
          val statement = parseIf(tokens)
          statements.add(statement)
        }
        KeyWords.Input -> {
          val identifier = getNextToken(tokens, Token.Identifier("someVariable"))
          statements.add(Statement.Input(identifier))
        }
        KeyWords.Output -> {
          val expr = parseExpression(tokens)
          statements.add(Statement.Output(expr))
        }
        KeyWords.While -> statements.add(parseWhile(tokens))
        else -> throw Lexer.InvalidInputException("Unexpected token $token expecting statement")
      }
    }
    return statements
  }

  fun parseAssign(tokens: LinkedList<IToken>, indent: Token.Identifier): Statement.Assignment {
    getNextToken(tokens, KeyWords.AssignOp)
    val expr = parseExpression(tokens)
    return Statement.Assignment(indent, expr)
  }

  fun parseIf(tokens: LinkedList<IToken>): Statement {
    getNextToken(tokens, KeyWords.OpenParen)

    val condition = parseExpression(tokens)

    getNextToken(tokens, KeyWords.CloseParen)
    getNextToken(tokens, KeyWords.OpenBlock)

    val ifBody = parseCodeBlock(tokens)

    if (checkNextToken(tokens, KeyWords.Else)) {
      getNextToken(tokens, KeyWords.Else)
      getNextToken(tokens, KeyWords.OpenBlock)
      val elseBody = parseCodeBlock(tokens)
      return Statement.IfElse(condition, ifBody.statementList, elseBody.statementList)
    } else {
      return Statement.If(condition, ifBody.statementList)
    }
  }

  fun parseWhile(tokens: LinkedList<IToken>) : Statement.While {
    getNextToken(tokens, KeyWords.OpenParen)
    val condition = parseExpression(tokens)
    getNextToken(tokens, KeyWords.CloseParen)
    getNextToken(tokens, KeyWords.OpenBlock)
    val body = parseCodeBlock(tokens).statementList
    return Statement.While(condition, body)
  }

  fun parseExpression(tokens: LinkedList<IToken>): Expression {
    if (!tokens.isEmpty()) {
      val token = tokens.removeFirst()
      when (token) {
        is Token.Num -> return Expression.Num(token.value)
        KeyWords.True -> return Expression.Bool(true)
        KeyWords.False -> return Expression.Bool(false)
        is Token.Op -> {
          val operator = token.operator
          val lhs = parseExpression(tokens)
          val rhs = parseExpression(tokens)
          return Expression.Op(operator, lhs, rhs)
        }
        KeyWords.Not -> return Expression.Not(parseExpression(tokens))
        KeyWords.And -> return Expression.And(parseExpression(tokens), parseExpression(tokens))
        KeyWords.Or -> return Expression.Or(parseExpression(tokens), parseExpression(tokens))
        is Token.Identifier -> return Expression.Identifier(token.text)
      }
      throw Lexer.InvalidInputException("Unexpected token $token, expected expression")
    }
    throw UnexpectedEndOfFile()
  }

  fun <T : IToken> getNextToken(tokens: LinkedList<IToken>, expectedToken: T): T {
    if (tokens.isEmpty()) {
      throw UnexpectedEndOfFile()
    }

    val token = tokens.removeFirst()
    if (token.isSameToken(expectedToken)) {
      return expectedToken::class.cast(token)
    }
    throw Lexer.InvalidInputException("unexpected input '$token', expected '$expectedToken'")
  }

  fun <T : IToken> checkNextToken(tokens: LinkedList<IToken>, expectedToken: T): Boolean {
    if (tokens.isEmpty()) {
      return false
    }

    val token = tokens[0]
    if (token.isSameToken(expectedToken)) {
      return true
    }
    return false
  }
}