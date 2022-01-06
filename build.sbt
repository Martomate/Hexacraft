enablePlugins(LauncherJarPlugin)

lazy val Benchmark = config("bench") extend Test

lazy val root = Project("hexacraft", file("."))
  .settings(Defaults.coreDefaultSettings: _*)
  .settings(mainSettings: _*)
  .configs(Benchmark)
  .settings(inConfig(Benchmark)(Defaults.testSettings): _*)

def mainSettings = Seq(
  name := "Hexacraft",
  organization := "com.martomate",
  version := "0.10",
  scalaVersion := "2.13.7",
  scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xlint"),
  publishArtifact := false,
  libraryDependencies ++= lwjglDependencies ++ otherDependencies ++ testDependencies,
  testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework"),
  parallelExecution in Benchmark := false,
  logBuffered := false
)

def lwjglDependencies = {
  val lwjglVersion = "3.3.0"

  Seq(
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
}

val scalatestVersion = "3.2.10"

def otherDependencies = Seq(
  "org.scalactic" %% "scalactic" % scalatestVersion,
  "org.joml" % "joml" % "1.10.3",
  "com.eclipsesource.minimal-json" % "minimal-json" % "0.9.5",
  "com.flowpowered" % "flow-nbt" % "1.0.0"
)

def testDependencies = Seq(
  "org.scalatest" %% "scalatest-flatspec" % scalatestVersion % "test",
  "org.scalatest" %% "scalatest-shouldmatchers" % scalatestVersion % "test",
  "org.scalamock" %% "scalamock" % "5.2.0" % "test",
  "com.storm-enroute" %% "scalameter" % "0.21" % "bench"
)
