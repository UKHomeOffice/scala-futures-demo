val fooS: Map[String, String] = Map("a" -> "xoxox", "b" -> "xoxo")

val barS: Map[Int, String] = Map(4 -> "P", 5 -> "Q")

def calc(s: String): Option[String] = {
  fooS.get(s) flatMap { x =>
    barS.get(x.length)
  }
}

calc("a")