import scala.util.Properties.isMac

lazy val root = Project("hexacraft", file("."))
  .enablePlugins(LauncherJarPlugin)
  .settings(Defaults.coreDefaultSettings*)
  .settings(mainSettings*)

def mainSettings: Seq[Def.SettingsDefinition] = Seq(
  name := "Hexacraft",
  organization := "com.martomate",
  version := "0.10",
  scalaVersion := "3.3.0",
  scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"),
  javaOptions ++= (if (isMac) Some("-XstartOnFirstThread") else None),
  publishArtifact := false,
  libraryDependencies ++= lwjglDependencies ++ otherDependencies ++ testDependencies,
  logBuffered := false,
  fork := true
)

def lwjglDependencies = {
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

def otherDependencies = Seq(
  "org.joml" % "joml" % "1.10.5",
  "com.eclipsesource.minimal-json" % "minimal-json" % "0.9.5",
  "com.flowpowered" % "flow-nbt" % "1.0.0"
)

def testDependencies = Seq(
  "org.scalameta" %% "munit" % "0.7.29" % "test",
  "org.scalatestplus" %% "mockito-4-11" % "3.2.16.0" % "test",
  "com.tngtech.archunit" % "archunit" % "1.0.1" % "test",
  "org.slf4j" % "slf4j-nop" % "2.0.5" % "test" // Needed for ArchUnit
)
