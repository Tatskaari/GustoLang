package tatskaari.eval

import kotlin.browser.window

object AlertOutputProvider: OutputProvider {
  override fun println(text: String) {
    window.alert(text)
  }
}