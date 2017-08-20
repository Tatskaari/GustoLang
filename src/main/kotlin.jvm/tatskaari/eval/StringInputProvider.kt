package tatskaari.eval

class StringInputProvider(val string: String): InputProvider {
  val strings: List<String> = string.split("\n")
  var index = 0

  override fun readLine(): String? {
    return strings[index++]
  }
}