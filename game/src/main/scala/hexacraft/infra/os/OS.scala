package hexacraft.infra.os

sealed trait OS:
  def name: String
  def appdataPath: String

case object Windows extends OS:
  val name = "Windows"
  def appdataPath: String = System.getenv("appdata")

case object Mac extends OS:
  val name = "Mac"
  def appdataPath: String = System.getProperty("user.home")

case object Linux extends OS:
  val name = "Linux"
  def appdataPath: String = System.getProperty("user.home")
