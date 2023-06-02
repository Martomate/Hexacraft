package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.util.{FileUtils, OpenGL}

import java.io.IOException
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

object ShaderBuilder {
  def start(name: String): ShaderBuilder = new ShaderBuilder(name)
}

class ShaderBuilder(name: String) {
  private val shaders = collection.mutable.Map.empty[OpenGL.ShaderType, OpenGL.ShaderId]
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

  private def getShaderType(shaderType: String): Try[OpenGL.ShaderType] = {
    import OpenGL.ShaderType

    shaderType match {
      case "vs" | "vert" => Success(ShaderType.Vertex)
      case "fs" | "frag" => Success(ShaderType.Fragment)
      case "gs" | "geom" => Success(ShaderType.Geometry)
      case "tc"          => Success(ShaderType.TessControl)
      case "te"          => Success(ShaderType.TessEvaluation)
      case _             => Failure(new Exception("Shadertype '" + shaderType + "' not supported"))
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
        getShaderType(shaderType) match
          case Success(t) => loadShader(t, source)
          case Failure(e) => System.err.println(e.getMessage)
      }
    }
    this
  }

  private def loadShader(shaderType: OpenGL.ShaderType, source: String): ShaderBuilder = {
    val shaderID = OpenGL.glCreateShader(shaderType)

    OpenGL.glShaderSource(shaderID, header + source)
    OpenGL.glCompileShader(shaderID)
    if (!OpenGL.glGetShaderBoolProp(shaderID, OpenGL.ShaderIntProp.CompileStatus)) {
      import OpenGL.ShaderType

      val shaderTypeName = shaderType match
        case ShaderType.Vertex   => "Vertexshader"
        case ShaderType.Fragment => "Fragmentshader"
        case _                   => "Shader"

      val maxLen = math.max(OpenGL.glGetShaderIntProp(shaderID, OpenGL.ShaderIntProp.InfoLogLength), 256)
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

  def linkAndFinish(): OpenGL.ProgramId = {
    OpenGL.glLinkProgram(programID)

    if (!OpenGL.glGetProgramBoolProp(programID, OpenGL.ProgramIntProp.LinkStatus)) {
      val maxLen = math.max(OpenGL.glGetProgramIntProp(programID, OpenGL.ProgramIntProp.InfoLogLength), 256)
      System.err.println("Link error: " + OpenGL.glGetProgramInfoLog(programID, maxLen))
    }

    for (i <- shaders.values) {
      OpenGL.glDetachShader(programID, i)
      OpenGL.glDeleteShader(i)
    }

    programID
  }
}
