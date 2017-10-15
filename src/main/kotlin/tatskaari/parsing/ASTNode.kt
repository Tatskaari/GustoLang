package tatskaari.parsing

import tatskaari.tokenising.Token


abstract class ASTNode (val startToken: Token, val endToken: Token)