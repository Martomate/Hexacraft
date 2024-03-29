package hexacraft.renderer

import hexacraft.infra.fs.Bundle
import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.gpu.OpenGL.ShaderType
import hexacraft.util.{Resource, Result}
import hexacraft.util.Result.{Err, Ok}

import org.joml.{Matrix4f, Vector2f, Vector3f, Vector4f}
import org.lwjgl.BufferUtils

import java.io.IOException
import scala.collection.mutable

object Shader {
  private var activeShader: Shader = _

  private val matrixBuffer = BufferUtils.createFloatBuffer(16)

  def unload(): Unit = {
    activeShader = null
    OpenGL.glUseProgram(OpenGL.ProgramId.none)
  }

  def from(config: ShaderConfig): Shader = new Shader(config)
}

class Shader private (config: ShaderConfig) extends Resource {
  private var shaderID: OpenGL.ProgramId = OpenGL.ProgramId.none
  private val uniformLocations = collection.mutable.Map.empty[String, OpenGL.UniformLocation]
  private val attributeLocations = collection.mutable.Map.empty[String, Int]

  load()

  protected def load(): Unit = {
    shaderID = new ShaderBuilder()
      .setDefines(config.defines)
      .loadAll(config.fileNames)
      .bindAttribs(config.inputs: _*)
      .attachAll()
      .linkAndFinish()

    uniformLocations.clear()
    attributeLocations.clear()
  }

  private def getUniformLocation(name: String): OpenGL.UniformLocation = {
    uniformLocations.get(name) match {
      case Some(loc) => loc
      case None =>
        val loc = OpenGL.glGetUniformLocation(shaderID, name)
        uniformLocations.put(name, loc)
        loc
    }
  }

  def getAttribLocation(name: String): Int = {
    attributeLocations.get(name) match {
      case Some(loc) => loc
      case None =>
        val loc = OpenGL.glGetAttribLocation(shaderID, name)
        attributeLocations.put(name, loc)
        loc
    }
  }

  def bindAttribute(loc: Int, name: String): Unit = {
    OpenGL.glBindAttribLocation(shaderID, loc, name)
  }

  private def setUniform(name: String)(func: OpenGL.UniformLocation => Unit): Unit = {
    activate()
    val loc = getUniformLocation(name)
    if loc.exists then {
      func(loc)
    }
  }

  def setUniform1i(name: String, a: Int): Unit = {
    setUniform(name)(OpenGL.glUniform1i(_, a))
  }

  def setUniform1f(name: String, a: Float): Unit = {
    setUniform(name)(OpenGL.glUniform1f(_, a))
  }

  def setUniform2f(name: String, a: Float, b: Float): Unit = {
    setUniform(name)(OpenGL.glUniform2f(_, a, b))
  }

  def setUniform3f(name: String, a: Float, b: Float, c: Float): Unit = {
    setUniform(name)(OpenGL.glUniform3f(_, a, b, c))
  }

  def setUniform4f(name: String, a: Float, b: Float, c: Float, d: Float): Unit = {
    setUniform(name)(OpenGL.glUniform4f(_, a, b, c, d))
  }

  def setUniform2f(name: String, vec: Vector2f): Unit = {
    setUniform2f(name, vec.x, vec.y)
  }

  def setUniform3f(name: String, vec: Vector3f): Unit = {
    setUniform3f(name, vec.x, vec.y, vec.z)
  }

  def setUniform4f(name: String, vec: Vector4f): Unit = {
    setUniform4f(name, vec.x, vec.y, vec.z, vec.w)
  }

  def setUniformMat4(name: String, matrix: Matrix4f): Unit = {
    setUniform(name)(loc => {
      matrix.get(Shader.matrixBuffer)
      Shader.matrixBuffer.rewind()
      OpenGL.glUniformMatrix4fv(loc, false, Shader.matrixBuffer)
      Shader.matrixBuffer.clear()
    })
  }

  def activate(): Unit = {
    if Shader.activeShader != this then {
      Shader.activeShader = this
      OpenGL.glUseProgram(shaderID)
    }
  }

  protected def unload(): Unit = {
    if Shader.activeShader == this then {
      Shader.unload()
    }
    OpenGL.deleteProgram(shaderID)
    shaderID = OpenGL.ProgramId.none
  }
}

object ShaderConfig {
  def apply(): ShaderConfig = {
    ShaderConfig(Map.empty, Nil, Nil)
  }
}

/** @param fileNames
  * The names of the shader files for each shader stage
  * @param inputs
  * A list of all inputs to the first shader stage. Note: matrices take several spots
  * @param defines
  * A list of #define statements to include in the beginning of the shader file
  */
case class ShaderConfig(
    fileNames: Map[OpenGL.ShaderType, String],
    inputs: Seq[String],
    defines: Seq[(String, String)]
) {
  def withStage(stage: ShaderType, path: String): ShaderConfig = copy(fileNames = fileNames + (stage -> path))

  /** a.k.a. "attributes" */
  def withInputs(inputs: String*): ShaderConfig = copy(inputs = inputs)

  def withDefines(defines: (String, String)*): ShaderConfig = copy(defines = defines)
}

class ShaderBuilder {
  private val shaders = collection.mutable.Map.empty[OpenGL.ShaderType, OpenGL.ShaderId]
  private val programID = OpenGL.createProgram()
  private var prefix = "shaders/"
  private var definesText = ""

  def setPrefix(newPrefix: String): ShaderBuilder = {
    prefix = newPrefix
    this
  }

  def setDefines(defines: Seq[(String, String)]): ShaderBuilder = {
    definesText = defines.map(d => s"#define ${d._1} ${d._2}\n").mkString
    this
  }

  private def header: String = s"#version 330 core\n\n$definesText"

  private def loadSource(path: String): String = {
    val source = new mutable.StringBuilder()

    try {
      for line <- Bundle.locate(prefix + path).get.readLines() do {
        source.append(line).append('\n')
      }
    } catch {
      case e: Exception =>
        throw new IOException("Could not load shader at " + (prefix + path), e)
    }

    source.toString
  }

  def loadAll(paths: Map[OpenGL.ShaderType, String]): ShaderBuilder = {
    for (shaderType, path) <- paths do {
      val source = loadSource(path)

      OpenGL.loadShader(shaderType, header + source) match {
        case Ok(shaderId) =>
          shaders.put(shaderType, shaderId)
        case Err(e) =>
          System.err.println(s"Shader '$path' failed to compile:\n${e.message}")
      }
    }

    this
  }

  def bindAttribs(attribs: String*): ShaderBuilder = {
    for i <- attribs.indices do {
      if attribs(i) != "" then {
        OpenGL.glBindAttribLocation(programID, i, attribs(i))
      }
    }
    this
  }

  def attachAll(): ShaderBuilder = {
    for i <- shaders.values do {
      OpenGL.glAttachShader(programID, i)
    }
    this
  }

  def linkAndFinish(): OpenGL.ProgramId = {
    OpenGL.linkProgram(programID) match {
      case Err(errorMessage) =>
        System.err.println("Link error: " + errorMessage)
      case _ =>
    }

    for i <- shaders.values do {
      OpenGL.glDetachShader(programID, i)
      OpenGL.unloadShader(i)
    }

    programID
  }
}
