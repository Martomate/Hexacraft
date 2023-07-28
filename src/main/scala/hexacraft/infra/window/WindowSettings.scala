package hexacraft.infra.window

case class WindowSettings(
    width: Int,
    height: Int,
    title: String,
    opengl: WindowSettings.Opengl,
    resizable: Boolean,
    samples: Int
)

object WindowSettings {
  case class Opengl(majorVersion: Int, minorVersion: Int, debugMode: Boolean)
}
