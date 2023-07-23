package com.martomate.hexacraft

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*
import com.tngtech.archunit.library.Architectures.*
import java.nio.file.Paths
import munit.FunSuite

class RootArchTest extends FunSuite {
  import ArchUnitHelpers.*

  private val allClasses = new ClassFileImporter()
    .importPackages("com.martomate.hexacraft..")
    .ignoreScalaTests()
    .ignoreMTests()

  test("world should not depend on gui") {
    Packages("com.martomate.hexacraft.gui..").assertNotUsedIn("com.martomate.hexacraft.world..")
  }

  test("util should not depend on world") {
    Packages("com.martomate.hexacraft.world..").assertNotUsedIn("com.martomate.hexacraft.util..")
  }

  test("Nbt library should only be used in Nbt wrapper".ignore) {
    Packages("com.flowpowered.nbt..").assertOnlyUsedIn("com.martomate.hexacraft.nbt")
  }

  test("Glfw library should only be used in Glfw wrapper") {
    Packages("org.lwjgl.glfw..").assertOnlyUsedIn("com.martomate.hexacraft.infra.window")
  }

  test("OpenGL library should only be used in OpenGL wrapper") {
    Packages("org.lwjgl.opengl..").assertOnlyUsedIn("com.martomate.hexacraft.infra.gpu")
  }

  // TODO: reduce package dependencies and update this test accordingly
  test("packages should not depend on too many other packages") {
    val root = "root"
    val Font = "Font"
    val Game = "Game"
    val GUI = "GUI"
    val Infra = "Infra"
    val Main = "Main"
    val Menu = "Menu"
    val Nbt = "Nbt"
    val Renderer = "Renderer"
    val Util = "Util"
    val World = "World"

    val JOML = "JOML"
    val JSON = "JSON"
    val NbtLib = "NbtLib"
    val LWJGL = "LWJGL"
    val OpenGL = "OpenGL"
    val GLFW = "GLFW"

    layeredArchitecture()
      .consideringAllDependencies()
      .ignoreDependencyToJava()
      .ignoreDependencyToScala()
      .layer(root, "com.martomate.hexacraft")
      .layer(Font, "com.martomate.hexacraft.font..")
      .layer(Game, "com.martomate.hexacraft.game..")
      .layer(GUI, "com.martomate.hexacraft.gui..")
      .layer(Infra, "com.martomate.hexacraft.infra..")
      .layer(Main, "com.martomate.hexacraft.main..")
      .layer(Menu, "com.martomate.hexacraft.menu..")
      .layer(Nbt, "com.martomate.hexacraft.nbt..")
      .layer(Renderer, "com.martomate.hexacraft.renderer..")
      .layer(Util, "com.martomate.hexacraft.util..")
      .layer(World, "com.martomate.hexacraft.world..")
      .optionalLayer(JOML, "org.joml..")
      .optionalLayer(JSON, "com.eclipsesource.json..")
      .optionalLayer(NbtLib, "com.flowpowered.nbt..")
      .optionalLayer(LWJGL, "org.lwjgl", "org.lwjgl.system..")
      .optionalLayer(OpenGL, "org.lwjgl.opengl..")
      .optionalLayer(GLFW, "org.lwjgl.glfw..")
      .where(Font, _.mayOnlyAccessLayers(Infra, Renderer, JOML))
      .where(Game, _.mayOnlyAccessLayers(root, Font, GUI, Infra, Renderer, Util, World, JOML, Nbt, NbtLib))
      .where(GUI, _.mayOnlyAccessLayers(root, Infra, Font, Renderer, Util, JOML))
      .where(Infra, _.mayOnlyAccessLayers(Util, OpenGL, GLFW, LWJGL, Nbt, NbtLib))
      .where(Main, _.mayOnlyAccessLayers(root, Infra, Game, GUI, Menu, Renderer, Util, World, JOML, LWJGL))
      .where(Menu, _.mayOnlyAccessLayers(root, Infra, Font, Game, GUI, World, JOML, Nbt))
      .where(Renderer, _.mayOnlyAccessLayers(Infra, Util, JOML, LWJGL))
      .where(Nbt, _.mayOnlyAccessLayers(JOML, NbtLib))
      .where(Util, _.mayOnlyAccessLayers(JOML, Nbt))
      .where(World, _.mayOnlyAccessLayers(Infra, Renderer, Util, JOML, JSON, LWJGL, Nbt, NbtLib))
      .check(allClasses)
  }

  case class Packages(packageNames: String*) {
    def assertOnlyUsedIn(packages: String*): Unit = {
      noClasses()
        .that()
        .resideOutsideOfPackages(packages*)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(this.packageNames*)
        .check(allClasses)
    }

    def assertNotUsedIn(packages: String*): Unit =
      noClasses()
        .that()
        .resideInAnyPackage(packages*)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(this.packageNames*)
        .check(allClasses)
  }
}
