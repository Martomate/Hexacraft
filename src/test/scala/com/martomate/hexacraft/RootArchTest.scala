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
    noClasses()
      .that()
      .resideInAPackage("com.martomate.hexacraft.world..")
      .should()
      .dependOnClassesThat()
      .resideInAPackage("com.martomate.hexacraft.gui..")
      .check(allClasses)
  }

  test("util should not depend on world") {
    noClasses()
      .that()
      .resideInAPackage("com.martomate.hexacraft.util..")
      .should()
      .dependOnClassesThat()
      .resideInAPackage("com.martomate.hexacraft.world..")
      .check(allClasses)
  }

  // TODO: reduce package dependencies and update this test accordingly
  test("packages should not depend on too many other packages") {
    layeredArchitecture()
      .consideringAllDependencies()
      .ignoreDependencyToJava()
      .ignoreDependencyToScala()
      .layer("root", "com.martomate.hexacraft")
      .layer("Font", "com.martomate.hexacraft.font..")
      .layer("Game", "com.martomate.hexacraft.game..")
      .layer("GUI", "com.martomate.hexacraft.gui..")
      .layer("Infra", "com.martomate.hexacraft.infra..")
      .layer("Main", "com.martomate.hexacraft.main..")
      .layer("Menu", "com.martomate.hexacraft.menu..")
      .layer("Renderer", "com.martomate.hexacraft.renderer..")
      .layer("Util", "com.martomate.hexacraft.util..")
      .layer("World", "com.martomate.hexacraft.world..")
      .optionalLayer("JOML", "org.joml..")
      .optionalLayer("JSON", "com.eclipsesource.json..")
      .optionalLayer("NBT", "com.flowpowered.nbt..")
      .optionalLayer("LWJGL", "org.lwjgl", "org.lwjgl.system..")
      .optionalLayer("OpenGL", "org.lwjgl.opengl..")
      .optionalLayer("GLFW", "org.lwjgl.glfw..")
      .whereLayer("Font", _.mayOnlyAccessLayers("Renderer", "Util", "JOML"))
      .whereLayer(
        "Game",
        _.mayOnlyAccessLayers("root", "Font", "GUI", "Infra", "Renderer", "Util", "World", "JOML", "NBT")
      )
      .whereLayer("GUI", _.mayOnlyAccessLayers("root", "Font", "Renderer", "Util", "JOML"))
      .whereLayer(
        "Main",
        _.mayOnlyAccessLayers("root", "GUI", "Menu", "Renderer", "Util", "World", "JOML", "LWJGL", "GLFW")
      )
      .whereLayer("Menu", _.mayOnlyAccessLayers("root", "Font", "Game", "GUI", "Util", "World", "JOML", "NBT"))
      .whereLayer("Renderer", _.mayOnlyAccessLayers("Util", "JOML", "LWJGL"))
      .whereLayer("Util", _.mayOnlyAccessLayers("JOML", "NBT", "OpenGL", "LWJGL"))
      .whereLayer("World", _.mayOnlyAccessLayers("Infra", "Renderer", "Util", "JOML", "JSON", "LWJGL", "NBT"))
      .check(allClasses)
  }
}
