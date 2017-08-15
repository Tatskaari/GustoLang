package tatskaari.parsing

import tatskaari.tokenising.IToken
import tatskaari.tokenising.KeyWords
import tatskaari.tokenising.Lexer
import tatskaari.tokenising.Token
import java.util.*

object Parser {
  class UnexpectedEndOfFile : RuntimeException("Unexpected end of file")

  fun parse(program: String): List<Statement> {
    return parse(Lexer.lex(program))
  }


  fun parseBody(tokens: LinkedList<IToken>) : List<Statement> {
    tokens.getNextToken(KeyWords.OpenBlock)
    val body = parse(tokens)
    tokens.getNextToken(KeyWords.CloseBlock)
    return body
  }

  fun parse(tokens: LinkedList<IToken>): LinkedList<Statement> {
    val statements = LinkedList<Statement>()
    while (!tokens.isEmpty()) {
      val token = tokens.removeFirst()
      when (token) {
        KeyWords.NewLine -> {}
        KeyWords.OpenBlock -> {
          statements.add(Statement.CodeBlock(parse(tokens)))
          tokens.getNextToken(KeyWords.CloseBlock)
        }
        KeyWords.CloseBlock -> {
          tokens.addFirst(token)
          return statements
        }
        KeyWords.Function -> statements.add(parseFunction(tokens))
        is Token.Identifier -> statements.add(parseAssign(tokens, token))
        KeyWords.Return -> statements.add(Statement.Return(ParseExpression.expression(tokens)))
        KeyWords.Val -> statements.add(parseValDeclaration(tokens))
        KeyWords.If -> {
          statements.add(parseIf(tokens))
        }
        KeyWords.Input -> {
          val identifier = tokens.getNextToken(Token.Identifier("someVariable"))
          statements.add(Statement.Input(identifier))
        }
        KeyWords.Output -> {
          val expr = ParseExpression.expression(tokens)
          statements.add(Statement.Output(expr))
        }
        KeyWords.While -> statements.add(parseWhile(tokens))
        else -> throw Lexer.InvalidInputException("Unexpected token $token expecting statement")
      }
    }
    return statements
  }

  fun parseValDeclaration(tokens: LinkedList<IToken>): Statement.ValDeclaration {
    val identifier = tokens.getNextToken(Token.Identifier("someVariable"))
    tokens.getNextToken(KeyWords.AssignOp)
    val expression = ParseExpression.expression(tokens)
    return Statement.ValDeclaration(identifier, expression)
  }

  fun parseAssign(tokens: LinkedList<IToken>, indent: Token.Identifier): Statement.Assignment {
    tokens.getNextToken(KeyWords.AssignOp)
    val expr = ParseExpression.expression(tokens)
    return Statement.Assignment(indent, expr)
  }

  fun parseIf(tokens: LinkedList<IToken>): Statement {
    tokens.getNextToken(KeyWords.OpenParen)

    val condition = ParseExpression.expression(tokens)

    tokens.getNextToken(KeyWords.CloseParen)

    val ifBody = parseBody(tokens)

    if (tokens.match(KeyWords.Else)) {
      tokens.getNextToken(KeyWords.Else)
      val elseBody = parseBody(tokens)
      return Statement.IfElse(condition, ifBody, elseBody)
    } else {
      return Statement.If(condition, ifBody)
    }
  }

  fun parseWhile(tokens: LinkedList<IToken>) : Statement.While {
    tokens.getNextToken(KeyWords.OpenParen)
    val condition = ParseExpression.expression(tokens)
    tokens.getNextToken(KeyWords.CloseParen)
    val body = parseBody(tokens)
    return Statement.While(condition, body)
  }

  fun parseFunction(tokens: LinkedList<IToken>): Statement.Function {
    val identifier = tokens.getNextToken(Token.Identifier("functionName"))
    val params = getParameterDecs(tokens, LinkedList())
    val body = parseBody(tokens)
    return Statement.Function(identifier, params, body)
  }

  fun getParameterDecs(tokens: LinkedList<IToken>, params : LinkedList<Token.Identifier>) : List<Token.Identifier> {
    if (tokens.isEmpty()){
      throw UnexpectedEndOfFile()
    }
    if (tokens.match(KeyWords.OpenParen)){
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


}