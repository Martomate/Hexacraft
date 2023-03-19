package com.martomate.hexacraft.util

import java.nio.{ByteBuffer, FloatBuffer}
import org.lwjgl.opengl.*
import org.lwjgl.system.MemoryUtil
import scala.annotation.targetName

object OpenGL {
  enum ShaderType {
    case Vertex
    case Fragment
    case Geometry
    case TessControl
    case TessEvaluation

    def toGL: Int = this match
      case ShaderType.Vertex         => GL20.GL_VERTEX_SHADER
      case ShaderType.Fragment       => GL20.GL_FRAGMENT_SHADER
      case ShaderType.Geometry       => GL32.GL_GEOMETRY_SHADER
      case ShaderType.TessControl    => GL40.GL_TESS_CONTROL_SHADER
      case ShaderType.TessEvaluation => GL40.GL_TESS_EVALUATION_SHADER
  }

  enum ShaderIntProp {
    case CompileStatus
    case InfoLogLength

    def toGL: Int = this match
      case CompileStatus => GL20.GL_COMPILE_STATUS
      case InfoLogLength => GL20.GL_INFO_LOG_LENGTH
  }

  def createCapabilities(): GLCapabilities = GL.createCapabilities()

  def getCapabilities: GLCapabilities = GL.getCapabilities

  def hasDebugExtension: Boolean = getCapabilities.GL_KHR_debug

  // Shaders

  opaque type ShaderId = Int

  def glCreateShader(shaderType: ShaderType): ShaderId =
    GL20.glCreateShader(shaderType.toGL)

  def glShaderSource(shader: ShaderId, string: String): Unit = GL20.glShaderSource(shader, string)

  def glCompileShader(shader: ShaderId): Unit = GL20.glCompileShader(shader)

  def glGetShaderIntProp(shader: ShaderId, prop: ShaderIntProp): Int = GL20.glGetShaderi(shader, prop.toGL)

  def glGetShaderBoolProp(shader: ShaderId, prop: ShaderIntProp): Boolean =
    glGetShaderIntProp(shader, prop) == GL11.GL_TRUE

  def glGetShaderInfoLog(shader: ShaderId, maxLength: Int): String = GL20.glGetShaderInfoLog(shader, maxLength)

  def glDeleteShader(shader: ShaderId): Unit = GL20.glDeleteShader(shader)

  // Programs

  opaque type ProgramId = Int
  object ProgramId {
    def none: ProgramId = 0
  }

  enum ProgramIntProp {
    case LinkStatus
    case InfoLogLength

    def toGL: Int = this match
      case LinkStatus    => GL20.GL_LINK_STATUS
      case InfoLogLength => GL20.GL_INFO_LOG_LENGTH
  }

  opaque type UniformLocation = Int
  object UniformLocation {
    extension (loc: UniformLocation) def exists: Boolean = loc != -1
  }

  def glCreateProgram(): ProgramId = GL20.glCreateProgram()

  def glUseProgram(program: ProgramId): Unit = GL20.glUseProgram(program)

  def glLinkProgram(program: ProgramId): Unit = GL20.glLinkProgram(program)

  def glDeleteProgram(program: ProgramId): Unit = GL20.glDeleteProgram(program)

  def glGetProgramInfoLog(program: ProgramId, maxLength: Int): String = GL20.glGetProgramInfoLog(program, maxLength)

  def glAttachShader(program: ProgramId, shader: ShaderId): Unit = GL20.glAttachShader(program, shader)

  def glDetachShader(program: ProgramId, shader: ShaderId): Unit = GL20.glDetachShader(program, shader)

  def glGetUniformLocation(program: ProgramId, name: String): UniformLocation = GL20.glGetUniformLocation(program, name)

  def glGetAttribLocation(program: ProgramId, name: String): Int = GL20.glGetAttribLocation(program, name)

  def glBindAttribLocation(program: ProgramId, index: Int, name: String): Unit =
    GL20.glBindAttribLocation(program, index, name)

