import Dependencies.*

import scala.util.Properties.isMac

ThisBuild / organization := "com.martomate"
ThisBuild / version := "0.14"
ThisBuild / scalaVersion := "3.4.2"
ThisBuild / publishArtifact := false
ThisBuild / logBuffered := false
ThisBuild / fork := true

val commonSettings: scala.Seq[Def.Setting[?]] = Defaults.coreDefaultSettings ++ Seq(
  artifactName := { (_: ScalaVersion, module: ModuleID, artifact: Artifact) =>
    module.organization + "." + module.name + "-" + module.revision + "." + artifact.extension
  },
  scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"),
  javacOptions ++= Seq("-release", "11"),
  libraryDependencies += "org.scala-lang" %% "scala2-library-tasty-experimental" % scalaVersion.value
)

lazy val hexacraft = project
  .in(file("."))
  .aggregate(common, nbt, window, audio, fs, gpu, system, game, client, server, main)

lazy val common = project
  .in(file("common"))
  .settings(commonSettings*)
  .settings(
    libraryDependencies ++= Seq(Joml) :+ MUnit
  )

lazy val nbt = project
  .in(file("nbt"))
  .settings(commonSettings*)
  .settings(
    libraryDependencies ++= Seq(Joml, FlowNbt) :+ MUnit
  )

lazy val window = project
  .in(file("window"))
  .dependsOn(common)
  .settings(commonSettings*)
  .settings(
    libraryDependencies ++= LwjglGlfw :+ MUnit
  )

lazy val audio = project
  .in(file("audio"))
  .dependsOn(common, fs)
  .settings(commonSettings*)
  .settings(
    libraryDependencies ++= LwjglOpenAL ++ LwjglStb :+ MUnit
  )

lazy val fs = project
  .in(file("fs"))
  .dependsOn(common, nbt)
  .settings(commonSettings*)
  .settings(
    libraryDependencies ++= Seq() :+ MUnit
  )

lazy val gpu = project
  .in(file("gpu"))
  .dependsOn(common)
  .settings(commonSettings*)
  .settings(
    libraryDependencies ++= LwjglOpenGL :+ MUnit
  )

lazy val system = project
  .in(file("system"))
  .dependsOn(common)
  .settings(commonSettings*)
  .settings(
    libraryDependencies ++= Seq() :+ MUnit
  )

lazy val game = project
  .in(file("game"))
  .dependsOn(common, nbt, window, fs)
  .settings(commonSettings*)
  .settings(
    libraryDependencies ++= LwjglSystem ++ Seq(Joml, ZeroMQ) ++ Seq(MUnit, Mockito)
  )

lazy val client = project
  .in(file("client"))
  .dependsOn(game, audio, gpu)
  .settings(commonSettings*)
  .settings(
    libraryDependencies ++= Seq(Joml, ZeroMQ) :+ MUnit
  )

lazy val server = project
  .in(file("server"))
  .dependsOn(game % "compile->compile;test->test")
  .settings(commonSettings*)
  .settings(
    libraryDependencies ++= Seq(Joml, ZeroMQ) :+ MUnit
  )

lazy val main = project
  .in(file("main"))
  .dependsOn(
    game % "compile->compile;test->test",
    client % "compile->compile;test->test",
    server,
    system
  )
  .settings(commonSettings*)
  .settings( // General
    javaOptions ++= (if (isMac) Some("-XstartOnFirstThread") else None)
  )
  .settings( // Dependencies
    libraryDependencies ++= Seq(Joml, ZeroMQ) ++ Seq(MUnit, Mockito) ++ ArchUnit
  )
  .enablePlugins(PackPlugin)
  .settings( // Packaging (using sbt-pack)
    moduleName := "hexacraft",
    packArchivePrefix := "hexacraft",
    packMain := Map(
      "hexacraft" -> "hexacraft.main.Main",
      "hexacraft-mac" -> "hexacraft.main.Main"
    ),
    packJvmOpts := Map(
      "hexacraft" -> Seq(),
      "hexacraft-mac" -> Seq("-XstartOnFirstThread")
    ),
    packJarNameConvention := "full",
    packGenerateMakefile := false,
    packArchiveExcludes := Seq("VERSION", "bin/hexacraft-mac.bat"),
    packExcludeJars := Seq(".*scala2-library-tasty.*\\.jar")
  )
