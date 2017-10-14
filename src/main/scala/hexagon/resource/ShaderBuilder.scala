package hexagon.resource

import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL32
import org.lwjgl.opengl.GL40

import hexagon.Main

object ShaderBuilder {
  def start(name: String): ShaderBuilder = new ShaderBuilder(name)
}

class ShaderBuilder(name: String) {
  private val shaders = collection.mutable.Map.empty[Int, Int]
  private val programID = GL20.glCreateProgram()
  private var prefix = "res/shaders/"
  private var definesText = ""

  def setPrefix(newPrefix: String): ShaderBuilder = {
    prefix = newPrefix
    this
  }

  def setDefines(defines: Seq[(String, String)]): Unit = {
    definesText = defines.map(d => s"#define ${d._1} ${d._2}\n").mkString
  }

  def load(shaderType: String): ShaderBuilder = load(shaderType, name + '.' + shaderType)

  def load(shaderType: String, path: String): ShaderBuilder = {
    val t: Int = shaderType match {
      case "vs" | "vert" => GL20.GL_VERTEX_SHADER
      case "fs" | "frag" => GL20.GL_FRAGMENT_SHADER
      case "gs" | "geom" => GL32.GL_GEOMETRY_SHADER
      case "tc"          => GL40.GL_TESS_CONTROL_SHADER
      case "te"          => GL40.GL_TESS_EVALUATION_SHADER
      case _ =>
        System.err.println("Shadertype '" + shaderType + "' not supported.")
        -1
    }
    t match {
      case -1 => this
      case _  => load(t, path)
    }
  }

  def load(shaderType: Int, path: String): ShaderBuilder = {
    val shaderID = GL20.glCreateShader(shaderType)
    val source = new StringBuilder("#version 330 core\n\n" + definesText)

    try {
      val reader = new BufferedReader(new FileReader(prefix + path))
      reader.lines.forEach(line => {
        source.append(line).append('\n')
      })
      reader.close()
    } catch {
      case e: IOException =>
        e.printStackTrace()
        Main.destroy
        System.exit(1)
    }

    GL20.glShaderSource(shaderID, source)
    GL20.glCompileShader(shaderID)
    if (GL20.glGetShaderi(shaderID, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
      val shaderTypeName = if (shaderType == GL20.GL_VERTEX_SHADER) {
        "Vertexshader"
      } else if (shaderType == GL20.GL_FRAGMENT_SHADER) {
        "Fragmentshader"
      } else {
        "Shader"
      }
      System.err.println(s"$shaderTypeName failed to compile ($path).\nError log:\n"
        + GL20.glGetShaderInfoLog(shaderID, GL20.glGetShaderi(shaderID, GL20.GL_INFO_LOG_LENGTH)))
    }
    shaders.put(shaderType, shaderID)
    this
  }

  def bindAttribs(attribs: String*): ShaderBuilder = {
    for (i <- attribs.indices) GL20.glBindAttribLocation(programID, i, attribs(i))
    this
  }

  def attatchAll(): ShaderBuilder = {
    for (i <- shaders.values) GL20.glAttachShader(programID, i)
    this
  }

  def linkAndFinish(): (String, Int) = {
    GL20.glLinkProgram(programID)

    if (GL20.glGetProgrami(programID, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
      System.err.println("Link error: " + GL20.glGetProgramInfoLog(programID, GL20.glGetShaderi(programID, GL20.GL_INFO_LOG_LENGTH)))
    }

    GL20.glValidateProgram(programID)

    if (GL20.glGetProgrami(programID, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
      System.err.println("Validation error: " + GL20.glGetProgramInfoLog(programID, GL20.glGetShaderi(programID, GL20.GL_INFO_LOG_LENGTH)))
    }

    for (i <- shaders.values) {
      GL20.glDetachShader(programID, i)
      GL20.glDeleteShader(i)
    }

    (name, programID)
  }
}