  def glGetProgramIntProp(program: ProgramId, pname: ProgramIntProp): Int = GL20.glGetProgrami(program, pname.toGL)

  def glGetProgramBoolProp(program: ProgramId, pname: ProgramIntProp): Boolean =
    glGetProgramIntProp(program, pname) == GL11.GL_TRUE

  // Uniforms

  def glUniform1i(location: UniformLocation, v0: Int): Unit = GL20.glUniform1i(location, v0)

  def glUniform1f(location: UniformLocation, v0: Float): Unit = GL20.glUniform1f(location, v0)

  def glUniform2f(location: UniformLocation, v0: Float, v1: Float): Unit = GL20.glUniform2f(location, v0, v1)

  def glUniform3f(location: UniformLocation, v0: Float, v1: Float, v2: Float): Unit =
    GL20.glUniform3f(location, v0, v1, v2)

  def glUniform4f(location: UniformLocation, v0: Float, v1: Float, v2: Float, v3: Float): Unit =
    GL20.glUniform4f(location, v0, v1, v2, v3)

  def glUniformMatrix4fv(location: UniformLocation, transpose: Boolean, value: FloatBuffer): Unit =
    GL20.glUniformMatrix4fv(location, transpose, value)

  // Frame buffers

  opaque type FrameBufferId = Int
  object FrameBufferId {
    def none: FrameBufferId = 0
  }

  enum FrameBufferTarget {
    case Regular

    def toGL: Int = this match
      case Regular => GL30.GL_FRAMEBUFFER
  }

  enum FrameBufferAttachment {
    case ColorAttachment(index: Int)
    case DepthAttachment

    def toGL: Int = this match
      case FrameBufferAttachment.ColorAttachment(index) => GL30.GL_COLOR_ATTACHMENT0 + index
      case FrameBufferAttachment.DepthAttachment        => GL30.GL_DEPTH_ATTACHMENT
  }

  def glGenFramebuffer(): FrameBufferId = GL30.glGenFramebuffers()

  def glBindFramebuffer(target: FrameBufferTarget, framebuffer: FrameBufferId): Unit =
    GL30.glBindFramebuffer(target.toGL, framebuffer)

  def glDrawBuffer(buf: FrameBufferAttachment): Unit = GL11.glDrawBuffer(buf.toGL)

  def glFramebufferTexture(
      target: FrameBufferTarget,
      attachment: FrameBufferAttachment,
      texture: TextureId,
      level: Int
  ): Unit =
    GL32.glFramebufferTexture(target.toGL, attachment.toGL, texture, level)

  def glDeleteFramebuffer(framebuffer: FrameBufferId): Unit = GL30.glDeleteFramebuffers(framebuffer)

  // Drawing

  enum PrimitiveMode {
    case Lines
    case LineStrip
    case Triangles
    case TriangleStrip

    def toGL: Int = this match
      case PrimitiveMode.Lines         => GL11.GL_LINES
      case PrimitiveMode.LineStrip     => GL11.GL_LINE_STRIP
      case PrimitiveMode.Triangles     => GL11.GL_TRIANGLES
      case PrimitiveMode.TriangleStrip => GL11.GL_TRIANGLE_STRIP
  }

  def glDrawArrays(mode: PrimitiveMode, first: Int, count: Int): Unit = GL11.glDrawArrays(mode.toGL, first, count)

  def glDrawArraysInstanced(mode: PrimitiveMode, first: Int, count: Int, primcount: Int): Unit =
    GL31.glDrawArraysInstanced(mode.toGL, first, count, primcount)

  // Textures

  opaque type TextureId = Int
  object TextureId {
    def none: TextureId = 0
  }

  enum TextureTarget {
    case Texture2D
    case Texture2DArray

    def toGL: Int = this match
      case TextureTarget.Texture2D      => GL11.GL_TEXTURE_2D
      case TextureTarget.Texture2DArray => GL30.GL_TEXTURE_2D_ARRAY
  }

