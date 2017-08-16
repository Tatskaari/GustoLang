package tatskaari.parsing

import tatskaari.tokenising.IToken
import tatskaari.tokenising.KeyWords
import tatskaari.tokenising.Lexer
import tatskaari.tokenising.Token
import java.util.*

object Parser {
  class UnexpectedEndOfFile : RuntimeException("Unexpected end of file")

  fun parse(source: String): List<Statement> {
    return program(Lexer.lex(source))
  }

  // program => (statement)*
  fun program(tokens: LinkedList<IToken>) : List<Statement> {
    val statements = LinkedList<Statement>()
    while(tokens.isNotEmpty()){
      statements.add(statement(tokens))
    }

    return statements
  }

  // statement => if | while | codeBlock | function | return | valueDeclaration | input | output | assignment;
  fun statement(tokens: LinkedList<IToken>): Statement {
    val token = tokens.lookAhead()
    when(token){
      KeyWords.If -> return iff(tokens)
      KeyWords.While -> return whilee(tokens)
      KeyWords.OpenBlock -> return codeBlock(tokens)
      KeyWords.Function -> return function(tokens)
      KeyWords.Return -> return returnn(tokens)
      KeyWords.Val -> return valueDeclaration(tokens)
      KeyWords.Input -> return input(tokens)
      KeyWords.Output -> return output(tokens)
      is Token.Identifier -> return assignment(tokens)
    }
    throw Lexer.InvalidInputException("Unexpected token $token, expected statement")
  }

  // if => "if" expression codeBlock ("else" codeBlock)?
  fun iff(tokens: LinkedList<IToken>): Statement {
    tokens.getNextToken(KeyWords.If)
    val condition = expression(tokens)
    val trueBody = codeBlock(tokens)
    if (tokens.match(KeyWords.Else)){
      tokens.consumeToken()
      val elseBody = codeBlock(tokens)
      return Statement.IfElse(condition, trueBody, elseBody)
    } else {
      return Statement.If(condition, trueBody)
    }
  }

  // while => "while" "(" expression ")" codeBlock
  fun whilee(tokens: LinkedList<IToken>): Statement.While {
    tokens.getNextToken(KeyWords.While)
    val condition = expression(tokens)
    val body = codeBlock(tokens)
    return Statement.While(condition, body)
  }

  // codeBlock => "{" (statement)* "}"
  fun codeBlock(tokens: LinkedList<IToken>): Statement.CodeBlock {
    tokens.getNextToken(KeyWords.OpenBlock)
    val body = LinkedList<Statement>()
    while(!tokens.match(KeyWords.CloseBlock)){
      body.add(statement(tokens))
    }
    tokens.getNextToken(KeyWords.CloseBlock)
    return Statement.CodeBlock(body)
  }

  // function => STRING "(" (STRING)* (",")? ")" codeBlock
  fun function(tokens: LinkedList<IToken>): Statement.Function {
    tokens.getNextToken(KeyWords.Function)
    val name = tokens.getNextToken(Token.Identifier("someIdentifier"))
    tokens.getNextToken(KeyWords.OpenParen)
    val parameters = LinkedList<Token.Identifier>()
    while (!tokens.match(KeyWords.CloseParen)){
      val token = tokens.consumeToken()
      when(token){
        KeyWords.Comma -> {}
        is Token.Identifier -> parameters.add(token)
      }
    }
    tokens.getNextToken(KeyWords.CloseParen)
    val body = codeBlock(tokens)

    return Statement.Function(name, parameters, body)
  }

  // return => "return" expression
  fun returnn(tokens: LinkedList<IToken>): Statement.Return {
    tokens.getNextToken(KeyWords.Return)
    val expression = expression(tokens)
    return Statement.Return(expression)
  }

  // valueDeclaration => "val" STRING ":=" expression
  fun valueDeclaration(tokens: LinkedList<IToken>): Statement.ValDeclaration {
    tokens.getNextToken(KeyWords.Val)
    val identifier = tokens.getNextToken(Token.Identifier("someValue"))
    tokens.getNextToken(KeyWords.AssignOp)
    val expression = expression(tokens)
    return Statement.ValDeclaration(identifier, expression)
  }

  // assignment => STRING ":=" expression
  fun assignment(tokens: LinkedList<IToken>): Statement.Assignment {
    val identifier = tokens.getNextToken(Token.Identifier("someValue"))
    tokens.getNextToken(KeyWords.AssignOp)
    val expression = expression(tokens)
    return Statement.Assignment(identifier, expression)
  }

