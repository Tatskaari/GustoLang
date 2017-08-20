package tatskaari.eval

object StdinInputProvider: InputProvider{
  override fun readLine(): String? {
    return readLine()
  }
}