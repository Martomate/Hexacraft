package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.infra.OpenGL
import com.martomate.hexacraft.util.Tracker

import munit.FunSuite

class ShaderBuilderTest extends FunSuite {
  import OpenGL.Event.*
  import OpenGL.ShaderType.*

  test("loadAll loads shaders") {
    OpenGL._enterTestMode()

    val tracker = Tracker.withStorage[OpenGL.Event]
    OpenGL.trackEvents(tracker)

    ShaderBuilder.start("test_shader").loadAll("block.glsl")

    val events = tracker.events.map(_.asInstanceOf[ShaderLoaded])
    assertEquals(events.map(_.shaderType), Seq(Vertex, Fragment))
  }

  test("loadAll includes header when loading shaders") {
    OpenGL._enterTestMode()

    val tracker = Tracker.withStorage[OpenGL.Event]
    OpenGL.trackEvents(tracker)

    ShaderBuilder.start("test_shader").loadAll("block.glsl")

    val events = tracker.events.map(_.asInstanceOf[ShaderLoaded])
    assertEquals(
      events.map(_.source.takeWhile(_ != '\n')),
      Seq.fill(2)("#version 330 core")
    )
  }

  test("linkAndFinish unloads shaders after linking the program") {
    OpenGL._enterTestMode()

    val loadTracker = Tracker.withStorage[OpenGL.Event]
    val unloadTracker = Tracker.withStorage[OpenGL.Event]

    OpenGL.trackEvents(loadTracker)
    val builder = ShaderBuilder.start("test_shader").loadAll("block.glsl")
    val shaderIds = loadTracker.events.map(_.asInstanceOf[ShaderLoaded].shaderId)

    OpenGL.trackEvents(unloadTracker)
    builder.linkAndFinish()

    val events = unloadTracker.events.map(_.asInstanceOf[ShaderUnloaded])
    assertEquals(events.map(_.shaderId), shaderIds)
  }
}
