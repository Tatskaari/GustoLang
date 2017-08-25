package tatskaari.parsing

import tatskaari.compatibility.*
import tatskaari.tokenising.Lexer
import tatskaari.tokenising.Token
import tatskaari.tokenising.TokenType
import tatskaari.tokenising.TokenType.*

class Parser {
  open class ParserException(val reason: String) : Exception()
  data class UnexpectedToken(val token: Token?, private val expectedTokens: List<tatskaari.tokenising.TokenType>)
    : ParserException("Unexpected token $token, expected one of $expectedTokens")
  object UnexpectedEndOfFile: Exception()
  object ParsingFailedException: Exception()

  private var isPanicMode = false
  val parserExceptions = ArrayList<ParserException>()

  fun parse(source: String): List<Statement>? {
    return try {
      program(Lexer.lex(source))
    } catch (exception: ParsingFailedException){
      null
    } catch (exception: UnexpectedEndOfFile){
      parserExceptions.add(ParserException("Unexpected end of file"))
      null
    }
  }

  // program => (statement)*
  fun program(tokens: TokenList) : List<Statement> {
    val statements = ArrayList<Statement>()
    while(tokens.isNotEmpty()){
      statements.add(statement(tokens))
    }

    return statements
  }

  // statement => if | while | codeBlock | function | return | valueDeclaration | input | output | assignment;
  private fun statement(tokens: TokenList): Statement {
    try{
      val token = tokens.lookAhead()
      return when(token.tokenType){
        If -> iff(tokens)
        While -> whilee(tokens)
        OpenBlock -> codeBlock(tokens)
        Function -> function(tokens)
        Return -> returnn(tokens)
        NumberVal, IntegerVal, BooleanVal, ListVal, TextVal -> valueDeclaration(token.tokenType, tokens)
        Input -> input(tokens)
        Output -> output(tokens)
        Identifier -> assignment(tokens)
        else -> throw UnexpectedToken(token, listOf(If, While, OpenBlock, Function, Return, NumberVal, IntegerVal, BooleanVal, ListVal, TextVal, Input, Output, Identifier))
      }
    } catch (rootException: ParserException){
      // Attempt to parse the next tokens as a statement until it works then parse the rest of the program to get any further errors
      if(!isPanicMode){
        isPanicMode = true
        parserExceptions.add(rootException)
        while(isPanicMode && tokens.isNotEmpty()){
          tokens.consumeToken()
          try{
            statement(tokens)
            isPanicMode = false
            while(tokens.isNotEmpty()){
              if (tokens.match(CloseBlock)){
                tokens.consumeToken()
              }
              statement(tokens)
            }
          } catch (cascadingException: ParsingFailedException){
            // Ignore
          } catch (cascadingException: UnexpectedEndOfFile){

          }
        }
      }
    }

    throw ParsingFailedException
  }

  // if => "if" expression codeBlock ("else" codeBlock)?
  private fun iff(tokens: TokenList): Statement {
    tokens.getNextToken(If)
    val condition = expression(tokens)
    tokens.getNextToken(Then)
    val trueBody = ArrayList<Statement>()
    while (!tokens.matchAny(listOf(TokenType.CloseBlock, TokenType.Else))){
      trueBody.add(statement(tokens))
    }
    return if (tokens.match(Else)){
      tokens.consumeToken()
      val elseBody = ArrayList<Statement>()
      while (!tokens.matchAny(listOf(TokenType.CloseBlock, TokenType.Else))){
        elseBody.add(statement(tokens))
      }
      tokens.getNextToken(CloseBlock)
      Statement.IfElse(condition, trueBody, elseBody)
    } else {
      tokens.getNextToken(CloseBlock)
      Statement.If(condition, trueBody)
    }
  }

  // while => "while" "(" expression ")" codeBlock
  private fun whilee(tokens: TokenList): Statement.While {
    tokens.getNextToken(While)
    val condition = expression(tokens)
    val body = codeBlock(tokens)
    return Statement.While(condition, body)
  }

  // codeBlock => "{" (statement)* "}"
  private fun codeBlock(tokens: TokenList): Statement.CodeBlock {
    tokens.getNextToken(OpenBlock)
    val body = ArrayList<Statement>()
    while(!tokens.match(CloseBlock)){
      body.add(statement(tokens))
    }
    tokens.getNextToken(CloseBlock)
    return Statement.CodeBlock(body)
  }

  // function => STRING "(" (STRING(",")?)*  ")" codeBlock
  fun function(tokens: TokenList): Statement.Function {
    tokens.getNextToken(Function)
    val name = tokens.getIdentifier()
    tokens.getNextToken(OpenParen)
    val parameters = ArrayList<Token.Identifier>()
    while (!tokens.match(CloseParen)){
      val token = tokens.consumeToken()
      when(token.tokenType){
        Comma -> {}
        Identifier -> parameters.add(token as Token.Identifier)
        else -> throw UnexpectedToken(token, listOf(Comma, Identifier))
      }
    }

    tokens.getNextToken(CloseParen)
    val body = codeBlock(tokens)

    return Statement.Function(name, parameters, body)
  }

  // return => "return" expression
  private fun returnn(tokens: TokenList): Statement.Return {
    tokens.getNextToken(Return)
    val expression = expression(tokens)
    return Statement.Return(expression)
  }

