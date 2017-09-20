package tatskaari.compatibility

import tatskaari.tokenising.Token

typealias TokenList = ArrayList<Token>

fun TokenList.removeFirst() = removeAt(0)
fun TokenList.addFirst(token: Token) = add(0, token)

fun random() : Double {
  return kotlin.js.Math.random()
}
