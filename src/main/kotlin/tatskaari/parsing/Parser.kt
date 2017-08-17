package tatskaari.parsing

import tatskaari.tokenising.Token
import tatskaari.tokenising.Lexer
import tatskaari.tokenising.TokenType
import java.util.*

object Parser {
  class UnexpectedEndOfFile : RuntimeException("Unexpected end of file")

  fun parse(source: String): List<Statement> {
    return program(Lexer.lex(source))
  }

  // program => (statement)*
  fun program(tokens: LinkedList<Token>) : List<Statement> {
    val statements = LinkedList<Statement>()
    while(tokens.isNotEmpty()){
      statements.add(statement(tokens))
    }

    return statements
  }

  // statement => if | while | codeBlock | function | return | valueDeclaration | input | output | assignment;
  fun statement(tokens: LinkedList<Token>): Statement {
    val token = tokens.lookAhead()
    when(token.tokenType){
      TokenType.If -> return iff(tokens)
      TokenType.While -> return whilee(tokens)
      TokenType.OpenBlock -> return codeBlock(tokens)
      TokenType.Function -> return function(tokens)
      TokenType.Return -> return returnn(tokens)
      TokenType.Val -> return valueDeclaration(tokens)
      TokenType.Input -> return input(tokens)
      TokenType.Output -> return output(tokens)
      TokenType.Identifier -> return assignment(tokens)
      else -> throw Lexer.InvalidInputException("Unexpected token $token, expected statement")
    }
  }

  // if => "if" expression codeBlock ("else" codeBlock)?
  fun iff(tokens: LinkedList<Token>): Statement {
    tokens.getNextToken(TokenType.If)
    val condition = expression(tokens)
    val trueBody = codeBlock(tokens)
    if (tokens.match(TokenType.Else)){
      tokens.consumeToken()
      val elseBody = codeBlock(tokens)
      return Statement.IfElse(condition, trueBody, elseBody)
    } else {
      return Statement.If(condition, trueBody)
    }
  }

  // while => "while" "(" expression ")" codeBlock
  fun whilee(tokens: LinkedList<Token>): Statement.While {
    tokens.getNextToken(TokenType.While)
    val condition = expression(tokens)
    val body = codeBlock(tokens)
    return Statement.While(condition, body)
  }

  // codeBlock => "{" (statement)* "}"
  fun codeBlock(tokens: LinkedList<Token>): Statement.CodeBlock {
    tokens.getNextToken(TokenType.OpenBlock)
    val body = LinkedList<Statement>()
    while(!tokens.match(TokenType.CloseBlock)){
      body.add(statement(tokens))
    }
    tokens.getNextToken(TokenType.CloseBlock)
    return Statement.CodeBlock(body)
  }

  // function => STRING "(" (STRING(",")?)*  ")" codeBlock
  fun function(tokens: LinkedList<Token>): Statement.Function {
    tokens.getNextToken(TokenType.Function)
    val name = tokens.getIdentifier()
    tokens.getNextToken(TokenType.OpenParen)
    val parameters = LinkedList<Token.Identifier>()
    while (!tokens.match(TokenType.CloseParen)){
      val token = tokens.consumeToken()
      when(token.tokenType){
        TokenType.Comma -> {}
        TokenType.Identifier -> parameters.add(token as Token.Identifier)
        else -> throw Lexer.InvalidInputException("Invalid input in exception $token")
      }
    }

    tokens.getNextToken(TokenType.CloseParen)
    val body = codeBlock(tokens)

    return Statement.Function(name, parameters, body)
  }

  // return => "return" expression
  fun returnn(tokens: LinkedList<Token>): Statement.Return {
    tokens.getNextToken(TokenType.Return)
    val expression = expression(tokens)
    return Statement.Return(expression)
  }

  // valueDeclaration => "val" STRING ":=" expression
  fun valueDeclaration(tokens: LinkedList<Token>): Statement.ValDeclaration {
    tokens.getNextToken(TokenType.Val)
    val identifier = tokens.getIdentifier()
    tokens.getNextToken(TokenType.AssignOp)
    val expression = expression(tokens)
    return Statement.ValDeclaration(identifier, expression)
  }

