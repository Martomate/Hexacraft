enablePlugins(LauncherJarPlugin)

lazy val Benchmark = config("bench") extend Test

lazy val root = Project(
  "hexacraft",
  file(".")
) settings (Defaults.coreDefaultSettings: _*) settings (
  name := "Hexacraft",
  organization := "com.martomate",
  version := "0.9",
  scalaVersion := "2.12.8",
  scalacOptions ++= Seq("-deprecation"),//, "-unchecked", "-feature", "-Xlint"),
  publishArtifact := false,
  libraryDependencies ++= lwjglDependencies ++ otherDependencies ++ testDependencies,
  resolvers ++= Seq(
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"
  ),
  testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework"),
  parallelExecution in Benchmark := false,
  logBuffered := false
) configs(
  Benchmark
) settings(
  inConfig(Benchmark)(Defaults.testSettings): _*
)

def lwjglDependencies = {
  val lwjglVersion = "3.2.2"

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

def otherDependencies = Seq(
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.joml" % "joml" % "1.9.9",
  "com.eclipsesource.minimal-json" % "minimal-json" % "0.9.4",
  "com.flowpowered" % "flow-nbt" % "1.0.0"
)

def testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "org.scalamock" %% "scalamock" % "4.1.0" % "test",
  "com.storm-enroute" %% "scalameter" % "0.19" % "bench"
)