  enum TextureInternalFormat {
    case Rgba
    case DepthComponent32

    def toGL: Int = this match
      case TextureInternalFormat.Rgba             => GL11.GL_RGBA
      case TextureInternalFormat.DepthComponent32 => GL14.GL_DEPTH_COMPONENT32
  }

  enum TexelDataFormat {
    case Rgba
    case DepthComponent

    def toGL: Int = this match
      case TexelDataFormat.Rgba           => GL11.GL_RGBA
      case TexelDataFormat.DepthComponent => GL11.GL_DEPTH_COMPONENT
  }

  enum TexelDataType {
    case UnsignedByte
    case Float

    def toGL: Int = this match
      case TexelDataType.UnsignedByte => GL11.GL_UNSIGNED_BYTE
      case TexelDataType.Float        => GL11.GL_FLOAT
  }

  def glGenTextures(): TextureId = GL11.glGenTextures()

  def glBindTexture(target: TextureTarget, texture: TextureId): Unit = GL11.glBindTexture(target.toGL, texture)

  def glTexImage2D(
      target: TextureTarget,
      level: Int,
      internalFormat: TextureInternalFormat,
      width: Int,
      height: Int,
      border: Int,
      format: TexelDataFormat,
      texelDataType: TexelDataType,
      pixels: ByteBuffer
  ): Unit =
    GL11.glTexImage2D(
      target.toGL,
      level,
      internalFormat.toGL,
      width,
      height,
      border,
      format.toGL,
      texelDataType.toGL,
      pixels
    )

  def glTexImage2D(
      target: TextureTarget,
      level: Int,
      internalFormat: TextureInternalFormat,
      width: Int,
      height: Int,
      border: Int,
      format: TexelDataFormat,
      texelDataType: TexelDataType,
      pixels: FloatBuffer
  ): Unit =
    GL11.glTexImage2D(
      target.toGL,
      level,
      internalFormat.toGL,
      width,
      height,
      border,
      format.toGL,
      texelDataType.toGL,
      pixels
    )

  def glTexImage3D(
      target: TextureTarget,
      level: Int,
      internalFormat: TextureInternalFormat,
      width: Int,
      height: Int,
      depth: Int,
      border: Int,
      format: TexelDataFormat,
      texelDataType: TexelDataType,
      pixels: ByteBuffer
  ): Unit =
    GL12.glTexImage3D(
      target.toGL,
      level,
      internalFormat.toGL,
      width,
      height,
      depth,
      border,
      format.toGL,
      texelDataType.toGL,
      pixels
    )

  enum TexMagFilter {
    case Linear
    case Nearest

    def toGL: Int = this match
      case TexMagFilter.Linear  => GL11.GL_LINEAR
      case TexMagFilter.Nearest => GL11.GL_NEAREST
  }

  enum TexMinFilter {
    case Linear
    case Nearest
    case NearestMipmapNearest
    case LinearMipmapNearest
    case NearestMipmapLinear
    case LinearMipmapLinear

    def toGL: Int = this match
      case TexMinFilter.Linear               => GL11.GL_LINEAR
      case TexMinFilter.Nearest              => GL11.GL_NEAREST
      case TexMinFilter.NearestMipmapNearest => GL11.GL_NEAREST_MIPMAP_NEAREST
      case TexMinFilter.LinearMipmapNearest  => GL11.GL_LINEAR_MIPMAP_NEAREST
      case TexMinFilter.NearestMipmapLinear  => GL11.GL_NEAREST_MIPMAP_LINEAR
      case TexMinFilter.LinearMipmapLinear   => GL11.GL_LINEAR_MIPMAP_LINEAR
  }

  enum TexWrap {
    case ClampToEdge
    case ClampToBorder
    case MirroredRepeat
    case Repeat

