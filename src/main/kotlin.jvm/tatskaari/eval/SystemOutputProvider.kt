package tatskaari.eval

object SystemOutputProvider: OutputProvider {
  override fun println(text: String) {
    kotlin.io.println(text)
  }
}