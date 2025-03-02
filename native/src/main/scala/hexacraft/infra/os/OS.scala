package hexacraft.infra.os

sealed trait OS {
  def name: String

  def appdataPath: String
}

object OS {
  def current: OS = {
    val osStr = System.getProperty("os.name").toLowerCase()

    if osStr.startsWith("win") then {
      Windows
    } else if osStr.startsWith("mac") || osStr.startsWith("darwin") then {
      Mac
    } else {
      Linux
    }
  }
}

case object Windows extends OS {
  val name = "Windows"

  def appdataPath: String = System.getenv("appdata")
}

case object Mac extends OS {
  val name = "Mac"

  def appdataPath: String = System.getProperty("user.home")
}

case object Linux extends OS {
  val name = "Linux"

  def appdataPath: String = System.getProperty("user.home")
}
