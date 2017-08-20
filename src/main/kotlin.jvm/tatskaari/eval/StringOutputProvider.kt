package tatskaari.eval

class StringOutputProvider: OutputProvider{
  var outputString: String = ""

  override fun println(text: String) {
    outputString+=text
  }
}