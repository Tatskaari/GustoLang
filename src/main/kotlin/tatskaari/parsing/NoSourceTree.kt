package tatskaari.parsing

object NoSourceTree : SourceTree {
  override fun getSource(path: String) : String {
    throw RuntimeException("Attempted to include sources however no source tree was configures: $path")
  }
}