    def toGL: Int = this match
      case TexWrap.ClampToEdge    => GL12.GL_CLAMP_TO_EDGE
      case TexWrap.ClampToBorder  => GL13.GL_CLAMP_TO_BORDER
      case TexWrap.MirroredRepeat => GL14.GL_MIRRORED_REPEAT
      case TexWrap.Repeat         => GL11.GL_REPEAT
  }

  enum TexIntParameter {
    case MagFilter(filter: TexMagFilter)
    case MinFilter(filter: TexMinFilter)
    case TextureWrapS(wrap: TexWrap)
    case TextureWrapT(wrap: TexWrap)
    case TextureWrapR(wrap: TexWrap)

    def toGL: (Int, Int) = this match
      case TexIntParameter.MagFilter(filter)  => (GL11.GL_TEXTURE_MAG_FILTER, filter.toGL)
      case TexIntParameter.MinFilter(filter)  => (GL11.GL_TEXTURE_MIN_FILTER, filter.toGL)
      case TexIntParameter.TextureWrapS(wrap) => (GL11.GL_TEXTURE_WRAP_S, wrap.toGL)
      case TexIntParameter.TextureWrapT(wrap) => (GL11.GL_TEXTURE_WRAP_T, wrap.toGL)
      case TexIntParameter.TextureWrapR(wrap) => (GL12.GL_TEXTURE_WRAP_R, wrap.toGL)
  }

  def glTexParameteri(target: TextureTarget, p: TexIntParameter): Unit =
    val (pname, param) = p.toGL
    GL11.glTexParameteri(target.toGL, pname, param)

  def glGenerateMipmap(target: TextureTarget): Unit = GL30.glGenerateMipmap(target.toGL)

  opaque type TextureSlot = Int
  object TextureSlot {
    def ofSlot(slot: Int): TextureSlot =
      require(slot >= 0)
      require(slot < 32)
      GL13.GL_TEXTURE0 + slot
  }

  def glActiveTexture(textureSlot: TextureSlot): Unit = GL13.glActiveTexture(textureSlot)

  def glDeleteTextures(texture: TextureId): Unit = GL11.glDeleteTextures(texture)

  // Vertex arrays

  opaque type VertexArrayId = Int
  object VertexArrayId {
    def none: VertexArrayId = 0
  }

  def glGenVertexArrays(): VertexArrayId = GL30.glGenVertexArrays()

  def glBindVertexArray(array: VertexArrayId): Unit = GL30.glBindVertexArray(array)

  def glDeleteVertexArrays(array: VertexArrayId): Unit = GL30.glDeleteVertexArrays(array)

  // Vertex buffers

  opaque type VertexBufferId = Int

  enum VertexBufferTarget {
    case ArrayBuffer
    case CopyReadBuffer
    case CopyWriteBuffer

    def toGL: Int = this match
      case VertexBufferTarget.ArrayBuffer     => GL15.GL_ARRAY_BUFFER
      case VertexBufferTarget.CopyReadBuffer  => GL31.GL_COPY_READ_BUFFER
      case VertexBufferTarget.CopyWriteBuffer => GL31.GL_COPY_WRITE_BUFFER
  }

  enum VboUsage {
    case StaticDraw
    case DynamicDraw

    def toGL: Int = this match
      case VboUsage.StaticDraw  => GL15.GL_STATIC_DRAW
      case VboUsage.DynamicDraw => GL15.GL_DYNAMIC_DRAW
  }

  def glGenBuffers(): VertexBufferId = GL15.glGenBuffers()

  def glBindBuffer(target: VertexBufferTarget, buffer: VertexBufferId): Unit = GL15.glBindBuffer(target.toGL, buffer)

  def glBufferData(target: VertexBufferTarget, size: Long, usage: VboUsage): Unit =
    GL15.glBufferData(target.toGL, size, usage.toGL)

  def glBufferSubData(target: VertexBufferTarget, offset: Long, data: ByteBuffer): Unit =
    GL15.glBufferSubData(target.toGL, offset, data)

