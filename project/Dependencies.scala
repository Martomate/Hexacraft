import sbt.*

object Dependencies {
  object versions {
    val lwjgl = "3.3.4"
    val joml = "1.10.7"
    val zeromq = "0.6.0"
  }

  def lwjglDependency(name: String) = {
    val platforms = Seq(
      "natives-windows",
      "natives-windows-arm64",
      "natives-linux",
      "natives-linux-arm64",
      "natives-macos",
      "natives-macos-arm64"
    )

    val base = "org.lwjgl" % name % versions.lwjgl
    val natives = platforms.map(p => "org.lwjgl" % name % versions.lwjgl classifier p)
    base +: natives
  }

  lazy val LwjglSystem = lwjglDependency("lwjgl")
  lazy val LwjglGlfw = lwjglDependency("lwjgl-glfw")
  lazy val LwjglOpenGL = lwjglDependency("lwjgl-opengl")
  lazy val LwjglOpenAL = lwjglDependency("lwjgl-openal")
  lazy val LwjglStb = lwjglDependency("lwjgl-stb")

  lazy val Joml = "org.joml" % "joml" % versions.joml
  lazy val FlowNbt = "com.flowpowered" % "flow-nbt" % "1.0.0"
  lazy val ZeroMQ = "org.zeromq" % "jeromq" % versions.zeromq

  lazy val MUnit = "org.scalameta" %% "munit" % "1.0.0" % Test
  lazy val Mockito = "org.scalatestplus" %% "mockito-5-8" % "3.2.17.0" % Test
  lazy val ArchUnit = Seq(
    "com.tngtech.archunit" % "archunit" % "1.3.0" % Test,
    "org.slf4j" % "slf4j-nop" % "2.0.13" % Test // Needed for ArchUnit
  )
}
