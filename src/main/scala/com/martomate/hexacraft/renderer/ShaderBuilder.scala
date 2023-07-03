package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.infra.gpu.OpenGL
import com.martomate.hexacraft.util.{FileUtils, Result}
import com.martomate.hexacraft.util.Result.{Err, Ok}

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

  def setPrefix(newPrefix: String): ShaderBuilder =
    prefix = newPrefix
    this

  def setDefines(defines: Seq[(String, String)]): ShaderBuilder =
    definesText = defines.map(d => s"#define ${d._1} ${d._2}\n").mkString
    this

  private def getShaderType(shaderType: String): Option[OpenGL.ShaderType] =
    import OpenGL.ShaderType

    shaderType match
      case "vs" | "vert" => Some(ShaderType.Vertex)
      case "fs" | "frag" => Some(ShaderType.Fragment)
      case "gs" | "geom" => Some(ShaderType.Geometry)
      case "tc"          => Some(ShaderType.TessControl)
      case "te"          => Some(ShaderType.TessEvaluation)
      case _             => None

  private def header: String = s"#version 330 core\n\n$definesText"

  private def loadSource(path: String): String =
    val source = new mutable.StringBuilder()

    try
      val reader = FileUtils.getBufferedReader(FileUtils.getResourceFile(prefix + path).get)
      reader.lines.forEach(line => source.append(line).append('\n'))
      reader.close()
    catch
      case e: Exception =>
        throw new IOException("Could not load shader at " + (prefix + path), e)

    source.toString

  def loadAll(path: String): ShaderBuilder =
    val s = loadSource(path)
    val stages = extractShaderStages(s)

    val (validStages, invalidStageErrors) = Result.split(stages.toSeq)

    for stageError <- invalidStageErrors do
      System.err.println(s"Shader file $path contained an invalid shader stage: $stageError")

    for (shaderType, source) <- validStages do
      tryLoadShader(shaderType, source) match
        case Ok(shaderId) => shaders.put(shaderType, shaderId)
        case Err(message) => System.err.println(message)

    this

  private def extractShaderStages(s: String) =
    for part <- s.split("#pragma shader ") if part.nonEmpty yield
      val newLineIdx = part.indexOf('\n')
      val shaderTypeName = part.substring(0, newLineIdx)
      val source = part.substring(newLineIdx + 1)

      getShaderType(shaderTypeName) match
        case Some(shaderType) => Ok(shaderType, source)
        case None             => Err("Shader type '" + shaderTypeName + "' not supported")

  private def tryLoadShader(t: OpenGL.ShaderType, source: String) =
    OpenGL
      .loadShader(t, header + source)
      .mapErr(e => s"${nameOfShaderType(t)} failed to compile ($name).\nError log:\n${e.message}")

  private def nameOfShaderType(shaderType: OpenGL.ShaderType) =
    import OpenGL.ShaderType.*

    shaderType match
      case Vertex   => "Vertex shader"
      case Fragment => "Fragment shader"
      case _        => "Shader"

  def bindAttribs(attribs: String*): ShaderBuilder =
    for
      i <- attribs.indices
      if attribs(i) != ""
    do OpenGL.glBindAttribLocation(programID, i, attribs(i))
    this

  def attachAll(): ShaderBuilder =
    for i <- shaders.values
    do OpenGL.glAttachShader(programID, i)
    this

  def linkAndFinish(): OpenGL.ProgramId =
    OpenGL.linkProgram(programID) match
      case Err(errorMessage) =>
        System.err.println("Link error: " + errorMessage)
      case _ =>

    for i <- shaders.values do
      OpenGL.glDetachShader(programID, i)
      OpenGL.unloadShader(i)

    programID
}
