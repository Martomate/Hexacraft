import sbt.*

import scala.collection.immutable.Seq

object Dependencies {
  lazy val lwjglDependencies = {
    val lwjglVersion = "3.3.2"

    val platforms = Seq(
      "natives-windows",
      "natives-windows-arm64",
      "natives-linux",
      "natives-linux-arm64",
      "natives-macos",
      "natives-macos-arm64"
    )

    Seq(
      "org.lwjgl" % "lwjgl" % lwjglVersion,
      "org.lwjgl" % "lwjgl-glfw" % lwjglVersion,
      "org.lwjgl" % "lwjgl-opengl" % lwjglVersion
    ) ++ platforms.flatMap(platform =>
      Seq(
        "org.lwjgl" % "lwjgl" % lwjglVersion classifier platform,
        "org.lwjgl" % "lwjgl-glfw" % lwjglVersion classifier platform,
        "org.lwjgl" % "lwjgl-opengl" % lwjglVersion classifier platform
      )
    )
  }

  lazy val Joml = "org.joml" % "joml" % "1.10.5"
  lazy val FlowNbt = "com.flowpowered" % "flow-nbt" % "1.0.0"

  lazy val MUnit = "org.scalameta" %% "munit" % "0.7.29" % "test"
  lazy val Mockito = "org.scalatestplus" %% "mockito-4-11" % "3.2.16.0" % "test"
  lazy val ArchUnit = Seq(
    "com.tngtech.archunit" % "archunit" % "1.0.1" % "test",
    "org.slf4j" % "slf4j-nop" % "2.0.5" % "test" // Needed for ArchUnit
  )
}