  def glCopyBufferSubData(
      readTarget: VertexBufferTarget,
      writeTarget: VertexBufferTarget,
      readOffset: Long,
      writeOffset: Long,
      size: Long
  ): Unit =
    GL31.glCopyBufferSubData(readTarget.toGL, writeTarget.toGL, readOffset, writeOffset, size)

  def glDeleteBuffers(buffer: VertexBufferId): Unit = GL15.glDeleteBuffers(buffer)

  // Vertex attributes

  def glEnableVertexAttribArray(index: Int): Unit = GL20.glEnableVertexAttribArray(index)

  enum VertexAttributeDataType {
    case Int
    case Float

    def toGL: Int = this match
      case VertexAttributeDataType.Int   => GL11.GL_INT
      case VertexAttributeDataType.Float => GL11.GL_FLOAT
  }

  enum VertexIntAttributeDataType {
    case Int

    def toGL: Int = this match
      case VertexIntAttributeDataType.Int => GL11.GL_INT
  }

  def glVertexAttribPointer(
      index: Int,
      size: Int,
      dataType: VertexAttributeDataType,
      normalized: Boolean,
      stride: Int,
      pointer: Long
  ): Unit = GL20.glVertexAttribPointer(index, size, dataType.toGL, normalized, stride, pointer)

  def glVertexAttribIPointer(
      index: Int,
      size: Int,
      dataType: VertexIntAttributeDataType,
      stride: Int,
      pointer: Long
  ): Unit =
    GL30.glVertexAttribIPointer(index, size, dataType.toGL, stride, pointer)

  def glVertexAttribDivisor(index: Int, divisor: Int): Unit = GL33.glVertexAttribDivisor(index, divisor)

  // Misc

  enum State {
    case Blend
    case DepthTest
    case ScissorTest
    case CullFace
    case DebugOutput
    case MultiSample

    def toGL: Int = this match
      case State.Blend       => GL11.GL_BLEND
      case State.DepthTest   => GL11.GL_DEPTH_TEST
      case State.ScissorTest => GL11.GL_SCISSOR_TEST
      case State.CullFace    => GL11.GL_CULL_FACE
      case State.DebugOutput => GL43.GL_DEBUG_OUTPUT
      case State.MultiSample => GL13.GL_MULTISAMPLE
  }

  enum BlendFactor {
    case SrcAlpha
    case OneMinusSrcAlpha

    def toGL: Int = this match
      case BlendFactor.SrcAlpha         => GL11.GL_SRC_ALPHA
      case BlendFactor.OneMinusSrcAlpha => GL11.GL_ONE_MINUS_SRC_ALPHA
  }

  enum DepthFunc {
    case LessThanOrEqual

    def toGL: Int = this match
      case LessThanOrEqual => GL11.GL_LEQUAL
  }

  opaque type ClearMask = Int
  object ClearMask {
    def colorBuffer: ClearMask = GL11.GL_COLOR_BUFFER_BIT

    def depthBuffer: ClearMask = GL11.GL_DEPTH_BUFFER_BIT

    extension (l: ClearMask)
      @targetName("with")
      def |(r: ClearMask): ClearMask = l | r
  }

  def glEnable(target: State): Unit = GL11.glEnable(target.toGL)

  def glDisable(target: State): Unit = GL11.glDisable(target.toGL)

  def glViewport(x: Int, y: Int, width: Int, height: Int): Unit = GL11.glViewport(x, y, width, height)

  def glScissor(x: Int, y: Int, width: Int, height: Int): Unit = GL11.glScissor(x, y, width, height)

  def glBlendFunc(sfactor: BlendFactor, dfactor: BlendFactor): Unit = GL11.glBlendFunc(sfactor.toGL, dfactor.toGL)

  def glDepthFunc(func: DepthFunc): Unit = GL11.glDepthFunc(func.toGL)

  def glClear(mask: ClearMask): Unit = GL11.glClear(mask)

  def glGetError(): Option[Int] =
    val e = GL11.glGetError()
    if e == GL11.GL_NO_ERROR then None else Some(e)

