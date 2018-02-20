package tatskaari;

class ByteArrayClassLoader(parent: ClassLoader?) : ClassLoader(parent) {
    fun defineClass(name: String, bytes: ByteArray): Class<*>? {
      return defineClass(name, bytes, 0, bytes.size)
    }
}
