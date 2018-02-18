package tatskaari.parsing

interface SourceTree {
  fun getSource(path : String) : String
}