  // input => "input" STRING
  fun input(tokens: LinkedList<IToken>): Statement.Input {
    tokens.getNextToken(KeyWords.Input)
    val identifier = tokens.getNextToken(Token.Identifier("someValue"))
    return Statement.Input(identifier)
  }

  // output => "output" expression
  fun output(tokens: LinkedList<IToken>): Statement.Output {
    tokens.getNextToken(KeyWords.Output)
    val expression = expression(tokens)
    return Statement.Output(expression)
  }

  // expression => logical
  fun expression(tokens: LinkedList<IToken>) : Expression{
    return logical(tokens)
  }

  // logical => equality ( ( "and" | "or" ) equality )*
  fun logical(tokens: LinkedList<IToken>) : Expression{
    var expr = equality(tokens)
    while(tokens.matchAny( listOf<IToken>(KeyWords.Or, KeyWords.And))){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = equality(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  // equality => comparison ( ( "!=" | "==" ) comparison )*
  fun equality(tokens: LinkedList<IToken>) : Expression{
    var expr = comparison(tokens)
    while(tokens.matchAny(listOf<IToken>(KeyWords.Equality, KeyWords.NotEquality))){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = comparison(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  // comparison => addition ( ( ">" | ">=" | "<" | "<=" ) addition )*
  fun comparison(tokens: LinkedList<IToken>) : Expression{
    var expr = addition(tokens)
    while(tokens.matchAny(listOf<IToken>(KeyWords.GreaterThan, KeyWords.GreaterThanEq, KeyWords.LessThan, KeyWords.LessThanEq))){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = addition(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  // addition => multiplication ( ( "-" | "+" ) multiplication )*
  fun addition(tokens: LinkedList<IToken>) : Expression{
    var expr = multiplication(tokens)
    while(tokens.matchAny( listOf<IToken>(KeyWords.Add, KeyWords.Sub))){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = multiplication(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  // multiplication => unary ( ( "/" | "*" ) unary )*
  fun multiplication(tokens: LinkedList<IToken>) : Expression{
    var expr = unary(tokens)
    while(tokens.matchAny(listOf<IToken>(KeyWords.Mul, KeyWords.Div))){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = unary(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  // unary => ( "!" | "-" ) unary | primary
  fun unary(tokens: LinkedList<IToken>) : Expression{
    if (tokens.matchAny(listOf<IToken>(KeyWords.Not, KeyWords.Sub))) {
      val operator = tokens.removeFirst()
      val right = unary(tokens)
      return Expression.UnaryOperator(UnaryOperators.getOperator(operator), right)
    }

    return primary(tokens)
  }

  // primary => NUMBER | functionCall | "false" | "true" | "nil" | "(" expression ")"
  fun primary(tokens : LinkedList<IToken>) : Expression{
    if(tokens.matchAny(listOf(KeyWords.OpenParen, Token.Num(0), Token.Identifier("someVar"), KeyWords.True, KeyWords.False))) {
      if (tokens.match(Token.Identifier("someVar"))) {
        return functionCall(tokens)
      }

      val token = tokens.consumeToken()

      when (token) {
        is Token.Num -> return Expression.Num(token.value)
        KeyWords.True -> return Expression.Bool(true)
        KeyWords.False -> return Expression.Bool(false)
      }

      val expr = expression(tokens)
      tokens.getNextToken(KeyWords.CloseParen)
      return expr
    } else {
      if (tokens.isEmpty()){
        throw Parser.UnexpectedEndOfFile()
      } else {
        val token = tokens[0]
        throw Lexer.InvalidInputException("Invalid token in expression $token")
      }
    }

  }
  // functionCall => STRING ("(" (expression (",")*)* ")")*
  fun functionCall(tokens: LinkedList<IToken>): Expression {
    val token = tokens.getNextToken(Token.Identifier("someVariable"))
    if (tokens.match(KeyWords.OpenParen)){
      tokens.consumeToken()
      val params = LinkedList<Expression>()
      while (!tokens.match(KeyWords.CloseParen)){
        if (tokens.match(KeyWords.Comma)){
          tokens.consumeToken()
        }
        params.add(expression(tokens))
      }
      tokens.getNextToken(KeyWords.CloseParen)

      return Expression.FunctionCall(token, params)
    } else {
      return Expression.Identifier(token.name)
    }
  }
}