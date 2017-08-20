package tatskaari.eval

external fun input(): String
object JSHookInputProvider : InputProvider {
  override fun readLine(): String? {
    return input()
  }
}