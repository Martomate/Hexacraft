import Dependencies.*

import scala.util.Properties.isMac

val commonSettings: Seq[Def.SettingsDefinition] = Seq(
  organization := "com.martomate",
  version := "0.11",
  scalaVersion := "3.3.0",
  scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")
)

lazy val nbt = project
  .in(file("nbt"))
  .settings(commonSettings*)
  .settings(
    libraryDependencies ++= Seq(Joml, FlowNbt) :+ MUnit
  )

lazy val root = Project("hexacraft", file("."))
  .dependsOn(nbt)
  .enablePlugins(LauncherJarPlugin)
  .settings(Defaults.coreDefaultSettings*)
  .settings(commonSettings*)
  .settings(
    libraryDependencies ++= lwjglDependencies ++ Seq(Joml, FlowNbt) ++ Seq(MUnit, Mockito) ++ ArchUnit,
    javaOptions ++= (if (isMac) Some("-XstartOnFirstThread") else None)
  )

ThisBuild / publishArtifact := false
ThisBuild / logBuffered := false
ThisBuild / fork := true
