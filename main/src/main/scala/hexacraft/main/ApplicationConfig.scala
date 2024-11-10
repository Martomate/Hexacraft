package hexacraft.main

case class ApplicationConfig(
    isDebug: Boolean,
    useTempSaveFolder: Boolean,
    startWorldName: Option[String]
)

object ApplicationConfig {
  def fromSystem: ApplicationConfig = {
    val isDebugStr = System.getProperty("hexacraft.debug")
    val useTempSaveFolder = System.getProperty("hexacraft.tempSaveFolder")
    val startWorldName = System.getProperty("hexacraft.start_world")

    ApplicationConfig(
      isDebug = isDebugStr == "true",
      useTempSaveFolder = useTempSaveFolder == "true",
      startWorldName = Option(startWorldName)
    )
  }
}
