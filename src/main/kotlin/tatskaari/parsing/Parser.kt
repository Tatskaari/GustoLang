package tatskaari.parsing

import tatskaari.compatibility.*
import tatskaari.tokenising.Lexer
import tatskaari.tokenising.Token
import tatskaari.tokenising.TokenType
import kotlin.collections.ArrayList

class Parser(private var sourceTree: SourceTree) {
  constructor() : this(NoSourceTree)

  open class ParserException(val reason: String, val start: Token, val end: Token) : Exception()
  data class UnexpectedToken(val token: Token, private val expectedTokens: List<TokenType>)
    : ParserException("Unexpected token $token, expected one of $expectedTokens", token, token)
  class UnexpectedEndOfFile : Exception()
  object ParsingFailedException: Exception()


  private var isPanicMode = false
  val parserExceptions = ArrayList<ParserException>()

  fun parse(source: String): ArrayList<Statement>? {
    val tokens = Lexer.lex(source)
    val statements = loadSources(tokens)
    return if (parserExceptions.size == 0){
      try {
        statements.addAll(program(TokenList(tokens)))
        statements
      } catch (exception: ParsingFailedException){
        null
      } catch (exception: UnexpectedEndOfFile){
        parserExceptions.add(ParserException("Unexpected end of file", tokens.last(), tokens.last()))
        null
      }
    } else {
      null
    }
  }

  private fun loadSources(tokens: TokenList) : ArrayList<Statement>{
    val sources = ArrayList<String>()
    while(tokens.match(TokenType.Include)){
      tokens.getNextToken(TokenType.Include)
      val text = tokens.getNextToken(TokenType.TextLiteral) as Token.TextLiteral
      sources.add(text.text)
    }
    return sources.flatMapTo(ArrayList<Statement>()) {
      parse(sourceTree.getSource(it)) ?: ArrayList()
    }
  }

