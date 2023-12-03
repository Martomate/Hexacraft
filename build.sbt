import Dependencies.*

import scala.util.Properties.isMac

ThisBuild / organization := "com.martomate"
ThisBuild / version := "0.11"
ThisBuild / scalaVersion := "3.3.1"
ThisBuild / publishArtifact := false
ThisBuild / logBuffered := false
ThisBuild / fork := true

val commonSettings: Seq[Def.SettingsDefinition] = Defaults.coreDefaultSettings ++ Seq(
  artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
    module.organization + "." + artifact.name + "-" + module.revision + "." + artifact.extension
  },
  scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")
)

lazy val root = project
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
    name := "hexacraft",
    javaOptions ++= (if (isMac) Some("-XstartOnFirstThread") else None)
  )
  .settings( // Dependencies
    libraryDependencies ++= lwjglDependencies ++ Seq(Joml) ++ Seq(MUnit, Mockito) ++ ArchUnit
  )
  .enablePlugins(PackPlugin)
  .settings( // Packaging
    packMain := Map(
      "hexacraft" -> "hexacraft.main.Main",
      "hexacraft-mac" -> "hexacraft.main.Main"
    ),
    packJvmOpts := Map(
      "hexacraft" -> Seq(),
      "hexacraft-mac" -> Seq("-XstartOnFirstThread")
    ),
    packJarNameConvention := "full",
    packCopyDependenciesTarget := packTargetDir.value / packDir.value / "libs",
    packGenerateMakefile := false,
    packArchiveExcludes := Seq("VERSION", "bin/hexacraft-mac.bat")
  )