  // assignment => STRING ":=" expression
  fun assignment(tokens: LinkedList<Token>): Statement.Assignment {
    val identifier = tokens.getIdentifier()
    tokens.getNextToken(TokenType.AssignOp)
    val expression = expression(tokens)
    return Statement.Assignment(identifier, expression)
  }

  // input => "input" STRING
  fun input(tokens: LinkedList<Token>): Statement.Input {
    tokens.getNextToken(TokenType.Input)
    val identifier = tokens.getIdentifier()
    return Statement.Input(identifier)
  }

  // output => "output" expression
  fun output(tokens: LinkedList<Token>): Statement.Output {
    tokens.getNextToken(TokenType.Output)
    val expression = expression(tokens)
    return Statement.Output(expression)
  }

  // expression => logical
  fun expression(tokens: LinkedList<Token>) : Expression{
    return logical(tokens)
  }

  // logical => equality ( ( "and" | "or" ) equality )*
  fun logical(tokens: LinkedList<Token>) : Expression{
    var expr = equality(tokens)
    while(tokens.matchAny( listOf(TokenType.Or, TokenType.And))){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = equality(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  // equality => comparison ( ( "!=" | "==" ) comparison )*
  fun equality(tokens: LinkedList<Token>) : Expression{
    var expr = comparison(tokens)
    while(tokens.matchAny(listOf(TokenType.Equality, TokenType.NotEquality))){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = comparison(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  // comparison => addition ( ( ">" | ">=" | "<" | "<=" ) addition )*
  fun comparison(tokens: LinkedList<Token>) : Expression{
    var expr = addition(tokens)
    while(tokens.matchAny(listOf(TokenType.GreaterThan, TokenType.GreaterThanEq, TokenType.LessThan, TokenType.LessThanEq))){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = addition(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  // addition => multiplication ( ( "-" | "+" ) multiplication )*
  fun addition(tokens: LinkedList<Token>) : Expression{
    var expr = multiplication(tokens)
    while(tokens.matchAny( listOf(TokenType.Add, TokenType.Sub))){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = multiplication(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  // multiplication => unary ( ( "/" | "*" ) unary )*
  fun multiplication(tokens: LinkedList<Token>) : Expression{
    var expr = unary(tokens)
    while(tokens.matchAny(listOf(TokenType.Mul, TokenType.Div))){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = unary(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  // unary => ( "!" | "-" ) unary | primary
  fun unary(tokens: LinkedList<Token>) : Expression{
    if (tokens.matchAny(listOf(TokenType.Not, TokenType.Sub))) {
      val operator = tokens.removeFirst()
      val right = unary(tokens)
      return Expression.UnaryOperator(UnaryOperators.getOperator(operator), right)
    }

    return primary(tokens)
  }

  // primary => NUMBER | functionCall | "false" | "true" | "nil" | "(" expression ")"
  fun primary(tokens : LinkedList<Token>) : Expression{
    if(tokens.matchAny(listOf(TokenType.OpenParen, TokenType.Num, TokenType.Identifier, TokenType.True, TokenType.False))) {
      if (tokens.match(TokenType.Identifier)) {
        return functionCall(tokens)
      }

      val token = tokens.consumeToken()

      when (token.tokenType) {
        TokenType.Num -> return Expression.Num((token as Token.Num).value)
        TokenType.True -> return Expression.Bool(true)
        TokenType.False -> return Expression.Bool(false)
        else -> {
          val expr = expression(tokens)
          tokens.getNextToken(TokenType.CloseParen)
          return expr
        }
      }
    } else {
      if (tokens.isEmpty()){
        throw Parser.UnexpectedEndOfFile()
      } else {
        val token = tokens[0]
        throw Lexer.InvalidInputException("Invalid token in expression $token")
      }
    }

  }
  // functionCall => STRING ("(" (expression (",")*)* ")")?
  fun functionCall(tokens: LinkedList<Token>): Expression {
    val token = tokens.getIdentifier()
    if (tokens.match(TokenType.OpenParen)){
      tokens.consumeToken()
      val params = LinkedList<Expression>()
      while (!tokens.match(TokenType.CloseParen)){
        if (tokens.match(TokenType.Comma)){
          tokens.consumeToken()
        }
        params.add(expression(tokens))
      }
      tokens.getNextToken(TokenType.CloseParen)

      return Expression.FunctionCall(token, params)
    } else {
      return Expression.Identifier(token.name)
    }
  }
}