package tatskaari.parsing

object ClassSourceTree : SourceTree {
  override fun getSource(path: String) : String {
    return javaClass.getResourceAsStream( "$path.gusto")
      .bufferedReader().use { it.readText() }
  }
}