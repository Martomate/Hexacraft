package hexacraft.renderer

import hexacraft.infra.fs.Bundle
import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.gpu.OpenGL.{linkProgram, ShaderType}
import hexacraft.util.{Batch, Resource, Result}

import org.joml.{Matrix4f, Vector2f, Vector3f, Vector4f}
import org.lwjgl.BufferUtils

import scala.collection.mutable
import scala.util.Try

object Shader {
  private var activeShader: Shader = null.asInstanceOf[Shader]

  private val matrixBuffer = BufferUtils.createFloatBuffer(16)

  def unload(): Unit = {
    activeShader = null
    OpenGL.glUseProgram(OpenGL.ProgramId.none)
  }

  def from(config: ShaderConfig): Shader = {
    val programId = ShaderLoader.tryLoad(config).unwrap()
    Shader(programId)
  }
}

class Shader private (shaderID: OpenGL.ProgramId) extends Resource {
  private val uniformLocations = collection.mutable.Map.empty[String, OpenGL.UniformLocation]
  private val attributeLocations = collection.mutable.Map.empty[String, Int]

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

object ShaderLoader {
  enum Error {
    case FileNotFound(shaderType: ShaderType, path: String)
    case FileNotReadable(shaderType: ShaderType, error: Throwable)
    case CompilationFailed(shaderType: ShaderType, message: String)
    case LinkingFailed(message: String)
  }

  def tryLoad(config: ShaderConfig): Result[OpenGL.ProgramId, Error] = {
    val prelude = makePrelude(config.defines)

    val shaderIds = for {
      (shaderType, path) <- Batch.of(config.fileNames.toSeq)

      file <- Result
        .fromOption(findShader(path))
        .mapErr(_ => Error.FileNotFound(shaderType, path))

      source <- Result
        .fromTry(readShaderFile(file, prelude))
        .mapErr(e => Error.FileNotReadable(shaderType, e))

      shaderId <- OpenGL
        .loadShader(shaderType, source)
        .mapErr(e => Error.CompilationFailed(shaderType, e.message))
    } yield shaderId

    for {
      shaderIds <- shaderIds.toResult
      programId <- finish(shaderIds, config.inputs)
    } yield programId
  }

  private def makePrelude(defines: Seq[(String, String)]): String = {
    val s = new StringBuilder

    s.append("#version 330 core\n")
    for (name, value) <- defines do {
      s.append(s"#define $name $value\n")
    }
    s.append("\n")

    s.toString
  }

  private def findShader(path: String): Option[Bundle.Resource] = {
    Bundle.locate(s"shaders/$path")
  }

  private def readShaderFile(file: Bundle.Resource, prelude: String): Try[String] = {
    Try(file.readLines()).map(lines => prelude + lines.mkString("\n"))
  }

  private def finish(shaderIds: Seq[OpenGL.ShaderId], inputs: Seq[String]): Result[OpenGL.ProgramId, Error] = {
    val programId = OpenGL.createProgram()

    for i <- inputs.indices do {
      if inputs(i) != "" then {
        OpenGL.glBindAttribLocation(programId, i, inputs(i))
      }
    }

    for i <- shaderIds do {
      OpenGL.glAttachShader(programId, i)
    }

    val linkResult = OpenGL.linkProgram(programId).mapErr(m => Error.LinkingFailed(m))

    for i <- shaderIds do {
      OpenGL.glDetachShader(programId, i)
      OpenGL.unloadShader(i)
    }

    linkResult.map(_ => programId)
  }
}
