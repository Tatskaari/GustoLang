object TestUtil {
  fun loadProgram(name : String) : String {
    return javaClass.getResourceAsStream(name + ".flav").bufferedReader().use { it.readText() }
  }
}