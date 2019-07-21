name := "Hexacraft"
organization := "com.martomate"
version := "0.8.1"
scalaVersion := "2.12.6"

enablePlugins(LauncherJarPlugin)

val lwjglVersion = "3.1.6"

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "org.joml" % "joml" % "1.9.9",
  "com.eclipsesource.minimal-json" % "minimal-json" % "0.9.4",
  "com.flowpowered" % "flow-nbt" % "1.0.0",
  "org.lwjgl" % "lwjgl" % lwjglVersion,
  "org.lwjgl" % "lwjgl" % lwjglVersion classifier "natives-windows",
  "org.lwjgl" % "lwjgl" % lwjglVersion classifier "natives-linux",
  "org.lwjgl" % "lwjgl" % lwjglVersion classifier "natives-macos",
  "org.lwjgl" % "lwjgl-glfw" % lwjglVersion,
  "org.lwjgl" % "lwjgl-glfw" % lwjglVersion classifier "natives-windows",
  "org.lwjgl" % "lwjgl-glfw" % lwjglVersion classifier "natives-linux",
  "org.lwjgl" % "lwjgl-glfw" % lwjglVersion classifier "natives-macos",
  "org.lwjgl" % "lwjgl-opengl" % lwjglVersion,
  "org.lwjgl" % "lwjgl-opengl" % lwjglVersion classifier "natives-windows",
  "org.lwjgl" % "lwjgl-opengl" % lwjglVersion classifier "natives-linux",
  "org.lwjgl" % "lwjgl-opengl" % lwjglVersion classifier "natives-macos"
)