  // valueDeclaration => "val" STRING ":=" expression
  private fun valueDeclaration(valType: TokenType, tokens: TokenList): Statement {
    val token = tokens.getNextToken(valType)
    val identifier = tokens.getIdentifier()
    tokens.getNextToken(AssignOp)
    val expression = expression(tokens)
    return when (valType) {
      NumberVal -> Statement.NumberDeclaration(identifier, expression)
      IntegerVal -> Statement.IntegerDeclaration(identifier, expression)
      BooleanVal -> Statement.BooleanDeclaration(identifier, expression)
      TextVal -> Statement.TextDeclaration(identifier, expression)
      ListVal -> Statement.ListDeclaration(identifier, expression)
      else -> throw UnexpectedToken(token, listOf(NumberVal, IntegerVal, BooleanVal, TextVal, ListVal))
    }
  }

  // assignment => STRING ":=" expression | STRING "[" expression "]" ":=" expression
  private fun assignment(tokens: TokenList): Statement {
    val identifier = tokens.getIdentifier()
    return if (tokens.match(AssignOp)){
      tokens.getNextToken(AssignOp)
      val expression = expression(tokens)
      Statement.Assignment(identifier, expression)
    } else {
      tokens.getNextToken(ListStart)
      val indexExpression = expression(tokens)
      tokens.getNextToken(ListEnd)
      tokens.getNextToken(AssignOp)
      val expression = expression(tokens)
      Statement.ListAssignment(identifier, indexExpression, expression)
    }
  }

  // input => "input" STRING
  fun input(tokens: TokenList): Statement.Input {
    tokens.getNextToken(Input)
    val identifier = tokens.getIdentifier()
    return Statement.Input(identifier)
  }

  // output => "output" expression
  fun output(tokens: TokenList): Statement.Output {
    tokens.getNextToken(Output)
    val expression = expression(tokens)
    return Statement.Output(expression)
  }

  // expression => logical
  fun expression(tokens: TokenList) : Expression{
    return logical(tokens)
  }

  // logical => equality ( ( "and" | "or" ) equality )*
  private fun logical(tokens: TokenList) : Expression{
    var expr = equality(tokens)
    while(tokens.matchAny( listOf(Or, And))){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = equality(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  // equality => comparison ( ( "!=" | "==" ) comparison )*
  private fun equality(tokens: TokenList) : Expression{
    var expr = comparison(tokens)
    while(tokens.matchAny(listOf(Equality, NotEquality))){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = comparison(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  // comparison => addition ( ( ">" | ">=" | "<" | "<=" ) addition )*
  private fun comparison(tokens: TokenList) : Expression{
    var expr = addition(tokens)
    while(tokens.matchAny(listOf(GreaterThan, GreaterThanEq, LessThan, LessThanEq))){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = addition(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  // addition => multiplication ( ( "-" | "+" ) multiplication )*
  private fun addition(tokens: TokenList) : Expression{
    var expr = multiplication(tokens)
    while(tokens.matchAny( listOf(Add, Sub))){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = multiplication(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  // multiplication => unary ( ( "/" | "*" ) unary )*
  private fun multiplication(tokens: TokenList) : Expression{
    var expr = unary(tokens)
    while(tokens.matchAny(listOf(Mul, Div))){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = unary(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs)
    }
    return expr
  }

  // unary => ( "!" | "-" ) unary | primary ("(" expressionList ")" | "[" expression "]")
  private fun unary(tokens: TokenList) : Expression{
    if (tokens.matchAny(listOf(Not, Sub))) {
      val operator = tokens.removeFirst()
      val right = unary(tokens)
      return Expression.UnaryOperator(UnaryOperators.getOperator(operator), right)
    }

    var expr = primary(tokens)

    while(tokens.matchAny(listOf(OpenParen, ListStart))){
      if(tokens.match(OpenParen)){
        tokens.getNextToken(OpenParen)
        val params = expressionList(tokens, CloseParen)
        tokens.getNextToken(CloseParen)
        expr = Expression.FunctionCall(expr, params)
      } else if(tokens.match(ListStart)){
        tokens.getNextToken(ListStart)
        val indexExpression = expression(tokens)
        tokens.getNextToken(ListEnd)
        expr = Expression.ListAccess(expr, indexExpression)
      }
    }

    return expr
  }

  // expressionList =  (expression (",")*)*
  private fun expressionList(tokens: TokenList, listEndTokenType: TokenType): List<Expression>{
    val list = ArrayList<Expression>()
    while (!tokens.match(listEndTokenType)){
      if (tokens.match(Comma)){
        tokens.consumeToken()
      }
      list.add(expression(tokens))
    }
    return list
  }

  // primary => NUMBER | STRING | "false" | "true" | "(" expression ")" | listDeclaration
  private fun primary(tokens : TokenList) : Expression{
    val expectedTokens = listOf(OpenParen, IntLiteral, Identifier, True, False, ListStart)

    val token = tokens.consumeToken()
    when (token.tokenType) {
      IntLiteral -> return Expression.IntLiteral((token as Token.IntLiteral).value)
      NumLiteral -> return Expression.NumLiteral((token as Token.NumLiteral).value)
      True -> return Expression.Bool(true)
      False -> return Expression.Bool(false)
      TextLiteral -> return Expression.Text((token as Token.TextLiteral).text)
      OpenParen -> {
        val expr = expression(tokens)
        tokens.getNextToken(CloseParen)
        return expr
      }
      ListStart -> {
        tokens.addFirst(token)
        return listDeclaration(tokens)
      }
      Identifier -> return Expression.Identifier(token.tokenText)
      else -> throw UnexpectedToken(token, expectedTokens)
    }

  }

  // listDeclaration = "[" (expression (",")*)* "]"
  private fun listDeclaration(tokens: TokenList): Expression.ListDeclaration {
    tokens.getNextToken(ListStart)
    val listItems = expressionList(tokens, ListEnd)
    tokens.getNextToken(ListEnd)

    return Expression.ListDeclaration(listItems)
  }
}