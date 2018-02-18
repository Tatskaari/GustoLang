package tatskaari.parsing

external fun getResource(path: String) : String
object WebRootSourceTree : SourceTree {
  override fun getSource(path: String): String {
    return getResource("tatskaari/parsing/$path.gusto")
  }
}