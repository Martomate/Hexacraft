package hexacraft

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*
import com.tngtech.archunit.library.Architectures.*
import munit.FunSuite

class RootArchTest extends FunSuite {
  import ArchUnitHelpers.*

  private val allClasses = new ClassFileImporter()
    .importPackages("hexacraft..")
    .ignoreScalaTests()
    .ignoreMTests()

  test("world should not depend on gui") {
    Packages("hexacraft.gui..").assertNotUsedIn("hexacraft.world..")
  }

  test("util should not depend on world") {
    Packages("hexacraft.world..").assertNotUsedIn("hexacraft.util..")
  }

  test("Nbt library should only be used in Nbt module") {
    Packages("com.flowpowered.nbt..").assertOnlyUsedIn() // not used in any package, only in nbt module
  }

  test("Glfw library should only be used in Glfw wrapper") {
    Packages("org.lwjgl.glfw..").assertOnlyUsedIn("hexacraft.infra.window")
  }

  test("OpenGL library should only be used in OpenGL wrapper") {
    Packages("org.lwjgl.opengl..").assertOnlyUsedIn("hexacraft.infra.gpu")
  }

  // TODO: reduce package dependencies and update this test accordingly
  test("packages should not depend on too many other packages") {
    val root = "root"
    val Game = "Game"
    val GUI = "GUI"
    val Infra = "Infra"
    val Main = "Main"
    val Math = "Math"
    val Physics = "Physics"
    val Renderer = "Renderer"
    val Text = "Text"
    val Util = "Util"
    val World = "World"

    val Nbt = "Nbt"
    val JOML = "JOML"
    val LWJGL = "LWJGL"
    val OpenGL = "OpenGL"
    val GLFW = "GLFW"
    val ZeroMQ = "ZeroMQ"

    layeredArchitecture()
      .consideringAllDependencies()
      .ignoreDependencyToJava()
      .ignoreDependencyToScala()
      .layer(root, "hexacraft")
      .layer(Game, "hexacraft.game..")
      .layer(GUI, "hexacraft.gui..")
      .layer(Infra, "hexacraft.infra..")
      .layer(Main, "hexacraft.main..")
      .layer(Math, "hexacraft.math..")
      .layer(Physics, "hexacraft.physics..")
      .layer(Renderer, "hexacraft.renderer..")
      .layer(Text, "hexacraft.text..")
      .layer(Util, "hexacraft.util..")
      .layer(World, "hexacraft.world..")
      .optionalLayer(JOML, "org.joml..")
      .optionalLayer(Nbt, "com.martomate.nbt..")
      .optionalLayer(LWJGL, "org.lwjgl", "org.lwjgl.system..")
      .optionalLayer(OpenGL, "org.lwjgl.opengl..")
      .optionalLayer(GLFW, "org.lwjgl.glfw..")
      .optionalLayer(ZeroMQ, "org.zeromq..")
      .where(
        Game,
        _.mayOnlyAccessLayers(root, Text, GUI, Infra, Math, Renderer, Physics, Util, World, JOML, Nbt, ZeroMQ)
      )
      .where(GUI, _.mayOnlyAccessLayers(root, Infra, Math, Text, Renderer, Util, JOML))
      .where(Infra, _.mayOnlyAccessLayers(Math, Renderer, Util, World, OpenGL, GLFW, LWJGL, Nbt))
      .where(Main, _.mayOnlyAccessLayers(root, Infra, Game, GUI, Renderer, Util, World, JOML, LWJGL))
      .where(Renderer, _.mayOnlyAccessLayers(Infra, Util, JOML, LWJGL))
      .where(Text, _.mayOnlyAccessLayers(Infra, Renderer, JOML))
      .where(Util, _.mayOnlyAccessLayers(JOML, Nbt))
      .where(World, _.mayOnlyAccessLayers(Math, Infra, Renderer, Physics, Util, JOML, LWJGL, Nbt))
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
