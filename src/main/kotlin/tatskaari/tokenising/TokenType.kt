package tatskaari.tokenising

import tatskaari.tokenising.Matcher.*

enum class TokenType(var matcher: Matcher, var tokenConstructor: (TokenType, String, Int, Int) -> Token) {
  Add(KeywordMatcher("+"),Token::Keyword),
  Sub(KeywordMatcher("-"), Token::Keyword),
  Mul(KeywordMatcher("*"), Token::Keyword),
  Div(KeywordMatcher("/"), Token::Keyword),
  LessThan(KeywordMatcher("<"), Token::Keyword),
  GreaterThan(KeywordMatcher(">"), Token::Keyword),
  LessThanEq(KeywordMatcher("<="), Token::Keyword),
  GreaterThanEq(KeywordMatcher(">="), Token::Keyword),
  And(KeywordMatcher("and"), Token::Keyword),
  Function(KeywordMatcher("function"), Token::Keyword),
  Return(KeywordMatcher("return"), Token::Keyword),
  Comma(KeywordMatcher(","), Token::Keyword),
  Or(KeywordMatcher("or"), Token::Keyword),
  OpenBlock(KeywordMatcher("do"), Token::Keyword),
  CloseBlock(KeywordMatcher("end"), Token::Keyword),
  ListStart(KeywordMatcher("["), Token::Keyword),
  ListEnd(KeywordMatcher("]"), Token::Keyword),
  Val(KeywordMatcher("val"), Token::Keyword),
  AssignOp(KeywordMatcher(":="), Token::Keyword),
  Not(KeywordMatcher("!"), Token::Keyword),
  Equality(KeywordMatcher("="), Token::Keyword),
  NotEquality(KeywordMatcher("!="), Token::Keyword),
  If(KeywordMatcher("if"), Token::Keyword),
  Else(KeywordMatcher("else"), Token::Keyword),
  True(KeywordMatcher("true"), Token::Keyword),
  False(KeywordMatcher("false"), Token::Keyword),
  While(KeywordMatcher("while"), Token::Keyword),
  OpenParen(KeywordMatcher("("), Token::Keyword),
  CloseParen(KeywordMatcher(")"), Token::Keyword),
  Input(KeywordMatcher("input"), Token::Keyword),
  Output(KeywordMatcher("output"), Token::Keyword),
  Comment(CommentMatcher, Token::Comment),
  Num(NumberMatcher, Token::Num),
  TextLiteral(TextMatcher, Token::TextLiteral),
  Identifier(IdentifierMatcher, Token::Identifier);


  override fun toString(): String {
    return matcher.getTokenDescription()
  }
}