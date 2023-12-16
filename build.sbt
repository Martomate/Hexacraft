import Dependencies.*

import scala.util.Properties.isMac

ThisBuild / organization := "com.martomate"
ThisBuild / version := "0.12"
ThisBuild / scalaVersion := "3.3.1"
ThisBuild / publishArtifact := false
ThisBuild / logBuffered := false
ThisBuild / fork := true

val commonSettings: scala.Seq[Def.Setting[?]] = Defaults.coreDefaultSettings ++ Seq(
  artifactName := { (_: ScalaVersion, module: ModuleID, artifact: Artifact) =>
    module.organization + "." + module.name + "-" + module.revision + "." + artifact.extension
  },
  scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"),
  javacOptions ++= Seq("-release", "11")
)

lazy val hexacraft = project
  .in(file("."))
  .aggregate(nbt, game)
  .dependsOn(nbt, game)

lazy val nbt = project
  .in(file("nbt"))
  .settings(commonSettings*)
  .settings(
    libraryDependencies ++= Seq(Joml, FlowNbt) :+ MUnit
  )

lazy val game = project
  .in(file("game"))
  .dependsOn(nbt)
  .settings(commonSettings*)
  .settings( // General
    javaOptions ++= (if (isMac) Some("-XstartOnFirstThread") else None)
  )
  .settings( // Dependencies
    libraryDependencies ++= lwjglDependencies ++ Seq(Joml, ZeroMQ) ++ Seq(MUnit, Mockito) ++ ArchUnit
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
    packArchiveExcludes := Seq("VERSION", "bin/hexacraft-mac.bat")
  )
