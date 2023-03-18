package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.util.{OpenGL, Resource}

import org.joml.{Matrix4f, Vector2f, Vector3f, Vector4f}
import org.lwjgl.BufferUtils

object Shader {
  private var activeShader: Shader = _

  private val shaders = collection.mutable.Map.empty[String, Shader]
  private val matrixBuffer = BufferUtils.createFloatBuffer(16)

  def get(name: String): Option[Shader] = shaders.get(name)

  def init(): Unit = {
    cleanUp()

    Shaders.registerAll()
  }

  def foreach(action: Shader => Unit): Unit = {
    shaders.values.foreach(s => {
      s.enable()
      action(s)
    })
  }

  def unload(): Unit = {
    activeShader = null
    OpenGL.glUseProgram(0)
  }

  def cleanUp(): Unit = {
    unload()
    shaders.values.foreach(_.unload())
    shaders.clear()
  }

  def register(config: ShaderConfig): Shader = {
    val shader = new Shader(config)
    Shader.shaders += config.name -> shader
    shader
  }
}

class Shader private (config: ShaderConfig) extends Resource {
  private var shaderID: Int = _
  private val uniformLocations = collection.mutable.Map.empty[String, Int]
  private val attributeLocations = collection.mutable.Map.empty[String, Int]

  load()

  protected def load(): Unit = {
    shaderID = ShaderBuilder
      .start(config.name)
      .setDefines(config.defines)
      .loadAll(config.fileName + ".glsl")
      .bindAttribs(config.attribs: _*)
      .attachAll()
      .linkAndFinish()

    uniformLocations.clear()
    attributeLocations.clear()
  }

  protected def reload(): Unit = {
    unload()
    load()
  }

  def getUniformLocation(name: String): Int = uniformLocations.get(name) match {
    case Some(loc) => loc
    case None =>
      val loc = OpenGL.glGetUniformLocation(shaderID, name)
      uniformLocations.put(name, loc)
      loc
  }

  def getAttribLocation(name: String): Int = attributeLocations.get(name) match {
    case Some(loc) => loc
    case None =>
      val loc = OpenGL.glGetAttribLocation(shaderID, name)
      attributeLocations.put(name, loc)
      loc
  }

  def bindAttribute(loc: Int, name: String): Unit = {
    OpenGL.glBindAttribLocation(shaderID, loc, name)
  }

  private def setUniform(name: String)(func: Int => Unit): Unit = {
    enable()
    val loc = getUniformLocation(name)
    if (loc != -1) func(loc)
  }

  def setUniform1i(name: String, a: Int): Unit = setUniform(name)(OpenGL.glUniform1i(_, a))

  def setUniform1f(name: String, a: Float): Unit = setUniform(name)(OpenGL.glUniform1f(_, a))

  def setUniform2f(name: String, a: Float, b: Float): Unit =
    setUniform(name)(OpenGL.glUniform2f(_, a, b))

  def setUniform3f(name: String, a: Float, b: Float, c: Float): Unit =
    setUniform(name)(OpenGL.glUniform3f(_, a, b, c))

  def setUniform4f(name: String, a: Float, b: Float, c: Float, d: Float): Unit =
    setUniform(name)(OpenGL.glUniform4f(_, a, b, c, d))

  def setUniform2f(name: String, vec: Vector2f): Unit = setUniform2f(name, vec.x, vec.y)

  def setUniform3f(name: String, vec: Vector3f): Unit = setUniform3f(name, vec.x, vec.y, vec.z)

  def setUniform4f(name: String, vec: Vector4f): Unit =
    setUniform4f(name, vec.x, vec.y, vec.z, vec.w)

  def setUniformMat4(name: String, matrix: Matrix4f): Unit = {
    setUniform(name)(loc => {
      matrix.get(Shader.matrixBuffer)
      Shader.matrixBuffer.rewind()
      OpenGL.glUniformMatrix4fv(loc, false, Shader.matrixBuffer)
      Shader.matrixBuffer.clear()
    })
  }

  def enable(): Unit = {
    if (Shader.activeShader != this) {
      Shader.activeShader = this
      OpenGL.glUseProgram(shaderID)
    }
  }

  protected def unload(): Unit = {
    if (Shader.activeShader == this) Shader.unload()
    OpenGL.glDeleteProgram(shaderID)
    shaderID = 0
  }
}
