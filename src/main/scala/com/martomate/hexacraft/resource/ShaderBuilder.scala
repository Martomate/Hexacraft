package com.martomate.hexacraft.resource

import java.io.IOException

import com.martomate.hexacraft.util.FileUtils
import org.lwjgl.opengl.{GL11, GL20, GL32, GL40}

object ShaderBuilder {
  def start(name: String): ShaderBuilder = new ShaderBuilder(name)
}

class ShaderBuilder(name: String) {
  private val shaders = collection.mutable.Map.empty[Int, Int]
  private val programID = GL20.glCreateProgram()
  private var prefix = "shaders/"
  private var definesText = ""

  def setPrefix(newPrefix: String): ShaderBuilder = {
    prefix = newPrefix
    this
  }

  def setDefines(defines: Seq[(String, String)]): Unit = {
    definesText = defines.map(d => s"#define ${d._1} ${d._2}\n").mkString
  }

  def getShaderType(shaderType: String): Int = {
    shaderType match {
      case "vs" | "vert" => GL20.GL_VERTEX_SHADER
      case "fs" | "frag" => GL20.GL_FRAGMENT_SHADER
      case "gs" | "geom" => GL32.GL_GEOMETRY_SHADER
      case "tc"          => GL40.GL_TESS_CONTROL_SHADER
      case "te"          => GL40.GL_TESS_EVALUATION_SHADER
      case _ =>
        System.err.println("Shadertype '" + shaderType + "' not supported.")
        -1
    }
  }

  def header: String = "#version 330 core\n\n" + definesText

  def loadSource(path: String): String = {
    val source = new StringBuilder()

    try {
      val reader = FileUtils.getBufferedReader(FileUtils.getResourceFile(prefix + path).get)
      reader.lines.forEach(line => {
        source.append(line).append('\n')
      })
      reader.close()
    } catch {
      case e: Exception =>
        throw new IOException("Could not load shader at " + (prefix + path), e)
    }

    source.toString
  }

  def loadAll(path: String): ShaderBuilder = {
    val s = loadSource(path)
    for (part <- s.split("#pragma shader ")) {
      val newLineIdx = part.indexOf('\n')
      if (newLineIdx != -1) {
        val shaderType = part.substring(0, newLineIdx)
        val source = part.substring(newLineIdx + 1)
        loadShader(getShaderType(shaderType), source)
      }
    }
    this
  }

  def load(shaderType: Int, path: String): ShaderBuilder = {
    loadShader(shaderType, loadSource(path))
  }

  def loadShader(shaderType: Int, source: String): ShaderBuilder = {
    val shaderID = GL20.glCreateShader(shaderType)

    GL20.glShaderSource(shaderID, header + source)
    GL20.glCompileShader(shaderID)
    if (GL20.glGetShaderi(shaderID, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
      val shaderTypeName = if (shaderType == GL20.GL_VERTEX_SHADER) {
        "Vertexshader"
      } else if (shaderType == GL20.GL_FRAGMENT_SHADER) {
        "Fragmentshader"
      } else {
        "Shader"
      }
      val maxLen = math.max(GL20.glGetShaderi(programID, GL20.GL_INFO_LOG_LENGTH), 256)
      System.err.println(s"$shaderTypeName failed to compile ($name).\nError log:\n"
        + GL20.glGetShaderInfoLog(shaderID, maxLen))
    }
    shaders.put(shaderType, shaderID)
    this
  }

  def bindAttribs(attribs: String*): ShaderBuilder = {
    for (i <- attribs.indices) if (attribs(i) != "") GL20.glBindAttribLocation(programID, i, attribs(i))
    this
  }

  def attatchAll(): ShaderBuilder = {
    for (i <- shaders.values) GL20.glAttachShader(programID, i)
    this
  }

  def linkAndFinish(): Int = {
    GL20.glLinkProgram(programID)

    if (GL20.glGetProgrami(programID, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
      val maxLen = math.max(GL20.glGetShaderi(programID, GL20.GL_INFO_LOG_LENGTH), 256)
      System.err.println("Link error: " + GL20.glGetProgramInfoLog(programID, maxLen))
    }

    for (i <- shaders.values) {
      GL20.glDetachShader(programID, i)
      GL20.glDeleteShader(i)
    }

    programID
  }
}