  // program => (statement)*
  private fun program(tokens: TokenList) : List<Statement> {
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
        TokenType.If -> iff(tokens)
        TokenType.While -> whilee(tokens)
        TokenType.OpenBlock -> codeBlock(tokens)
        TokenType.Function -> function(tokens)
        TokenType.Return -> returnn(tokens)
        TokenType.Value -> valueDeclaration(tokens)
        TokenType.Input -> input(tokens)
        TokenType.Output -> output(tokens)
        TokenType.Type -> typeDeclaration(tokens)
        // the statement is either as assignment or an expression statement
        TokenType.Identifier, TokenType.NumLiteral, TokenType.IntLiteral, TokenType.TextLiteral, TokenType.True, TokenType.False, TokenType.Sub, TokenType.Not -> {
          val expressionToken = tokens.consumeToken()
          if (tokens.matchAny(TokenType.AssignOp, TokenType.ListStart)) {
            tokens.addFirst(expressionToken)
            assignment(tokens)
          } else {
            tokens.addFirst(expressionToken)
            expressionStatement(tokens)
          }
        }
        else -> throw UnexpectedToken(token, listOf(TokenType.If, TokenType.While, TokenType.OpenBlock, TokenType.Function,
          TokenType.Return, TokenType.Input, TokenType.Output, TokenType.Identifier, TokenType.NumLiteral, TokenType.IntLiteral,
          TokenType.TextLiteral, TokenType.True, TokenType.False, TokenType.Sub, TokenType.Not))
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
              if (tokens.match(TokenType.CloseBlock)){
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

  private fun typeDeclaration(tokens: TokenList) : Statement.TypeDeclaration {
    val startToken = tokens.getNextToken(TokenType.Type)
    val typeName = tokens.getNextToken(TokenType.Identifier) as Token.Identifier
    tokens.getNextToken(TokenType.AssignOp)
    val members = ArrayList<Pair<Token, TypeNotation.VariantMember>>()
    members.add(typeMember(tokens))
    while (tokens.match(TokenType.Comma)){
      tokens.consumeToken()
      members.add(typeMember(tokens))
    }

    return Statement.TypeDeclaration(typeName, emptyList(), members.map { it.second }, startToken, members.last().first)
  }

  private fun typeMember(tokens: TokenList): Pair<Token, TypeNotation.VariantMember> {
    val name = tokens.getNextToken(TokenType.Constructor)
    val type = if (tokens.match(TokenType.Of)){
      tokens.consumeToken()
      typeNotation(tokens)
    } else {
      TypeNotation.Unit
    }

    return Pair(name, TypeNotation.VariantMember(name.tokenText, type))
  }

  private fun expressionStatement(tokens: TokenList): Statement {
    val expr = expression(tokens)
    return Statement.ExpressionStatement(expr, expr.startToken, expr.endToken)
  }

  // if => "if" expression codeBlock ("else" codeBlock)?
  private fun iff(tokens: TokenList): Statement {
    val startToken = tokens.getNextToken(TokenType.If)
    val condition = expression(tokens)
    val trueBodyStart = tokens.getNextToken(TokenType.Then)
    val trueStatements = ArrayList<Statement>()
    while (!tokens.matchAny(TokenType.CloseBlock, TokenType.Else)){
      trueStatements.add(statement(tokens))
    }
    return if (tokens.match(TokenType.Else)){
      val elseToken = tokens.consumeToken()
      val elseStatements = ArrayList<Statement>()
      while (!tokens.matchAny(TokenType.CloseBlock, TokenType.Else)){
        elseStatements.add(statement(tokens))
      }
      val lastToken = tokens.getNextToken(TokenType.CloseBlock)
      val trueBody = Statement.CodeBlock(trueStatements, trueBodyStart, elseToken)
      val elseBody = Statement.CodeBlock(elseStatements, elseToken, lastToken)
      Statement.IfElse(condition, trueBody, elseBody, startToken, lastToken)
    } else {
      val lastToken = tokens.getNextToken(TokenType.CloseBlock)
      Statement.If(condition, Statement.CodeBlock(trueStatements, trueBodyStart, lastToken), startToken, lastToken)
    }
  }

  // while => "while" "(" expression ")" codeBlock
  private fun whilee(tokens: TokenList): Statement.While {
    val startToken = tokens.getNextToken(TokenType.While)
    val condition = expression(tokens)
    val body = codeBlock(tokens)
    return Statement.While(condition, body, startToken, body.endToken)
  }

  // codeBlock => "{" (statement)* "}"
  private fun codeBlock(tokens: TokenList): Statement.CodeBlock {
    val startToken = tokens.getNextToken(TokenType.OpenBlock)
    val body = ArrayList<Statement>()
    while(!tokens.match(TokenType.CloseBlock)){
      body.add(statement(tokens))
    }
    val endToken = tokens.getNextToken(TokenType.CloseBlock)
    return Statement.CodeBlock(body, startToken, endToken)
  }

  // function => STRING "(" (STRING typeNotation(",")?)*  ")" codeBlock
  fun function(tokens: TokenList): Statement.FunctionDeclaration {
    val startToken = tokens.getNextToken(TokenType.Function)
    val name = tokens.getIdentifier()
    val (params, paramTypes) = functionParams(tokens)
    val returnType = functionReturnType(tokens)
    val body = codeBlock(tokens)
    val function = Expression.Function(returnType, params, paramTypes, body, startToken, body.endToken)

    return Statement.FunctionDeclaration(name, function, startToken, body.endToken)
  }

  private fun listType(type: TypeNotation, tokens: TokenList): TypeNotation{
    return if (tokens.match(TokenType.List)){
      tokens.consumeToken()
      TypeNotation.ListOf(type)
    } else {
      return type
    }
  }

  // ("(" (typeNotation)? ",typeNotation"* ")" ("->" typeNotation)?)
  private fun functionType(tokens: TokenList): TypeNotation {
    val params = ArrayList<TypeNotation>()
    while(!tokens.match(TokenType.CloseParen)){
      params.add(typeNotation(tokens))
      if (tokens.match(TokenType.Comma)){
        tokens.consumeToken()
      }
    }
    tokens.consumeToken()

    return if (tokens.match(TokenType.RightArrow)) {
      tokens.getNextToken(TokenType.RightArrow)
      TypeNotation.Function(params, typeNotation(tokens))
    } else {
      TypeNotation.Tuple(params)
    }
  }


  // primitiveType => "unit" | "list" | ("number" | "integer" | "boolean" | "text") ("list")? | functionType
  private fun primitiveType(tokens: TokenList): TypeNotation {
    val token = tokens.consumeToken()
    return when(token.tokenType){
      TokenType.List -> TypeNotation.ListOf(TypeNotation.UnknownType)
      TokenType.Identifier -> listType(TypeNotation.Atomic(token.tokenText), tokens)
      TokenType.Constructor -> listType(TypeNotation.Atomic(token.tokenText), tokens)
      TokenType.OpenParen -> listType(functionType(tokens), tokens)
      else -> throw UnexpectedToken(token, listOf(TokenType.List, TokenType.Identifier, TokenType.OpenParen))
    }
  }

  // typeNotation => ( primitiveType (list)?) | functionType
  private fun typeNotation(tokens: TokenList): TypeNotation {
    return primitiveType(tokens)
  }

  // return => "return" expression
  private fun returnn(tokens: TokenList): Statement.Return {
    val startToken = tokens.getNextToken(TokenType.Return)
    val expression = expression(tokens)
    return Statement.Return(expression, startToken, expression.endToken)
  }

  // valueDeclaration => "var" STRING : typeNotation ":=" expression
  private fun valueDeclaration(tokens: TokenList): Statement {
    val startToken = tokens.getNextToken(TokenType.Value)
    val pattern = pattern(tokens)
    tokens.getNextToken(TokenType.AssignOp)
    val expression = expression(tokens)
    return Statement.ValDeclaration(pattern, expression, startToken, expression.endToken)
  }

  fun pattern(tokens: TokenList) : AssignmentPattern {
    return when(tokens.lookAhead().tokenType){
      TokenType.Identifier -> variablePattern(tokens)
      TokenType.Constructor -> constructorPattern(tokens)
      TokenType.OpenParen -> tuplePattern(tokens)
      else -> throw UnexpectedToken(tokens.lookAhead(), listOf(TokenType.Identifier, TokenType.Constructor, TokenType.OpenParen))
    }
  }

  private fun variablePattern(tokens: TokenList) : AssignmentPattern.Variable {
    val identifier = tokens.getIdentifier()

    val valType = if (tokens.match(TokenType.Colon)){
      tokens.getNextToken(TokenType.Colon)
      typeNotation(tokens)
    } else {
      TypeNotation.UnknownType
    }

    return AssignmentPattern.Variable(identifier, valType)
  }

  private fun constructorPattern(tokens: TokenList) : AssignmentPattern.Constructor {
    val name = tokens.getNextToken(TokenType.Constructor) as Token.Constructor
    val pattern = if(tokens.match(TokenType.Comma)){
      AssignmentPattern.Unit
    } else {
      pattern(tokens)
    }
    // If there's only 1 parameter to the tuple then it should be treated as a normal value pattern
    val unwrappedPattern = if (pattern is AssignmentPattern.Tuple && pattern.identifiers.size == 1) {
      pattern.identifiers.first()
    } else {
      pattern
    }
    return AssignmentPattern.Constructor(name, unwrappedPattern)
  }

  private fun tuplePattern(tokens: TokenList) : AssignmentPattern.Tuple {
    tokens.getNextToken(TokenType.OpenParen)
    val patterns = ArrayList<AssignmentPattern>()
    while(!tokens.match(TokenType.CloseParen)){
      patterns.add(pattern(tokens))
      if (tokens.match(TokenType.Comma)) tokens.getNextToken(TokenType.Comma)
    }
    tokens.getNextToken(TokenType.CloseParen)
    return AssignmentPattern.Tuple(patterns)

  }

  // assignment => STRING ":=" expression | STRING "[" expression "]" ":=" expression
  private fun assignment(tokens: TokenList): Statement {
    val identifier = tokens.getIdentifier()
    return when {
      tokens.match(TokenType.AssignOp) -> {
        tokens.getNextToken(TokenType.AssignOp)
        val expression = expression(tokens)
        Statement.Assignment(identifier, expression, identifier, expression.endToken)
      }
      tokens.match(TokenType.ListStart) -> {
        tokens.getNextToken(TokenType.ListStart)
        val indexExpression = expression(tokens)
        tokens.getNextToken(TokenType.ListEnd)
        tokens.getNextToken(TokenType.AssignOp)
        val expression = expression(tokens)
        Statement.ListAssignment(identifier, indexExpression, expression, identifier, expression.endToken)
      }
      else -> throw UnexpectedToken(tokens.lookAhead(), listOf(TokenType.AssignOp, TokenType.ListStart))
    }
  }

  // input => "input" STRING
  fun input(tokens: TokenList): Statement.Input {
    val startToken = tokens.getNextToken(TokenType.Input)
    val identifier = tokens.getIdentifier()
    return Statement.Input(identifier,startToken, identifier)
  }

  // output => "output" expression
  fun output(tokens: TokenList): Statement.Output {
    val startToken = tokens.getNextToken(TokenType.Output)
    val expression = expression(tokens)
    return Statement.Output(expression, startToken, expression.endToken)
  }

  // expression => logical
  fun expression(tokens: TokenList) : Expression{
    return logical(tokens)
  }

  // logical => equality ( ( "and" | "or" ) equality )*
  private fun logical(tokens: TokenList) : Expression{
    var expr = equality(tokens)
    while(tokens.matchAny(TokenType.Or, TokenType.And)){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = equality(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs, expr.startToken, rhs.endToken)
    }
    return expr
  }

  // equality => comparison ( ( "!=" | "==" ) comparison )*
  private fun equality(tokens: TokenList) : Expression{
    var expr = comparison(tokens)
    while(tokens.matchAny(TokenType.Equality, TokenType.NotEquality)){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = comparison(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs, expr.startToken, rhs.endToken)
    }
    return expr
  }

  // comparison => addition ( ( ">" | ">=" | "<" | "<=" ) addition )*
  private fun comparison(tokens: TokenList) : Expression{
    var expr = addition(tokens)
    while(tokens.matchAny(TokenType.GreaterThan, TokenType.GreaterThanEq, TokenType.LessThan, TokenType.LessThanEq)){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = addition(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs, expr.startToken, rhs.endToken)
    }
    return expr
  }

  // addition => multiplication ( ( "-" | "+" ) multiplication )*
  private fun addition(tokens: TokenList) : Expression{
    var expr = multiplication(tokens)
    while(tokens.matchAny(TokenType.Add, TokenType.Sub)){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = multiplication(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs, expr.startToken, rhs.endToken)
    }
    return expr
  }

  // multiplication => unary ( ( "/" | "*" ) unary )*
  private fun multiplication(tokens: TokenList) : Expression{
    var expr = unary(tokens)
    while(tokens.matchAny(TokenType.Mul, TokenType.Div)){
      val operator = BinaryOperators.getOperator(tokens.removeFirst())
      val rhs = unary(tokens)
      expr = Expression.BinaryOperator(operator, expr, rhs, expr.startToken, rhs.endToken)
    }

    return expr
  }

  // unary => ( "!" | "-" ) unary | primary ("(" expressionList ")" | "[" expression "]")
  private fun unary(tokens: TokenList) : Expression{
    if (tokens.matchAny(TokenType.Not, TokenType.Sub)) {
      val operator = tokens.removeFirst()
      val right = unary(tokens)
      return Expression.UnaryOperator(UnaryOperators.getOperator(operator), right, operator, right.endToken)
    }

    var expr = primary(tokens)

    while (tokens.match(TokenType.Dot)){
      tokens.getNextToken(TokenType.Dot)
      val function = primary(tokens)
      tokens.getNextToken(TokenType.OpenParen)
      val params = ArrayList(expressionList(tokens, TokenType.CloseParen))
      params.add(0, expr)
      val endToken = tokens.getNextToken(TokenType.CloseParen)
      expr = Expression.FunctionCall(function, params, expr.startToken, endToken)
    }

    while(tokens.matchAny(TokenType.OpenParen, TokenType.ListStart)){
      when {
        tokens.match(TokenType.OpenParen) -> {
          tokens.getNextToken(TokenType.OpenParen)
          val params = expressionList(tokens, TokenType.CloseParen)
          val endToken = tokens.getNextToken(TokenType.CloseParen)
          expr = Expression.FunctionCall(expr, params, expr.startToken, endToken)
        }
        tokens.match(TokenType.ListStart) -> {
          tokens.getNextToken(TokenType.ListStart)
          val indexExpression = expression(tokens)
          val endToken = tokens.getNextToken(TokenType.ListEnd)
          expr = Expression.ListAccess(expr, indexExpression, expr.startToken, endToken)
        }
      }
    }

    return expr
  }

  // expressionList =  (expression (",")*)*
  private fun expressionList(tokens: TokenList, listEndTokenType: TokenType): List<Expression> {
    val list = ArrayList<Expression>()
    while (!tokens.match(listEndTokenType)){
      if (tokens.match(TokenType.Comma)){
        tokens.consumeToken()
      }
      list.add(expression(tokens))
    }
    return list
  }

  // primary => NUMBER | STRING | "false" | "true" | "(" expression ")" | listDeclaration
  private fun primary(tokens : TokenList) : Expression{
    val expectedTokens = listOf(TokenType.OpenParen, TokenType.IntLiteral, TokenType.Function,
      TokenType.Identifier, TokenType.True, TokenType.False, TokenType.ListStart)

    val token = tokens.consumeToken()
    when (token.tokenType) {
      TokenType.IntLiteral -> return Expression.IntLiteral((token as Token.IntLiteral).value, token, token)
      TokenType.NumLiteral -> return Expression.NumLiteral((token as Token.NumLiteral).value, token, token)
      TokenType.True -> return Expression.BooleanLiteral(true, token, token)
      TokenType.False -> return Expression.BooleanLiteral(false, token, token)
      TokenType.TextLiteral -> return Expression.TextLiteral((token as Token.TextLiteral).text, token, token)
      TokenType.Match -> {
        tokens.addFirst(token)
        return match(tokens)
      }
      TokenType.Function -> {
        tokens.addFirst(token)
        return anonymousFunction(tokens)
      }
      TokenType.OpenParen -> {
        tokens.addFirst(token)
        return parenExpression(tokens)
      }
      TokenType.ListStart -> {
        tokens.addFirst(token)
        return listDeclaration(tokens)
      }
      TokenType.Identifier -> return Expression.Identifier(token.tokenText, token, token)
      TokenType.Constructor -> {
        tokens.addFirst(token)
        return constructorCall(tokens)
      }
      else -> throw UnexpectedToken(token, expectedTokens)
    }
  }

  private fun match(tokens: TokenList) : Expression.Match {
    val startToken = tokens.getNextToken(TokenType.Match)
    val expression = expression(tokens)
    tokens.getNextToken(TokenType.With)
    val matchBranches  = ArrayList<MatchBranch>()
    while (!tokens.matchAny(TokenType.Else, TokenType.CloseBlock)) {
      val start = tokens.lookAhead()
      val pattern = pattern(tokens)
      tokens.getNextToken(TokenType.RightArrow)
      val statement = statement(tokens)
      matchBranches.add(MatchBranch(pattern, statement, start, statement.endToken))
    }
    return if (tokens.match(TokenType.CloseBlock)) {
      Expression.Match(matchBranches, expression, ElseMatchBranch.NoElseBranch, startToken, tokens.consumeToken())
    } else {
      tokens.getNextToken(TokenType.Else)
      tokens.getNextToken(TokenType.RightArrow)
      val statement = statement(tokens)
      val elseBranch = ElseMatchBranch.ElseBranch(statement)
      Expression.Match(matchBranches, expression, elseBranch, startToken, tokens.getNextToken(TokenType.CloseBlock))
    }
  }

  private fun constructorCall(tokens: TokenList) : Expression.ConstructorCall {
    val startToken = tokens.getNextToken(TokenType.Constructor)
    return if (tokens.match(TokenType.OpenParen)){
      val expr = parenExpression(tokens)
      Expression.ConstructorCall(startToken.tokenText, expr,  startToken, expr.endToken)

    } else {
      Expression.ConstructorCall(startToken.tokenText, null,  startToken, startToken)

    }
  }

  private fun parenExpression(tokens: TokenList) : Expression{
    val startToken = tokens.getNextToken(TokenType.OpenParen)
    val exprs = expressionList(tokens, TokenType.CloseParen)
    val endToken = tokens.getNextToken(TokenType.CloseParen)
    return if (exprs.size == 1){
      exprs.first()
    } else {
      Expression.Tuple(exprs, startToken, endToken)
    }
  }

  // anonymousFunction => "function" "(" (STRING typeNotation(",")?)*  ")" codeBlock
  private fun anonymousFunction(tokens: TokenList): Expression.Function {
    val firstToken = tokens.getNextToken(TokenType.Function)
    val (params, paramTypes) = functionParams(tokens)
    val returnType = functionReturnType(tokens)
    val body = codeBlock(tokens)
    return Expression.Function(returnType, params, paramTypes, body, firstToken, body.endToken)
  }

  private fun functionParams(tokens: TokenList): Pair<List<Token.Identifier>, Map<Token.Identifier, TypeNotation>> {
    tokens.getNextToken(TokenType.OpenParen)
    val paramTypes = HashMap<Token.Identifier, TypeNotation>()
    val params = ArrayList<Token.Identifier>()
    while (!tokens.match(TokenType.CloseParen)){
      val paramIdentifier = tokens.getNextToken(TokenType.Identifier) as Token.Identifier
      tokens.getNextToken(TokenType.Colon)
      val paramType: TypeNotation = typeNotation(tokens)

      params.add(paramIdentifier)
      paramTypes.put(paramIdentifier, paramType)

      if(tokens.match(TokenType.Comma)){
        tokens.consumeToken()
      }
    }
    tokens.getNextToken(TokenType.CloseParen)

    return Pair(params, paramTypes)
  }

  // functionReturnType => NOTHING | ":" typeNotation
  private fun functionReturnType(tokens: TokenList): TypeNotation {
    return if (tokens.match(TokenType.Colon)) {
      tokens.consumeToken()
      typeNotation(tokens)
    } else {
      TypeNotation.Unit
    }
  }

  // listDeclaration = "[" (expression (",")*)* "]"
  private fun listDeclaration(tokens: TokenList): Expression.ListDeclaration {
    val startToken = tokens.getNextToken(TokenType.ListStart)
    val listItems = expressionList(tokens, TokenType.ListEnd)
    val endToken = tokens.getNextToken(TokenType.ListEnd)
    return Expression.ListDeclaration(listItems, startToken, endToken)
  }
}