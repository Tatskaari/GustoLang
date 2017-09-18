package tatskaari;

public class ByteArrayClassLoader(parent: ClassLoader?) : ClassLoader(parent) {
    fun defineClass(name: String, byes: ByteArray): Class<*>? {
      return defineClass(name, byes, 0, byes.size)
    }
}
