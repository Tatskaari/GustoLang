package tatskaari.eval

external fun output(text: String)
object JSHookOutputProvider : OutputProvider {
  override fun println(text: String) {
    output(text)
  }
}