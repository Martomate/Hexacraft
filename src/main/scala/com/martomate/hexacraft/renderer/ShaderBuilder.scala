package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.util.{FileUtils, OpenGL}

import java.io.IOException
import org.lwjgl.opengl.{GL11, GL20, GL32, GL40}
import scala.collection.mutable

object ShaderBuilder {
  def start(name: String): ShaderBuilder = new ShaderBuilder(name)
}

class ShaderBuilder(name: String) {
  private val shaders = collection.mutable.Map.empty[Int, Int]
  private val programID = OpenGL.glCreateProgram()
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

  private def getShaderType(shaderType: String): Int = {
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

  private def header: String = "#version 330 core\n\n" + definesText

  private def loadSource(path: String): String = {
    val source = new mutable.StringBuilder()

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

  private def loadShader(shaderType: Int, source: String): ShaderBuilder = {
    val shaderID = OpenGL.glCreateShader(shaderType)

    OpenGL.glShaderSource(shaderID, header + source)
    OpenGL.glCompileShader(shaderID)
    if (OpenGL.glGetShaderi(shaderID, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
      val shaderTypeName = if (shaderType == GL20.GL_VERTEX_SHADER) {
        "Vertexshader"
      } else if (shaderType == GL20.GL_FRAGMENT_SHADER) {
        "Fragmentshader"
      } else {
        "Shader"
      }
      val maxLen = math.max(OpenGL.glGetShaderi(programID, GL20.GL_INFO_LOG_LENGTH), 256)
      System.err.println(
        s"$shaderTypeName failed to compile ($name).\nError log:\n"
          + OpenGL.glGetShaderInfoLog(shaderID, maxLen)
      )
    }
    shaders.put(shaderType, shaderID)
    this
  }

  def bindAttribs(attribs: String*): ShaderBuilder = {
    for (i <- attribs.indices)
      if (attribs(i) != "") OpenGL.glBindAttribLocation(programID, i, attribs(i))
    this
  }

  def attachAll(): ShaderBuilder = {
    for (i <- shaders.values) OpenGL.glAttachShader(programID, i)
    this
  }

  def linkAndFinish(): Int = {
    OpenGL.glLinkProgram(programID)

    if (OpenGL.glGetProgrami(programID, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
      val maxLen = math.max(OpenGL.glGetShaderi(programID, GL20.GL_INFO_LOG_LENGTH), 256)
      System.err.println("Link error: " + OpenGL.glGetProgramInfoLog(programID, maxLen))
    }

    for (i <- shaders.values) {
      OpenGL.glDetachShader(programID, i)
      OpenGL.glDeleteShader(i)
    }

    programID
  }
}
