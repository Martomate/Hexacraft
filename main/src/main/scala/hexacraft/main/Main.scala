package hexacraft.main

object Main {
  def main(args: Array[String]): Unit = {
    val config = ApplicationConfig.fromSystem

    val application = Application.create(config)

    val success = application.run()

    if !success then {
      System.exit(1)
    }
  }
}
