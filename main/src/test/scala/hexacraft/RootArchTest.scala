package hexacraft

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*
import com.tngtech.archunit.library.Architectures.*
import munit.FunSuite

class RootArchTest extends FunSuite {
  import ArchUnitHelpers.*

  private val allClasses = new ClassFileImporter()
    .importPackages("hexacraft..", "com.martomate..")
    .ignoreScalaTests()
    .ignoreMTests()

  test("world should not depend on gui") {
    Packages("hexacraft.gui..").assertNotUsedIn("hexacraft.world..")
  }

  test("util should not depend on world") {
    Packages("hexacraft.world..").assertNotUsedIn("hexacraft.util..")
  }

  test("Nbt library should only be used in Nbt module") {
    Packages("com.flowpowered.nbt..").assertOnlyUsedIn("hexacraft.nbt")
  }

  test("Glfw library should only be used in Glfw wrapper") {
    Packages("org.lwjgl.glfw..").assertOnlyUsedIn("hexacraft.infra.window")
  }

  test("OpenGL library should only be used in OpenGL wrapper") {
    Packages("org.lwjgl.opengl..").assertOnlyUsedIn("hexacraft.infra.gpu")
  }

  test("OpenAL library should only be used in OpenAL wrapper") {
    Packages("org.lwjgl.openal..").assertOnlyUsedIn("hexacraft.infra.audio")
  }

  test("Stb library should only be used in OpenAL wrapper") {
    Packages("org.lwjgl.stb..").assertOnlyUsedIn("hexacraft.infra.audio")
  }

  // TODO: reduce package dependencies and update this test accordingly
  test("packages should not depend on too many other packages") {
    val root = "root"
    val Game = "Game"
    val Server = "Server"
    val Client = "Client"
    val GUI = "GUI"
    val Infra = "Infra"
    val Main = "Main"
    val Math = "Math"
    val Nbt = "Nbt"
    val Physics = "Physics"
    val Renderer = "Renderer"
    val Shaders = "Shaders"
    val Text = "Text"
    val Util = "Util"
    val World = "World"
    val Native = "Native"

    val NbtLib = "NbtLib"
    val JOML = "JOML"
    val LWJGL = "LWJGL"
    val ZeroMQ = "ZeroMQ"
    val WrappedLibs = "WrappedLibs"

    layeredArchitecture()
      .consideringAllDependencies()
      .ignoreDependencyToJava()
      .ignoreDependencyToScala()
      .layer(root, "hexacraft")
      .layer(Game, "hexacraft.game..")
      .layer(Client, "hexacraft.client..")
      .layer(Server, "hexacraft.server..")
      .layer(GUI, "hexacraft.gui..")
      .layer(Infra, "hexacraft.infra..")
      .layer(Main, "hexacraft.main..")
      .layer(Math, "hexacraft.math..")
      .layer(Physics, "hexacraft.physics..")
      .layer(Nbt, "hexacraft.nbt..")
      .layer(Renderer, "hexacraft.renderer..")
      .layer(Shaders, "hexacraft.shaders..")
      .layer(Text, "hexacraft.text..")
      .layer(Util, "hexacraft.util..")
      .layer(World, "hexacraft.world..")
      .layer(Native, "hexacraft.rs..")
      .optionalLayer(JOML, "org.joml..")
      .optionalLayer(NbtLib, "com.flowpowered.nbt..")
      .optionalLayer(LWJGL, "org.lwjgl", "org.lwjgl.system..") // TODO: wrap this lib
      .optionalLayer(ZeroMQ, "org.zeromq..")
      .optionalLayer(
        WrappedLibs,
        Seq(
          "org.lwjgl.opengl..",
          "org.lwjgl.openal..",
          "org.lwjgl.glfw..",
          "org.lwjgl.stb.."
        )*
      )
      .where(Game, _.mayOnlyAccessLayers(Infra, Physics, World, JOML, Nbt))
      // format: off
      .where(Client, _.mayOnlyAccessLayers(Game, Text, GUI, Infra, Math, Renderer, Shaders, Physics, Util, World, JOML, LWJGL, Nbt, ZeroMQ))
      // format: on
      .where(Server, _.mayOnlyAccessLayers(Game, Infra, Math, Physics, Util, World, JOML, Nbt, ZeroMQ))
      .where(GUI, _.mayOnlyAccessLayers(root, Infra, Math, Text, Renderer, Shaders, Util, JOML))
      .where(Infra, _.mayOnlyAccessLayers(Math, Util, JOML, WrappedLibs, LWJGL, Nbt))
      .where(Main, _.mayOnlyAccessLayers(root, Infra, Game, Server, Client, GUI, Renderer, Util, World, JOML, Nbt))
      .where(Math, _.mayOnlyAccessLayers(Util, Native, JOML))
      .where(Physics, _.mayOnlyAccessLayers(Util, JOML))
      .where(Nbt, _.mayOnlyAccessLayers(Util, JOML, NbtLib))
      .where(Renderer, _.mayOnlyAccessLayers(Infra, Util, JOML, LWJGL))
      .where(Shaders, _.mayOnlyAccessLayers(Infra, Renderer, World, LWJGL, JOML))
      .where(Text, _.mayOnlyAccessLayers(Infra, Renderer, Shaders, JOML))
      .where(Util, _.mayOnlyAccessLayers(JOML, Nbt))
      .where(World, _.mayOnlyAccessLayers(Infra, Math, Physics, Util, JOML, Nbt))
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
