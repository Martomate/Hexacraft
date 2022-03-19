enablePlugins(LauncherJarPlugin)

val isMac = System.getProperty("os.name").toLowerCase().contains("mac")

lazy val Benchmark = config("bench") extend Test

lazy val root = Project("hexacraft", file("."))
  .settings(Defaults.coreDefaultSettings: _*)
  .settings(mainSettings: _*)
  .configs(Benchmark)
  .settings(inConfig(Benchmark)(Defaults.testSettings): _*)

def mainSettings: Seq[Def.SettingsDefinition] = Seq(
  name := "Hexacraft",
  organization := "com.martomate",
  version := "0.10",
  scalaVersion := "2.13.7",
  scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xlint"),
  javaOptions ++= (if (isMac) Some("-XstartOnFirstThread") else None),
  publishArtifact := false,
  libraryDependencies ++= lwjglDependencies ++ otherDependencies ++ testDependencies,
  testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework"),
  parallelExecution in Benchmark := false,
  logBuffered := false,
  fork := true
)

def lwjglDependencies = {
  val lwjglVersion = "3.3.1"

  val platforms = Seq(
    "natives-windows",
    "natives-windows-arm64",
    "natives-linux",
    "natives-linux-arm64",
    "natives-macos",
    "natives-macos-arm64",
  )

  Seq(
    "org.lwjgl" % "lwjgl" % lwjglVersion,
    "org.lwjgl" % "lwjgl-glfw" % lwjglVersion,
    "org.lwjgl" % "lwjgl-opengl" % lwjglVersion
  ) ++ platforms.flatMap(platform => Seq(
    "org.lwjgl" % "lwjgl" % lwjglVersion classifier platform,
    "org.lwjgl" % "lwjgl-glfw" % lwjglVersion classifier platform,
    "org.lwjgl" % "lwjgl-opengl" % lwjglVersion classifier platform
  ))
}

val scalatestVersion = "3.2.11"

def otherDependencies = Seq(
  "org.scalactic" %% "scalactic" % scalatestVersion,
  "org.joml" % "joml" % "1.10.4",
  "com.eclipsesource.minimal-json" % "minimal-json" % "0.9.5",
  "com.flowpowered" % "flow-nbt" % "1.0.0"
)

def testDependencies = Seq(
  "org.scalatest" %% "scalatest-flatspec" % scalatestVersion % "test",
  "org.scalatest" %% "scalatest-shouldmatchers" % scalatestVersion % "test",
  "org.scalamock" %% "scalamock" % "5.2.0" % "test",
  "com.storm-enroute" %% "scalameter" % "0.21" % "bench"
)
