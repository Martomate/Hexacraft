import sbt.*

import scala.collection.immutable.Seq

object Dependencies {
  object versions {
    val lwjgl = "3.3.2"
    val joml = "1.10.5"
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
      "org.lwjgl" % "lwjgl-opengl" % versions.lwjgl
    ) ++ platforms.flatMap(platform =>
      Seq(
        "org.lwjgl" % "lwjgl" % versions.lwjgl classifier platform,
        "org.lwjgl" % "lwjgl-glfw" % versions.lwjgl classifier platform,
        "org.lwjgl" % "lwjgl-opengl" % versions.lwjgl classifier platform
      )
    )
  }

  lazy val Joml = "org.joml" % "joml" % versions.joml
  lazy val FlowNbt = "com.flowpowered" % "flow-nbt" % "1.0.0"

  lazy val MUnit = "org.scalameta" %% "munit" % "0.7.29" % "test"
  lazy val Mockito = "org.scalatestplus" %% "mockito-4-11" % "3.2.16.0" % "test"
  lazy val ArchUnit = Seq(
    "com.tngtech.archunit" % "archunit" % "1.0.1" % "test",
    "org.slf4j" % "slf4j-nop" % "2.0.5" % "test" // Needed for ArchUnit
  )
}
