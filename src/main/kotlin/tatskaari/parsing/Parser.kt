package tatskaari.parsing

import tatskaari.tokenising.IToken
import tatskaari.tokenising.KeyWords
import tatskaari.tokenising.Lexer
import tatskaari.tokenising.Token
import java.util.*
import kotlin.reflect.full.cast

object Parser {
  class UnexpectedEndOfFile : RuntimeException("Unexpected end of file")

  fun parse(program: String): List<Statement> {
    return parse(Lexer.lex(program))
  }


  fun parseBody(tokens: LinkedList<IToken>) : List<Statement> {
    getNextToken(tokens, KeyWords.OpenBlock)
    val body = parse(tokens)
    getNextToken(tokens, KeyWords.CloseBlock)
    return body
  }

  fun parse(tokens: LinkedList<IToken>): LinkedList<Statement> {
    val statements = LinkedList<Statement>()
    while (!tokens.isEmpty()) {
      val token = tokens.removeFirst()
      when (token) {
        KeyWords.OpenBlock -> {
          statements.add(Statement.CodeBlock(parse(tokens)))
          getNextToken(tokens, KeyWords.CloseBlock)
        }
        KeyWords.CloseBlock -> {
          tokens.addFirst(token)
          return statements
        }
        KeyWords.Function -> statements.add(parseFunction(tokens))
        is Token.Identifier -> statements.add(parseAssign(tokens, token))
        KeyWords.Return -> statements.add(Statement.Return(parseExpression(tokens)))
        KeyWords.Val -> statements.add(parseValDeclaration(tokens))
        KeyWords.If -> {
          statements.add(parseIf(tokens))
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

  fun parseValDeclaration(tokens: LinkedList<IToken>): Statement.ValDeclaration {
    val identifier = getNextToken(tokens, Token.Identifier("someVariable"))
    getNextToken(tokens, KeyWords.AssignOp)
    val expression = parseExpression(tokens)
    return Statement.ValDeclaration(identifier, expression)
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

    val ifBody = parseBody(tokens)

    if (checkNextToken(tokens, KeyWords.Else)) {
      getNextToken(tokens, KeyWords.Else)
      val elseBody = parseBody(tokens)
      return Statement.IfElse(condition, ifBody, elseBody)
    } else {
      return Statement.If(condition, ifBody)
    }
  }

  fun parseWhile(tokens: LinkedList<IToken>) : Statement.While {
    getNextToken(tokens, KeyWords.OpenParen)
    val condition = parseExpression(tokens)
    getNextToken(tokens, KeyWords.CloseParen)
    val body = parseBody(tokens)
    return Statement.While(condition, body)
  }

  fun parseExpression(tokens: LinkedList<IToken>): Expression {
    if (!tokens.isEmpty()) {
      val token = tokens.removeFirst()
      when (token) {
        KeyWords.True -> return Expression.Bool(true)
        KeyWords.False -> return Expression.Bool(false)
        KeyWords.Not -> return Expression.Not(parseExpression(tokens))
        KeyWords.And -> return Expression.And(parseExpression(tokens), parseExpression(tokens))
        KeyWords.Or -> return Expression.Or(parseExpression(tokens), parseExpression(tokens))
        is Token.Num -> return Expression.Num(token.value)
        is Token.Op -> {
          val operator = token.operator
          val lhs = parseExpression(tokens)
          val rhs = parseExpression(tokens)
          return Expression.Op(operator, lhs, rhs)
        }
        is Token.Identifier -> {
          if (checkNextToken(tokens, KeyWords.OpenParen)){
            return Expression.FunctionCall(token, getParameters(tokens, LinkedList()))
          } else {
            return Expression.Identifier(token.text)
          }
        }
      }
      throw Lexer.InvalidInputException("Unexpected token $token, expected expression")
    }
    throw UnexpectedEndOfFile()
  }

  fun getParameters(tokens: LinkedList<IToken>, params : LinkedList<Statement.ValDeclaration>) : List<Statement.ValDeclaration> {
    if (tokens.isEmpty()){
      throw UnexpectedEndOfFile()
    }
    if (checkNextToken(tokens, KeyWords.OpenParen)){
      tokens.removeFirst()
      return getParameters(tokens, params)
    }
    val token = tokens.removeFirst()
    when(token){
      KeyWords.CloseParen -> return params
      is Token.Identifier -> {
        tokens.addFirst(token)
        params.add(parseValDeclaration(tokens))
        return getParameters(tokens, params)
      }
      KeyWords.Comma -> return getParameters(tokens, params)
    }
    throw Lexer.InvalidInputException("unexpected input '$token', expected one of ',', 'Identifier(name=someIdentifier)' or ')'")
  }

  fun parseFunction(tokens: LinkedList<IToken>): Statement.Function {
    val identifier = getNextToken(tokens, Token.Identifier("functionName"))
    val params = getParameterDecs(tokens, LinkedList())
    val body = parseBody(tokens)
    return Statement.Function(identifier, params, body)
  }

  fun getParameterDecs(tokens: LinkedList<IToken>, params : LinkedList<Token.Identifier>) : List<Token.Identifier> {
    if (tokens.isEmpty()){
      throw UnexpectedEndOfFile()
    }
    if (checkNextToken(tokens, KeyWords.OpenParen)){
      tokens.removeFirst()
      return getParameterDecs(tokens, params)
    }
    val token = tokens.removeFirst()
    when(token){
      KeyWords.CloseParen -> return params
      is Token.Identifier -> {
        params.add(token)
        return getParameterDecs(tokens, params)
      }
      KeyWords.Comma -> return getParameterDecs(tokens, params)
    }
    throw Lexer.InvalidInputException("unexpected input '$token', expected one of ',', 'Identifier(name=someIdentifier)' or ')'")
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