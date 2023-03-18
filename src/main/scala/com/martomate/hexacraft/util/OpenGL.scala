package com.martomate.hexacraft.util

import java.nio.{ByteBuffer, FloatBuffer}
import org.lwjgl.opengl.{
  GL,
  GL11,
  GL12,
  GL13,
  GL15,
  GL20,
  GL30,
  GL31,
  GL33,
  GL43,
  GLCapabilities,
  GLDebugMessageCallbackI
}

object OpenGL {
  def createCapabilities(): GLCapabilities = GL.createCapabilities()

  def getCapabilities: GLCapabilities = GL.getCapabilities

  def glUseProgram(program: Int): Unit = GL20.glUseProgram(program)

  def glCreateProgram(): Int = GL20.glCreateProgram()

  def glLinkProgram(program: Int): Unit = GL20.glLinkProgram(program)

  def glDeleteProgram(program: Int): Unit = GL20.glDeleteProgram(program)

  def glCreateShader(shaderType: Int): Int = GL20.glCreateShader(shaderType)

  def glShaderSource(shader: Int, string: String): Unit = GL20.glShaderSource(shader, string)

  def glCompileShader(shader: Int): Unit = GL20.glCompileShader(shader)

  def glGetShaderi(shader: Int, pname: Int): Int = GL20.glGetShaderi(shader, pname)

  def glGetProgrami(program: Int, pname: Int): Int = GL20.glGetProgrami(program, pname)

  def glGetShaderInfoLog(shader: Int, maxLength: Int): String = GL20.glGetShaderInfoLog(shader, maxLength)

  def glGetProgramInfoLog(program: Int, maxLength: Int): String = GL20.glGetProgramInfoLog(program, maxLength)

  def glAttachShader(program: Int, shader: Int): Unit = GL20.glAttachShader(program, shader)

  def glDetachShader(program: Int, shader: Int): Unit = GL20.glDetachShader(program, shader)

  def glDeleteShader(shader: Int): Unit = GL20.glDeleteShader(shader)

  def glGetUniformLocation(program: Int, name: String): Int = GL20.glGetUniformLocation(program, name)

  def glGetAttribLocation(program: Int, name: String): Int = GL20.glGetAttribLocation(program, name)

  def glBindAttribLocation(program: Int, index: Int, name: String): Unit =
    GL20.glBindAttribLocation(program, index, name)

  def glUniform1i(location: Int, v0: Int): Unit = GL20.glUniform1i(location, v0)

  def glUniform1f(location: Int, v0: Float): Unit = GL20.glUniform1f(location, v0)

  def glUniform2f(location: Int, v0: Float, v1: Float): Unit = GL20.glUniform2f(location, v0, v1)

  def glUniform3f(location: Int, v0: Float, v1: Float, v2: Float): Unit = GL20.glUniform3f(location, v0, v1, v2)

  def glUniform4f(location: Int, v0: Float, v1: Float, v2: Float, v3: Float): Unit =
    GL20.glUniform4f(location, v0, v1, v2, v3)

  def glUniformMatrix4fv(location: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GL20.glUniformMatrix4fv(location, transpose, value)

  def glViewport(x: Int, y: Int, w: Int, h: Int): Unit = GL11.glViewport(x, y, w, h)

  def glGenFramebuffers(): Int = GL30.glGenFramebuffers()

  def glBindFramebuffer(target: Int, framebuffer: Int): Unit = GL30.glBindFramebuffer(target, framebuffer)

  def glDeleteFramebuffers(framebuffer: Int): Unit = GL30.glDeleteFramebuffers(framebuffer)

  def glEnable(target: Int): Unit = GL11.glEnable(target)

  def glDisable(target: Int): Unit = GL11.glDisable(target)

  def glDrawArrays(mode: Int, first: Int, count: Int): Unit = GL11.glDrawArrays(mode, first, count)

  def glDrawArraysInstanced(mode: Int, first: Int, count: Int, primcount: Int): Unit =
    GL31.glDrawArraysInstanced(mode, first, count, primcount)

  def glBindTexture(target: Int, texture: Int): Unit = GL11.glBindTexture(target, texture)

  def glGenTextures(): Int = GL11.glGenTextures()

  def glTexImage3D(
      target: Int,
      level: Int,
      internalFormat: Int,
      width: Int,
      height: Int,
      depth: Int,
      border: Int,
      format: Int,
      texelDataType: Int,
      pixels: ByteBuffer
  ): Unit =
    GL12.glTexImage3D(target, level, internalFormat, width, height, depth, border, format, texelDataType, pixels)

  def glTexParameteri(target: Int, pname: Int, param: Int): Unit = GL11.glTexParameteri(target, pname, param)

  def glGenerateMipmap(target: Int): Unit = GL30.glGenerateMipmap(target)

  def glDeleteTextures(texture: Int): Unit = GL11.glDeleteTextures(texture)

  def glTexImage2D(
      target: Int,
      level: Int,
      internalFormat: Int,
      width: Int,
      height: Int,
      border: Int,
      format: Int,
      texelDataType: Int,
      pixels: ByteBuffer
  ): Unit = GL11.glTexImage2D(target, level, internalFormat, width, height, border, format, texelDataType, pixels)

  def glBindVertexArray(array: Int): Unit = GL30.glBindVertexArray(array)

  def glDeleteVertexArrays(array: Int): Unit = GL30.glDeleteVertexArrays(array)

  def glGenVertexArrays(): Int = GL30.glGenVertexArrays()

  def glBindBuffer(target: Int, buffer: Int): Unit = GL15.glBindBuffer(target, buffer)

  def glCopyBufferSubData(readTarget: Int, writeTarget: Int, readOffset: Long, writeOffset: Long, size: Long): Unit =
    GL31.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size)

  def glBufferData(target: Int, size: Long, usage: Int): Unit = GL15.glBufferData(target, size, usage)

  def glBufferSubData(target: Int, offset: Long, data: ByteBuffer): Unit = GL15.glBufferSubData(target, offset, data)

  def glDeleteBuffers(buffer: Int): Unit = GL15.glDeleteBuffers(buffer)

  def glGenBuffers(): Int = GL15.glGenBuffers()

  def glEnableVertexAttribArray(index: Int): Unit = GL20.glEnableVertexAttribArray(index)

  def glVertexAttribPointer(
      index: Int,
      size: Int,
      dataType: Int,
      normalized: Boolean,
      stride: Int,
      pointer: Long
  ): Unit = GL20.glVertexAttribPointer(index, size, dataType, normalized, stride, pointer)

  def glVertexAttribIPointer(index: Int, size: Int, dataType: Int, stride: Int, pointer: Long): Unit =
    GL30.glVertexAttribIPointer(index, size, dataType, stride, pointer)

  def glVertexAttribDivisor(index: Int, divisor: Int): Unit = GL33.glVertexAttribDivisor(index, divisor)

  def glActiveTexture(texture: Int): Unit = GL13.glActiveTexture(texture)

  def glBlendFunc(sfactor: Int, dfactor: Int): Unit = GL11.glBlendFunc(sfactor, dfactor)

  def glScissor(x: Int, y: Int, width: Int, height: Int): Unit = GL11.glScissor(x, y, width, height)

  def glDebugMessageCallback(callback: GLDebugMessageCallbackI, userParam: Long): Unit =
    GL43.glDebugMessageCallback(callback, userParam)

  def glClear(mask: Int): Unit = GL11.glClear(mask)

  def glGetError(): Int = GL11.glGetError()

  def glDepthFunc(func: Int): Unit = GL11.glDepthFunc(func)
}
