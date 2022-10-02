package com.martomate.hexacraft

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*
import com.tngtech.archunit.library.Architectures.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.file.Paths

class RootArchTest extends AnyFlatSpec with Matchers {
  import ArchUnitHelpers.*

  private val allClasses = new ClassFileImporter()
    .importPackages("com.martomate.hexacraft..")
    .ignoreScalatests()

  "world" should "not depend on gui" in {
    noClasses()
      .that()
      .resideInAPackage("com.martomate.hexacraft.world..")
      .should()
      .dependOnClassesThat()
      .resideInAPackage("com.martomate.hexacraft.gui..")
      .check(allClasses)
  }

  "util" should "not depend on world" in {
    noClasses()
      .that()
      .resideInAPackage("com.martomate.hexacraft.util..")
      .should()
      .dependOnClassesThat()
      .resideInAPackage("com.martomate.hexacraft.world..")
      .check(allClasses)
  }

  // TODO: reduce package dependencies and update this test accordingly
  "packages" should "not depend on too many other packages" in {
    layeredArchitecture()
      .consideringAllDependencies()
      .ignoreDependencyToJava()
      .ignoreDependencyToScala()
      .layer("root", "com.martomate.hexacraft")
      .layer("Font", "com.martomate.hexacraft.font..")
      .layer("Game", "com.martomate.hexacraft.game..")
      .layer("GUI", "com.martomate.hexacraft.gui..")
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
      .whereLayer("Font", _.mayOnlyAccessLayers("Renderer", "Util", "JOML", "OpenGL"))
      .whereLayer("Game", _.mayOnlyAccessLayers("root", "Font", "GUI", "Renderer", "Util", "World", "JOML", "NBT"))
      .whereLayer("GUI", _.mayOnlyAccessLayers("root", "Font", "Renderer", "Util", "World", "JOML", "OpenGL"))
      .whereLayer(
        "Main",
        _.mayOnlyAccessLayers("root", "GUI", "Menu", "Renderer", "Util", "World", "JOML", "LWJGL", "OpenGL", "GLFW")
      )
      .whereLayer("Menu", _.mayOnlyAccessLayers("root", "Font", "Game", "GUI", "Util", "World", "JOML", "NBT"))
      .whereLayer("Renderer", _.mayOnlyAccessLayers("Util", "JOML", "LWJGL", "OpenGL"))
      .whereLayer("Util", _.mayOnlyAccessLayers("JOML", "NBT"))
      .whereLayer("World", _.mayOnlyAccessLayers("Renderer", "Util", "JOML", "JSON", "LWJGL", "NBT", "OpenGL"))
      .check(allClasses)
  }
}