  // Debug

  enum DebugMessageSource {
    case Api
    case WindowSystem
    case ShaderCompiler
    case ThirdParty
    case Application
    case Other
    case Unknown(code: Int)
  }
  object DebugMessageSource {
    def fromGL(code: Int): DebugMessageSource = code match
      case GL43.GL_DEBUG_SOURCE_API             => DebugMessageSource.Api
      case GL43.GL_DEBUG_SOURCE_WINDOW_SYSTEM   => DebugMessageSource.WindowSystem
      case GL43.GL_DEBUG_SOURCE_SHADER_COMPILER => DebugMessageSource.ShaderCompiler
      case GL43.GL_DEBUG_SOURCE_THIRD_PARTY     => DebugMessageSource.ThirdParty
      case GL43.GL_DEBUG_SOURCE_APPLICATION     => DebugMessageSource.Application
      case GL43.GL_DEBUG_SOURCE_OTHER           => DebugMessageSource.Other
      case _                                    => DebugMessageSource.Unknown(code)
  }

  enum DebugMessageType {
    case Error
    case DeprecatedBehavior
    case UndefinedBehavior
    case Portability
    case Performance
    case Marker
    case PushGroup
    case PopGroup
    case Other
    case Unknown(code: Int)
  }
  object DebugMessageType {
    def fromGL(code: Int): DebugMessageType = code match
      case GL43.GL_DEBUG_TYPE_ERROR               => DebugMessageType.Error
      case GL43.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR => DebugMessageType.DeprecatedBehavior
      case GL43.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR  => DebugMessageType.UndefinedBehavior
      case GL43.GL_DEBUG_TYPE_PORTABILITY         => DebugMessageType.Portability
      case GL43.GL_DEBUG_TYPE_PERFORMANCE         => DebugMessageType.Performance
      case GL43.GL_DEBUG_TYPE_MARKER              => DebugMessageType.Marker
      case GL43.GL_DEBUG_TYPE_PUSH_GROUP          => DebugMessageType.PushGroup
      case GL43.GL_DEBUG_TYPE_POP_GROUP           => DebugMessageType.PopGroup
      case GL43.GL_DEBUG_TYPE_OTHER               => DebugMessageType.Other
      case _                                      => DebugMessageType.Unknown(code)
  }

  enum DebugMessageSeverity {
    case High
    case Medium
    case Low
    case Notification
    case Unknown(code: Int)
  }
  object DebugMessageSeverity {
    def fromGL(code: Int): DebugMessageSeverity = code match
      case GL43.GL_DEBUG_SEVERITY_HIGH         => DebugMessageSeverity.High
      case GL43.GL_DEBUG_SEVERITY_MEDIUM       => DebugMessageSeverity.Medium
      case GL43.GL_DEBUG_SEVERITY_LOW          => DebugMessageSeverity.Low
      case GL43.GL_DEBUG_SEVERITY_NOTIFICATION => DebugMessageSeverity.Notification
      case _                                   => DebugMessageSeverity.Unknown(code)
  }

  case class DebugMessage(
      source: DebugMessageSource,
      debugType: DebugMessageType,
      id: Int,
      severity: DebugMessageSeverity,
      message: String,
      userParam: Long
  )

  def glDebugMessageCallback(callback: DebugMessage => Unit, userParam: Long): Unit =
    val glCallback: GLDebugMessageCallbackI = (source, debugType, id, severity, length, messageAddress, userParam) => {
      val message =
        if length < 0
        then MemoryUtil.memASCII(messageAddress)
        else MemoryUtil.memASCII(messageAddress, length)

      val debugMessage = DebugMessage(
        OpenGL.DebugMessageSource.fromGL(source),
        OpenGL.DebugMessageType.fromGL(debugType),
        id,
        OpenGL.DebugMessageSeverity.fromGL(severity),
        message,
        userParam
      )

      callback(debugMessage)
    }
    GL43.glDebugMessageCallback(glCallback, userParam)
}
