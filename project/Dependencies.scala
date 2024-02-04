import sbt.*

object Dependencies {
  object versions {
    val lwjgl = "3.3.3"
    val joml = "1.10.5"
    val zeromq = "0.5.4"
  }

  lazy val lwjglDependencies = {
    val platforms = Seq(
      "natives-windows",
      "natives-windows-arm64",
      "natives-linux",
      "natives-linux-arm64",
      "natives-macos",
      "natives-macos-arm64"
    )

    Seq(
      "org.lwjgl" % "lwjgl" % versions.lwjgl,
      "org.lwjgl" % "lwjgl-glfw" % versions.lwjgl,
      "org.lwjgl" % "lwjgl-opengl" % versions.lwjgl,
      "org.lwjgl" % "lwjgl-openal" % versions.lwjgl,
      "org.lwjgl" % "lwjgl-stb" % versions.lwjgl
    ) ++ platforms.flatMap(platform =>
      Seq(
        "org.lwjgl" % "lwjgl" % versions.lwjgl classifier platform,
        "org.lwjgl" % "lwjgl-glfw" % versions.lwjgl classifier platform,
        "org.lwjgl" % "lwjgl-opengl" % versions.lwjgl classifier platform,
        "org.lwjgl" % "lwjgl-openal" % versions.lwjgl classifier platform,
        "org.lwjgl" % "lwjgl-stb" % versions.lwjgl classifier platform
      )
    )
  }

  lazy val Joml = "org.joml" % "joml" % versions.joml
  lazy val FlowNbt = "com.flowpowered" % "flow-nbt" % "1.0.0"
  lazy val ZeroMQ = "org.zeromq" % "jeromq" % versions.zeromq

  lazy val MUnit = "org.scalameta" %% "munit" % "0.7.29" % "test"
  lazy val Mockito = "org.scalatestplus" %% "mockito-5-8" % "3.2.17.0" % "test"
  lazy val ArchUnit = Seq(
    "com.tngtech.archunit" % "archunit" % "1.2.1" % "test",
    "org.slf4j" % "slf4j-nop" % "2.0.11" % "test" // Needed for ArchUnit
  )
}
