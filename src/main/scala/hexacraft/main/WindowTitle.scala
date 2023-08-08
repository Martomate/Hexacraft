package hexacraft.main

class WindowTitle(fps: Int, frameTimeMs: Int, vsync: Boolean) {
  def format: String =
    val msString = (if frameTimeMs < 10 then "0" else "") + frameTimeMs
    val vsyncStr = if vsync then "vsync" else ""
    val parts = Seq("Hexacraft", s"$fps fps   ms: $msString", vsyncStr)
    parts.filter(_.nonEmpty).mkString("   |   ")
}
