package com.martomate.hexacraft.resource

import org.joml.{Matrix4f, Vector2f, Vector3f, Vector4f}
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL20

object Shader {
  private var activeShader: Shader = _

  private val shaders = collection.mutable.Map.empty[String, Shader]
  private val matrixBuffer = BufferUtils.createFloatBuffer(16)

  def get(name: String): Option[Shader] = shaders.get(name)

  def init(): Unit = {
    cleanUp()
    Shader("block")("position", "texCoords", "normal", "vertexIndex", "blockPos", "blockTex", "blockHeight", "brightness")("isSide" -> "0")
    Shader("blockSide", fileName = "block")("position", "texCoords", "normal", "vertexIndex", "blockPos", "blockTex", "blockHeight")("isSide" -> "1")
    Shader("entity", fileName = "entity_part")("position", "texCoords", "normal", "vertexIndex", "modelMatrix", "", "", "", "texOffset", "texDim", "blockTex", "brightness")("isSide" -> "0")
    Shader("entitySide", fileName = "entity_part")("position", "texCoords", "normal", "vertexIndex", "modelMatrix", "", "", "", "texOffset", "texDim", "blockTex", "brightness")("isSide" -> "1")
    Shader("gui_block")("position", "texCoords", "normal", "vertexIndex", "blockPos", "blockTex", "blockHeight", "brightness")("isSide" -> "0")
    Shader("gui_blockSide", fileName = "gui_block")("position", "texCoords", "normal", "vertexIndex", "blockPos", "blockTex", "blockHeight", "brightness")("isSide" -> "1")
    Shader("selectedBlock")("position", "blockPos", "color", "blockHeight")()
    Shader("sky")("position")()
    Shader("crosshair")("position")()
    Shader("image")("position")()
    Shader("color")("position")()
    Shader("font")("position", "textureCoords")()
  }

  def foreach(action: Shader => Unit): Unit = {
    shaders.values.foreach(s => {
      s.enable()
      action(s)
    })
  }
  
  def unload(): Unit = {
    activeShader = null
    GL20.glUseProgram(0)
  }

  def cleanUp(): Unit = {
    unload()
    shaders.values.foreach(_.unload())
    shaders.clear()
  }

  def apply(name: String,
            fileName: String = null)
           (attribs: String*)
           (defines: (String, String)*)
           : Shader = new Shader(name, if (fileName != null) fileName else name, attribs, defines)
}

class Shader(val name: String, fileName: String, attribs: Seq[String], defines: Seq[(String, String)]) extends Resource {
  private var shaderID: Int = _
  private val uniformLocations = collection.mutable.Map.empty[String, Int]
  private val attributeLocations = collection.mutable.Map.empty[String, Int]
  Shader.shaders += name -> this

  load()
  
  protected def load(): Unit = {
    val b = ShaderBuilder.start(name)
    b.setDefines(defines)
    b.loadAll(fileName + ".glsl")
    shaderID = b.bindAttribs(attribs: _*).attatchAll().linkAndFinish()._2

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
      val loc = GL20.glGetUniformLocation(shaderID, name)
      uniformLocations.put(name, loc)
      loc
  }

  def getAttribLocation(name: String): Int = attributeLocations.get(name) match {
    case Some(loc) => loc
    case None =>
      val loc = GL20.glGetAttribLocation(shaderID, name)
      attributeLocations.put(name, loc)
      loc
  }

  def bindAttribute(loc: Int, name: String): Unit = {
    GL20.glBindAttribLocation(shaderID, loc, name)
  }

  private def setUniform(name: String)(func: Int => Unit): Unit = {
    enable()
    val loc = getUniformLocation(name)
    if (loc != -1) func(loc)
  }

  def setUniform1i(name: String, a: Int): Unit = setUniform(name)(GL20.glUniform1i(_, a))
  def setUniform1f(name: String, a: Float): Unit = setUniform(name)(GL20.glUniform1f(_, a))
  def setUniform2f(name: String, a: Float, b: Float): Unit = setUniform(name)(GL20.glUniform2f(_, a, b))
  def setUniform3f(name: String, a: Float, b: Float, c: Float): Unit = setUniform(name)(GL20.glUniform3f(_, a, b, c))
  def setUniform4f(name: String, a: Float, b: Float, c: Float, d: Float): Unit = setUniform(name)(GL20.glUniform4f(_, a, b, c, d))

  def setUniform2f(name: String, vec: Vector2f): Unit = setUniform2f(name, vec.x, vec.y)
  def setUniform3f(name: String, vec: Vector3f): Unit = setUniform3f(name, vec.x, vec.y, vec.z)
  def setUniform4f(name: String, vec: Vector4f): Unit = setUniform4f(name, vec.x, vec.y, vec.z, vec.w)

  def setUniformMat4(name: String, matrix: Matrix4f): Unit = {
    setUniform(name)(loc => {
      matrix.get(Shader.matrixBuffer)
      Shader.matrixBuffer.rewind()
      GL20.glUniformMatrix4fv(loc, false, Shader.matrixBuffer)
      Shader.matrixBuffer.clear()
    })
  }

  def enable(): Unit = {
    if (Shader.activeShader != this) {
      Shader.activeShader = this
      GL20.glUseProgram(shaderID)
    }
  }
  
  protected def unload(): Unit = {
    if (Shader.activeShader == this) Shader.unload()
    GL20.glDeleteProgram(shaderID)
    shaderID = 0
  }
}
