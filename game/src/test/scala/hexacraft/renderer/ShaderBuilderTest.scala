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

    new ShaderBuilder().loadAll("frag.glsl")

    val shaderTypes = tracker.events.collect:
      case ShaderLoaded(_, shaderType, _) => shaderType

    assertEquals(shaderTypes, Seq(Vertex, Fragment))
  }

  test("loadAll includes header when loading shaders") {
    OpenGL._enterTestMode()

    val tracker = Tracker.withStorage[OpenGL.Event]
    OpenGL.trackEvents(tracker)

    new ShaderBuilder().loadAll("frag.glsl")

    val shaderSources = tracker.events.collect:
      case ShaderLoaded(_, _, source) => source.takeWhile(_ != '\n')

    assertEquals(shaderSources, Seq.fill(2)("#version 330 core"))
  }

  test("linkAndFinish unloads shaders after linking the program") {
    OpenGL._enterTestMode()

    val loadTracker = Tracker.withStorage[OpenGL.Event]
    val unloadTracker = Tracker.withStorage[OpenGL.Event]

    OpenGL.trackEvents(loadTracker)
    val builder = new ShaderBuilder().loadAll("frag.glsl")
    val loadedShaderIds = loadTracker.events.collect:
      case ShaderLoaded(shaderId, _, _) => shaderId

    OpenGL.trackEvents(unloadTracker)
    builder.linkAndFinish()

    val unloadedShaderIds = unloadTracker.events.collect:
      case ShaderUnloaded(shaderId) => shaderId

    assertEquals(unloadedShaderIds.sorted, loadedShaderIds.sorted)
  }
}
