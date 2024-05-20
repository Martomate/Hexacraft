package hexacraft.renderer

import hexacraft.infra.gpu.OpenGL
import hexacraft.util.Tracker

import munit.FunSuite

class ShaderBuilderTest extends FunSuite {
  import OpenGL.Event.*
  import OpenGL.ShaderType.*

  test("loadAll loads shaders") {
    OpenGL._enterTestMode()

    val tracker = Tracker.withStorage[OpenGL.Event]
    OpenGL.trackEvents(tracker)

    ShaderLoader.tryLoad(ShaderConfig().withStage(Vertex, "block/vert.glsl").withStage(Fragment, "block/frag.glsl"))

    val shaderTypes = tracker.events.collect:
      case ShaderLoaded(_, shaderType, _) => shaderType

    assertEquals(shaderTypes, Seq(Vertex, Fragment))
  }

  test("loadAll includes header when loading shaders") {
    OpenGL._enterTestMode()

    val tracker = Tracker.withStorage[OpenGL.Event]
    OpenGL.trackEvents(tracker)

    ShaderLoader.tryLoad(ShaderConfig().withStage(Vertex, "block/vert.glsl").withStage(Fragment, "block/frag.glsl"))

    val shaderSources = tracker.events.collect:
      case ShaderLoaded(_, _, source) => source.takeWhile(_ != '\n')

    assertEquals(shaderSources, Seq.fill(2)("#version 330 core"))
  }

  test("linkAndFinish unloads shaders after linking the program") {
    OpenGL._enterTestMode()

    val tracker = Tracker.withStorage[OpenGL.Event]

    OpenGL.trackEvents(tracker)
    val builder =
      ShaderLoader
        .tryLoad(ShaderConfig().withStage(Vertex, "block/vert.glsl").withStage(Fragment, "block/frag.glsl"))
        .unwrap()
    val loadedShaderIds = tracker.events.collect:
      case ShaderLoaded(shaderId, _, _) => shaderId

    val unloadedShaderIds = tracker.events.collect:
      case ShaderUnloaded(shaderId) => shaderId

    assertEquals(unloadedShaderIds.sorted, loadedShaderIds.sorted)
  }
}
