package hexacraft.infra.gpu

import hexacraft.util.Tracker

import munit.FunSuite

class OpenGLTest extends FunSuite {
  import OpenGL.Event.*

  test("loadShader works by default") {
    OpenGL._enterTestMode()

    val res = OpenGL.loadShader(OpenGL.ShaderType.Vertex, "void main() {}")

    assert(res.isOk)
  }

  test("loadShader emits a tracking event") {
    OpenGL._enterTestMode()

    val tracker = Tracker.withStorage[OpenGL.Event]
    OpenGL.trackEvents(tracker)

    val res = OpenGL.loadShader(OpenGL.ShaderType.Vertex, "void main() {}")

    val events = tracker.events
    assertEquals(events.size, 1)

    val event = events.head.asInstanceOf[ShaderLoaded]
    assertEquals(event.shaderType, OpenGL.ShaderType.Vertex)
    assertEquals(event.source, "void main() {}")
  }

  test("unloadShader emits a tracking event") {
    OpenGL._enterTestMode()

    val shaderId = OpenGL.loadShader(OpenGL.ShaderType.Vertex, "void main() {}").unwrap()

    val tracker = Tracker.withStorage[OpenGL.Event]
    OpenGL.trackEvents(tracker)

    OpenGL.unloadShader(shaderId)

    val events = tracker.events
    assertEquals(events.size, 1)
    assertEquals(events, Seq(ShaderUnloaded(shaderId)))
  }

  test("createProgram emits a tracking event") {
    OpenGL._enterTestMode()

    val tracker = Tracker.withStorage[OpenGL.Event]
    OpenGL.trackEvents(tracker)

    val programId = OpenGL.createProgram()

    val events = tracker.events
    assertEquals(events, Seq(ProgramCreated(programId)))
  }

  test("deleteProgram emits a tracking event") {
    OpenGL._enterTestMode()

    val programId = OpenGL.createProgram()

    val tracker = Tracker.withStorage[OpenGL.Event]
    OpenGL.trackEvents(tracker)

    OpenGL.deleteProgram(programId)

    val events = tracker.events
    assertEquals(events, Seq(ProgramDeleted(programId)))
  }
}
