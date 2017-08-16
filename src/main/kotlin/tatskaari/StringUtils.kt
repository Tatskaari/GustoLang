package tatskaari

object StringUtils {
  fun String.rest(head: String): String {
    return substring(head.length, length)
  }
}

