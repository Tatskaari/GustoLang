package tatskaari.eval
import kotlin.browser.window

object PromptInputProvider: InputProvider {
  override fun readLine(): String? {
    val value = window.prompt()
    return value
  }
}