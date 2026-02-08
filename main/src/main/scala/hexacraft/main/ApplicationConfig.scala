package hexacraft.main

case class ApplicationConfig(
    isDebug: Boolean,
    useTempSaveFolder: Boolean
)

object ApplicationConfig {
  def fromSystem: ApplicationConfig = {
    val isDebugStr = System.getProperty("hexacraft.debug")
    val useTempSaveFolder = System.getProperty("hexacraft.tempSaveFolder")

    ApplicationConfig(
      isDebug = isDebugStr == "true",
      useTempSaveFolder = useTempSaveFolder == "true"
    )
  }
}
