package hexacraft.infra.gpu

import hexacraft.infra.*
import hexacraft.infra.os.{Mac, OS}
import hexacraft.util.*
import hexacraft.util.Result.{Err, Ok}

import org.lwjgl.opengl.*
import org.lwjgl.system.MemoryUtil

import java.nio.{ByteBuffer, FloatBuffer}
import scala.annotation.targetName

object OpenGL {
  enum Event {
    case ShaderLoaded(shaderId: ShaderId, shaderType: ShaderType, source: String)
    case ShaderUnloaded(shaderId: ShaderId)
    case ProgramCreated(programId: ProgramId)
    case ProgramDeleted(programId: ProgramId)
  }

  private var gl: GLWrapper = RealGL

  private val dispatcher = new EventDispatcher[Event]
  def trackEvents(tracker: Tracker[Event]): Unit = dispatcher.track(tracker)

  def _enterTestMode(): Unit = {
    gl = new StubGL
  }

  // ---- Implementation ----

  enum ShaderType {
    case Vertex
    case Fragment
    case Geometry
    case TessControl
    case TessEvaluation

    def toGL: Int = this match {
      case ShaderType.Vertex         => GL20.GL_VERTEX_SHADER
      case ShaderType.Fragment       => GL20.GL_FRAGMENT_SHADER
      case ShaderType.Geometry       => GL32.GL_GEOMETRY_SHADER
      case ShaderType.TessControl    => GL40.GL_TESS_CONTROL_SHADER
      case ShaderType.TessEvaluation => GL40.GL_TESS_EVALUATION_SHADER
    }
  }

  def createCapabilities(): Unit = {
    gl.createCapabilities()
  }

  def hasDebugExtension: Boolean = {
    gl.getCapabilities.GL_KHR_debug
  }

  // Shaders

  opaque type ShaderId = Int
  object ShaderId {
    given Ordering[ShaderId] = Ordering.Int
  }
  case class ShaderCompilationError(message: String)

  def loadShader(shaderType: ShaderType, shaderSource: String): Result[ShaderId, ShaderCompilationError] = {
    val shaderId = gl.glCreateShader(shaderType.toGL)
    gl.glShaderSource(shaderId, shaderSource)
    gl.glCompileShader(shaderId)

    if gl.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) != GL11.GL_TRUE then {
      val maxLen = math.max(gl.glGetShaderi(shaderId, GL20.GL_INFO_LOG_LENGTH), 256)
      val errorMessage = gl.glGetShaderInfoLog(shaderId, maxLen)

      gl.glDeleteShader(shaderId)
      Err(ShaderCompilationError(errorMessage))
    } else {
      dispatcher.notify(Event.ShaderLoaded(shaderId, shaderType, shaderSource))
      Ok(shaderId)
    }
  }

  def unloadShader(shader: ShaderId): Unit = {
    gl.glDeleteShader(shader)
    dispatcher.notify(Event.ShaderUnloaded(shader))
  }

  // Programs

  opaque type ProgramId = Int
  object ProgramId {
    def none: ProgramId = 0

    given Ordering[ProgramId] = Ordering.Int
  }

  opaque type UniformLocation = Int
  object UniformLocation {
    extension (loc: UniformLocation) {
      def exists: Boolean = loc != -1
    }
  }

  def createProgram(): ProgramId = {
    val id = gl.glCreateProgram()
    dispatcher.notify(Event.ProgramCreated(id))
    id
  }

  def glUseProgram(program: ProgramId): Unit = {
    gl.glUseProgram(program)
  }

  def linkProgram(programId: ProgramId): Result[Unit, String] = {
    gl.glLinkProgram(programId)

    if gl.glGetProgrami(programId, GL20.GL_LINK_STATUS) != GL11.GL_TRUE then {
      val maxLen = math.max(gl.glGetProgrami(programId, GL20.GL_INFO_LOG_LENGTH), 256)
      val errorMessage = gl.glGetProgramInfoLog(programId, maxLen)

      return Err(errorMessage)
    }

    Ok(())
  }

  def deleteProgram(id: ProgramId): Unit = {
    gl.glDeleteProgram(id)
    dispatcher.notify(Event.ProgramDeleted(id))
  }

  def glAttachShader(program: ProgramId, shader: ShaderId): Unit = {
    gl.glAttachShader(program, shader)
  }

  def glDetachShader(program: ProgramId, shader: ShaderId): Unit = {
    gl.glDetachShader(program, shader)
  }

  def glGetUniformLocation(program: ProgramId, name: String): UniformLocation = {
    gl.glGetUniformLocation(program, name)
  }

  def glGetAttribLocation(program: ProgramId, name: String): Int = {
    gl.glGetAttribLocation(program, name)
  }

  def glBindAttribLocation(program: ProgramId, index: Int, name: String): Unit = {
    gl.glBindAttribLocation(program, index, name)
  }

  // Uniforms

  def glUniform1i(location: UniformLocation, v0: Int): Unit = {
    gl.glUniform1i(location, v0)
  }

  def glUniform1f(location: UniformLocation, v0: Float): Unit = {
    gl.glUniform1f(location, v0)
  }

  def glUniform2f(location: UniformLocation, v0: Float, v1: Float): Unit = {
    gl.glUniform2f(location, v0, v1)
  }

  def glUniform3f(location: UniformLocation, v0: Float, v1: Float, v2: Float): Unit = {
    gl.glUniform3f(location, v0, v1, v2)
  }

  def glUniform4f(location: UniformLocation, v0: Float, v1: Float, v2: Float, v3: Float): Unit = {
    gl.glUniform4f(location, v0, v1, v2, v3)
  }

  def glUniformMatrix4fv(location: UniformLocation, transpose: Boolean, value: FloatBuffer): Unit = {
    gl.glUniformMatrix4fv(location, transpose, value)
  }

  // Frame buffers

  opaque type FrameBufferId = Int
  object FrameBufferId {
    def none: FrameBufferId = 0
  }

  enum FrameBufferTarget {
    case Regular

    def toGL: Int = this match {
      case Regular => GL30.GL_FRAMEBUFFER
    }
  }

  enum FrameBufferAttachment {
    case ColorAttachment(index: Int)
    case DepthAttachment

    def toGL: Int = this match {
      case FrameBufferAttachment.ColorAttachment(index) => GL30.GL_COLOR_ATTACHMENT0 + index
      case FrameBufferAttachment.DepthAttachment        => GL30.GL_DEPTH_ATTACHMENT
    }
  }

  def glGenFramebuffer(): FrameBufferId = {
    gl.glGenFramebuffers()
  }

  def glBindFramebuffer(target: FrameBufferTarget, framebuffer: FrameBufferId): Unit = {
    gl.glBindFramebuffer(target.toGL, framebuffer)
  }

  def glDrawBuffer(buf: FrameBufferAttachment): Unit = {
    gl.glDrawBuffer(buf.toGL)
  }

  def glDrawBuffers(buf: Seq[FrameBufferAttachment]): Unit = {
    gl.glDrawBuffers(buf.map(_.toGL).toArray)
  }

  def glFramebufferTexture(
      target: FrameBufferTarget,
      attachment: FrameBufferAttachment,
      texture: TextureId,
      level: Int
  ): Unit = {
    gl.glFramebufferTexture(target.toGL, attachment.toGL, texture, level)
  }

  def glDeleteFramebuffer(framebuffer: FrameBufferId): Unit = {
    gl.glDeleteFramebuffers(framebuffer)
  }

  // Drawing

  enum PrimitiveMode {
    case Lines
    case LineStrip
    case Triangles
    case TriangleStrip

    def toGL: Int = this match {
      case PrimitiveMode.Lines         => GL11.GL_LINES
      case PrimitiveMode.LineStrip     => GL11.GL_LINE_STRIP
      case PrimitiveMode.Triangles     => GL11.GL_TRIANGLES
      case PrimitiveMode.TriangleStrip => GL11.GL_TRIANGLE_STRIP
    }
  }

  def glDrawArrays(mode: PrimitiveMode, first: Int, count: Int): Unit = {
    gl.glDrawArrays(mode.toGL, first, count)
  }

  def glDrawArraysInstanced(mode: PrimitiveMode, first: Int, count: Int, primcount: Int): Unit = {
    gl.glDrawArraysInstanced(mode.toGL, first, count, primcount)
  }

  // Textures

  opaque type TextureId = Int
  object TextureId {
    def none: TextureId = 0
  }

  enum TextureTarget {
    case Texture2D
    case Texture2DArray

    def toGL: Int = this match {
      case TextureTarget.Texture2D      => GL11.GL_TEXTURE_2D
      case TextureTarget.Texture2DArray => GL30.GL_TEXTURE_2D_ARRAY
    }
  }

  enum TextureInternalFormat {
    case Rgba16f
    case Rgba
    case DepthComponent32

    def toGL: Int = this match {
      case TextureInternalFormat.Rgba16f          => GL30.GL_RGBA16F
      case TextureInternalFormat.Rgba             => GL11.GL_RGBA
      case TextureInternalFormat.DepthComponent32 => GL14.GL_DEPTH_COMPONENT32
    }
  }

  enum TexelDataFormat {
    case Rgba
    case DepthComponent

    def toGL: Int = this match {
      case TexelDataFormat.Rgba           => GL11.GL_RGBA
      case TexelDataFormat.DepthComponent => GL11.GL_DEPTH_COMPONENT
    }
  }

  enum TexelDataType {
    case UnsignedByte
    case Float

    def toGL: Int = this match {
      case TexelDataType.UnsignedByte => GL11.GL_UNSIGNED_BYTE
      case TexelDataType.Float        => GL11.GL_FLOAT
    }
  }

  def glGenTextures(): TextureId = {
    gl.glGenTextures()
  }

  def glBindTexture(target: TextureTarget, texture: TextureId): Unit = {
    gl.glBindTexture(target.toGL, texture)
  }

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
  ): Unit = {
    gl.glTexImage2D(
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
  }

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
  ): Unit = {
    gl.glTexImage2D(
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
  }

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
  ): Unit = {
    gl.glTexImage3D(
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
  }

  enum TexMagFilter {
    case Linear
    case Nearest

    def toGL: Int = this match {
      case TexMagFilter.Linear  => GL11.GL_LINEAR
      case TexMagFilter.Nearest => GL11.GL_NEAREST
    }
  }

  enum TexMinFilter {
    case Linear
    case Nearest
    case NearestMipmapNearest
    case LinearMipmapNearest
    case NearestMipmapLinear
    case LinearMipmapLinear

    def toGL: Int = this match {
      case TexMinFilter.Linear               => GL11.GL_LINEAR
      case TexMinFilter.Nearest              => GL11.GL_NEAREST
      case TexMinFilter.NearestMipmapNearest => GL11.GL_NEAREST_MIPMAP_NEAREST
      case TexMinFilter.LinearMipmapNearest  => GL11.GL_LINEAR_MIPMAP_NEAREST
      case TexMinFilter.NearestMipmapLinear  => GL11.GL_NEAREST_MIPMAP_LINEAR
      case TexMinFilter.LinearMipmapLinear   => GL11.GL_LINEAR_MIPMAP_LINEAR
    }
  }

  enum TexWrap {
    case ClampToEdge
    case ClampToBorder
    case MirroredRepeat
    case Repeat

    def toGL: Int = this match {
      case TexWrap.ClampToEdge    => GL12.GL_CLAMP_TO_EDGE
      case TexWrap.ClampToBorder  => GL13.GL_CLAMP_TO_BORDER
      case TexWrap.MirroredRepeat => GL14.GL_MIRRORED_REPEAT
      case TexWrap.Repeat         => GL11.GL_REPEAT
    }
  }

  enum TexIntParameter {
    case MagFilter(filter: TexMagFilter)
    case MinFilter(filter: TexMinFilter)
    case TextureWrapS(wrap: TexWrap)
    case TextureWrapT(wrap: TexWrap)
    case TextureWrapR(wrap: TexWrap)

    def toGL: (Int, Int) = this match {
      case TexIntParameter.MagFilter(filter)  => (GL11.GL_TEXTURE_MAG_FILTER, filter.toGL)
      case TexIntParameter.MinFilter(filter)  => (GL11.GL_TEXTURE_MIN_FILTER, filter.toGL)
      case TexIntParameter.TextureWrapS(wrap) => (GL11.GL_TEXTURE_WRAP_S, wrap.toGL)
      case TexIntParameter.TextureWrapT(wrap) => (GL11.GL_TEXTURE_WRAP_T, wrap.toGL)
      case TexIntParameter.TextureWrapR(wrap) => (GL12.GL_TEXTURE_WRAP_R, wrap.toGL)
    }
  }

  def glTexParameteri(target: TextureTarget, p: TexIntParameter): Unit = {
    val (pname, param) = p.toGL
    gl.glTexParameteri(target.toGL, pname, param)
  }

  def glGenerateMipmap(target: TextureTarget): Unit = {
    gl.glGenerateMipmap(target.toGL)
  }

  opaque type TextureSlot = Int
  object TextureSlot {
    def ofSlot(slot: Int): TextureSlot = {
      require(slot >= 0)
      require(slot < 32)
      GL13.GL_TEXTURE0 + slot
    }
  }

  def glActiveTexture(textureSlot: TextureSlot): Unit = {
    gl.glActiveTexture(textureSlot)
  }

  def glDeleteTextures(texture: TextureId): Unit = {
    gl.glDeleteTextures(texture)
  }

  // Vertex arrays

  opaque type VertexArrayId = Int
  object VertexArrayId {
    def none: VertexArrayId = 0
  }

  def glGenVertexArrays(): VertexArrayId = {
    gl.glGenVertexArrays()
  }

  def glBindVertexArray(array: VertexArrayId): Unit = {
    gl.glBindVertexArray(array)
  }

  def glDeleteVertexArrays(array: VertexArrayId): Unit = {
    gl.glDeleteVertexArrays(array)
  }

  // Vertex buffers

  opaque type VertexBufferId = Int

  enum VertexBufferTarget {
    case ArrayBuffer
    case CopyReadBuffer
    case CopyWriteBuffer

    def toGL: Int = this match {
      case VertexBufferTarget.ArrayBuffer     => GL15.GL_ARRAY_BUFFER
      case VertexBufferTarget.CopyReadBuffer  => GL31.GL_COPY_READ_BUFFER
      case VertexBufferTarget.CopyWriteBuffer => GL31.GL_COPY_WRITE_BUFFER
    }
  }

  enum VboUsage {
    case StaticDraw
    case DynamicDraw

    def toGL: Int = this match {
      case VboUsage.StaticDraw  => GL15.GL_STATIC_DRAW
      case VboUsage.DynamicDraw => GL15.GL_DYNAMIC_DRAW
    }
  }

  def glGenBuffers(): VertexBufferId = {
    gl.glGenBuffers()
  }

  def glBindBuffer(target: VertexBufferTarget, buffer: VertexBufferId): Unit = {
    gl.glBindBuffer(target.toGL, buffer)
  }

  def glBufferData(target: VertexBufferTarget, size: Long, usage: VboUsage): Unit = {
    gl.glBufferData(target.toGL, size, usage.toGL)
  }

  def glBufferSubData(target: VertexBufferTarget, offset: Long, data: ByteBuffer): Unit = {
    gl.glBufferSubData(target.toGL, offset, data)
  }

  def glDeleteBuffers(buffer: VertexBufferId): Unit = {
    gl.glDeleteBuffers(buffer)
  }

  // Vertex attributes

  def glEnableVertexAttribArray(index: Int): Unit = {
    gl.glEnableVertexAttribArray(index)
  }

  enum VertexAttributeDataType {
    case Int
    case Float

    def toGL: Int = this match {
      case VertexAttributeDataType.Int   => GL11.GL_INT
      case VertexAttributeDataType.Float => GL11.GL_FLOAT
    }
  }

  enum VertexIntAttributeDataType {
    case Int

    def toGL: Int = this match {
      case VertexIntAttributeDataType.Int => GL11.GL_INT
    }
  }

  def glVertexAttribPointer(
      index: Int,
      size: Int,
      dataType: VertexAttributeDataType,
      normalized: Boolean,
      stride: Int,
      pointer: Long
  ): Unit = {
    gl.glVertexAttribPointer(index, size, dataType.toGL, normalized, stride, pointer)
  }

  def glVertexAttribIPointer(
      index: Int,
      size: Int,
      dataType: VertexIntAttributeDataType,
      stride: Int,
      pointer: Long
  ): Unit = {
    gl.glVertexAttribIPointer(index, size, dataType.toGL, stride, pointer)
  }

  def glVertexAttribDivisor(index: Int, divisor: Int): Unit = {
    gl.glVertexAttribDivisor(index, divisor)
  }

  // Misc

  enum State {
    case Blend
    case DepthTest
    case ScissorTest
    case CullFace
    case DebugOutput
    case MultiSample

    def toGL: Int = this match {
      case State.Blend       => GL11.GL_BLEND
      case State.DepthTest   => GL11.GL_DEPTH_TEST
      case State.ScissorTest => GL11.GL_SCISSOR_TEST
      case State.CullFace    => GL11.GL_CULL_FACE
      case State.DebugOutput => GL43.GL_DEBUG_OUTPUT
      case State.MultiSample => GL13.GL_MULTISAMPLE
    }
  }

  enum BlendFactor {
    case SrcAlpha
    case OneMinusSrcAlpha

    def toGL: Int = this match {
      case BlendFactor.SrcAlpha         => GL11.GL_SRC_ALPHA
      case BlendFactor.OneMinusSrcAlpha => GL11.GL_ONE_MINUS_SRC_ALPHA
    }
  }

  enum DepthFunc {
    case LessThanOrEqual

    def toGL: Int = this match {
      case LessThanOrEqual => GL11.GL_LEQUAL
    }
  }

  opaque type ClearMask = Int
  object ClearMask {
    def colorBuffer: ClearMask = GL11.GL_COLOR_BUFFER_BIT

    def depthBuffer: ClearMask = GL11.GL_DEPTH_BUFFER_BIT

    extension (l: ClearMask)
      @targetName("with")
      def |(r: ClearMask): ClearMask = l | r
  }

  private val isMac = OS.current == Mac

  def lockContext(): Unit = {
    if isMac then {
      gl.lockContext()
    }
  }

  def unlockContext(): Unit = {
    if isMac then {
      gl.unlockContext()
    }
  }

  def glIsEnabled(target: State): Boolean = {
    gl.glIsEnabled(target.toGL)
  }

  def glEnable(target: State): Unit = {
    gl.glEnable(target.toGL)
  }

  def glDisable(target: State): Unit = {
    gl.glDisable(target.toGL)
  }

  def glViewport(x: Int, y: Int, width: Int, height: Int): Unit = {
    gl.glViewport(x, y, width, height)
  }

  def glScissor(x: Int, y: Int, width: Int, height: Int): Unit = {
    gl.glScissor(x, y, width, height)
  }

  def glBlendFunc(sfactor: BlendFactor, dfactor: BlendFactor): Unit = {
    gl.glBlendFunc(sfactor.toGL, dfactor.toGL)
  }

  def glDepthMask(shouldWriteDepth: Boolean): Unit = {
    gl.glDepthMask(shouldWriteDepth)
  }

  def glDepthFunc(func: DepthFunc): Unit = {
    gl.glDepthFunc(func.toGL)
  }

  def glClear(mask: ClearMask): Unit = {
    gl.glClear(mask)
  }

  def glGetError(): Option[Int] =
    val e = gl.glGetError()
    if e == GL11.GL_NO_ERROR then None else Some(e)

  // Debug

  object Debug {
    enum MessageSource {
      case Api
      case WindowSystem
      case ShaderCompiler
      case ThirdParty
      case Application
      case Other
      case Unknown(code: Int)
    }

    object MessageSource {
      def fromGL(code: Int): MessageSource = code match {
        case GL43.GL_DEBUG_SOURCE_API             => MessageSource.Api
        case GL43.GL_DEBUG_SOURCE_WINDOW_SYSTEM   => MessageSource.WindowSystem
        case GL43.GL_DEBUG_SOURCE_SHADER_COMPILER => MessageSource.ShaderCompiler
        case GL43.GL_DEBUG_SOURCE_THIRD_PARTY     => MessageSource.ThirdParty
        case GL43.GL_DEBUG_SOURCE_APPLICATION     => MessageSource.Application
        case GL43.GL_DEBUG_SOURCE_OTHER           => MessageSource.Other
        case _                                    => MessageSource.Unknown(code)
      }
    }

    enum MessageType {
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

    object MessageType {
      def fromGL(code: Int): MessageType = code match {
        case GL43.GL_DEBUG_TYPE_ERROR               => MessageType.Error
        case GL43.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR => MessageType.DeprecatedBehavior
        case GL43.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR  => MessageType.UndefinedBehavior
        case GL43.GL_DEBUG_TYPE_PORTABILITY         => MessageType.Portability
        case GL43.GL_DEBUG_TYPE_PERFORMANCE         => MessageType.Performance
        case GL43.GL_DEBUG_TYPE_MARKER              => MessageType.Marker
        case GL43.GL_DEBUG_TYPE_PUSH_GROUP          => MessageType.PushGroup
        case GL43.GL_DEBUG_TYPE_POP_GROUP           => MessageType.PopGroup
        case GL43.GL_DEBUG_TYPE_OTHER               => MessageType.Other
        case _                                      => MessageType.Unknown(code)
      }
    }

    enum MessageSeverity {
      case High
      case Medium
      case Low
      case Notification
      case Unknown(code: Int)
    }

    object MessageSeverity {
      def fromGL(code: Int): MessageSeverity = code match {
        case GL43.GL_DEBUG_SEVERITY_HIGH         => MessageSeverity.High
        case GL43.GL_DEBUG_SEVERITY_MEDIUM       => MessageSeverity.Medium
        case GL43.GL_DEBUG_SEVERITY_LOW          => MessageSeverity.Low
        case GL43.GL_DEBUG_SEVERITY_NOTIFICATION => MessageSeverity.Notification
        case _                                   => MessageSeverity.Unknown(code)
      }
    }

    case class Message(
        source: MessageSource,
        debugType: MessageType,
        id: Int,
        severity: MessageSeverity,
        message: String,
        userParam: Long
    )

    object Message {
      def fromGL(source: Int, debugType: Int, id: Int, severity: Int, message: String, userParam: Long): Message = {
        val messageSource = MessageSource.fromGL(source)
        val messageType = MessageType.fromGL(debugType)
        val messageSeverity = MessageSeverity.fromGL(severity)
        Message(messageSource, messageType, id, messageSeverity, message, userParam)
      }
    }
  }

  def glDebugMessageCallback(callback: Debug.Message => Unit, userParam: Long): Unit = {
    val glCallback: GLDebugMessageCallbackI = (source, debugType, id, severity, length, messageAddress, userParam) => {
      val message =
        if length < 0 then {
          MemoryUtil.memASCII(messageAddress)
        } else {
          MemoryUtil.memASCII(messageAddress, length)
        }

      val debugMessage = Debug.Message.fromGL(source, debugType, id, severity, message, userParam)

      callback(debugMessage)
    }
    gl.glDebugMessageCallback(glCallback, userParam)
  }
}

// ---- Wrappers and stubs ----

trait GLCapabilitiesWrapper {
  def GL_KHR_debug: Boolean
}

class RealGLCapabilities(cap: GLCapabilities) extends GLCapabilitiesWrapper {
  def GL_KHR_debug: Boolean = cap.GL_KHR_debug
}

class StubGLCapabilities extends GLCapabilitiesWrapper {
  def GL_KHR_debug = true
}

trait GLWrapper {
  def createCapabilities(): GLCapabilitiesWrapper
  def getCapabilities: GLCapabilitiesWrapper

  def lockContext(): Unit
  def unlockContext(): Unit

  def glCreateShader(shaderType: Int): Int
  def glShaderSource(shader: Int, string: String): Unit
  def glCompileShader(shader: Int): Unit
  def glGetShaderi(shader: Int, prop: Int): Int
  def glGetShaderInfoLog(shader: Int, maxLength: Int): String
  def glDeleteShader(shader: Int): Unit

  def glCreateProgram(): Int
  def glUseProgram(program: Int): Unit
  def glLinkProgram(program: Int): Unit
  def glDeleteProgram(program: Int): Unit

  def glGetProgramInfoLog(program: Int, maxLength: Int): String
  def glAttachShader(program: Int, shader: Int): Unit
  def glDetachShader(program: Int, shader: Int): Unit
  def glGetUniformLocation(program: Int, name: String): Int
  def glGetAttribLocation(program: Int, name: String): Int
  def glBindAttribLocation(program: Int, index: Int, name: String): Unit
  def glGetProgrami(program: Int, prop: Int): Int

  def glUniform1i(location: Int, v0: Int): Unit
  def glUniform1f(location: Int, v0: Float): Unit
  def glUniform2f(location: Int, v0: Float, v1: Float): Unit
  def glUniform3f(location: Int, v0: Float, v1: Float, v2: Float): Unit
  def glUniform4f(location: Int, v0: Float, v1: Float, v2: Float, v3: Float): Unit
  def glUniformMatrix4fv(location: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glGenFramebuffers(): Int
  def glBindFramebuffer(target: Int, framebuffer: Int): Unit
  def glDrawBuffer(buf: Int): Unit
  def glDrawBuffers(bufs: Array[Int]): Unit
  def glFramebufferTexture(target: Int, attachment: Int, texture: Int, level: Int): Unit
  def glDeleteFramebuffers(framebuffer: Int): Unit

  def glDrawArrays(mode: Int, first: Int, count: Int): Unit
  def glDrawArraysInstanced(mode: Int, first: Int, count: Int, primcount: Int): Unit

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
  ): Unit

  def glTexImage2D(
      target: Int,
      level: Int,
      internalFormat: Int,
      width: Int,
      height: Int,
      border: Int,
      format: Int,
      texelDataType: Int,
      pixels: FloatBuffer
  ): Unit

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
  ): Unit

  def glBindTexture(target: Int, texture: Int): Unit
  def glGenTextures(): Int

  def glDeleteTextures(texture: Int): Unit
  def glActiveTexture(textureSlot: Int): Unit
  def glGenerateMipmap(target: Int): Unit
  def glTexParameteri(target: Int, pname: Int, param: Int): Unit

  def glDeleteVertexArrays(array: Int): Unit
  def glBindVertexArray(array: Int): Unit
  def glGenVertexArrays(): Int

  def glDeleteBuffers(buffer: Int): Unit
  def glBufferSubData(target: Int, offset: Long, data: ByteBuffer): Unit
  def glBufferData(target: Int, size: Long, usage: Int): Unit
  def glBindBuffer(target: Int, buffer: Int): Unit
  def glGenBuffers(): Int

  def glVertexAttribDivisor(index: Int, divisor: Int): Unit
  def glVertexAttribIPointer(index: Int, size: Int, dataType: Int, stride: Int, pointer: Long): Unit
  def glVertexAttribPointer(index: Int, size: Int, dataType: Int, normalized: Boolean, stride: Int, pointer: Long): Unit
  def glEnableVertexAttribArray(index: Int): Unit

  def glGetError(): Int
  def glClear(mask: Int): Unit
  def glDepthMask(flag: Boolean): Unit
  def glDepthFunc(func: Int): Unit
  def glBlendFunc(sfactor: Int, dfactor: Int): Unit
  def glScissor(x: Int, y: Int, width: Int, height: Int): Unit
  def glViewport(x: Int, y: Int, width: Int, height: Int): Unit
  def glDisable(target: Int): Unit
  def glEnable(target: Int): Unit
  def glIsEnabled(target: Int): Boolean

  def glDebugMessageCallback(callback: GLDebugMessageCallbackI, userParam: Long): Unit
}

class StubGL extends GLWrapper {
  def createCapabilities(): GLCapabilitiesWrapper = new StubGLCapabilities
  def getCapabilities: GLCapabilitiesWrapper = new StubGLCapabilities

  def lockContext(): Unit = ()
  def unlockContext(): Unit = ()

  private var lastShaderId = 8
  def glCreateShader(shaderType: Int): Int =
    lastShaderId += 1
    lastShaderId

  def glShaderSource(shader: Int, string: String): Unit = ()
  def glCompileShader(shader: Int): Unit = ()
  def glGetShaderi(shader: Int, prop: Int): Int = prop match
    case GL20.GL_COMPILE_STATUS  => GL11.GL_TRUE
    case GL20.GL_INFO_LOG_LENGTH => 1000
    case _                       => throw new RuntimeException(s"Unknown glGetShaderi prop: $prop")
  def glGetShaderInfoLog(shader: Int, maxLength: Int): String = ""
  def glDeleteShader(shader: Int): Unit = ()

  private var lastProgramId = 6
  def glCreateProgram(): Int =
    lastProgramId += 1
    lastProgramId

  def glUseProgram(program: Int): Unit = ()
  def glLinkProgram(program: Int): Unit = ()
  def glDeleteProgram(program: Int): Unit = ()

  def glGetProgramInfoLog(program: Int, maxLength: Int): String = ""
  def glAttachShader(program: Int, shader: Int): Unit = ()
  def glDetachShader(program: Int, shader: Int): Unit = ()
  def glGetUniformLocation(program: Int, name: String): Int = 3
  def glGetAttribLocation(program: Int, name: String): Int = 5
  def glBindAttribLocation(program: Int, index: Int, name: String): Unit = ()
  def glGetProgrami(program: Int, prop: Int): Int = prop match
    case GL20.GL_LINK_STATUS     => GL11.GL_TRUE
    case GL20.GL_INFO_LOG_LENGTH => 1000
    case _                       => throw new RuntimeException(s"Unknown glGetProgrami prop: $prop")

  def glUniform1i(location: Int, v0: Int): Unit = ()
  def glUniform1f(location: Int, v0: Float): Unit = ()
  def glUniform2f(location: Int, v0: Float, v1: Float): Unit = ()
  def glUniform3f(location: Int, v0: Float, v1: Float, v2: Float): Unit = ()
  def glUniform4f(location: Int, v0: Float, v1: Float, v2: Float, v3: Float): Unit = ()
  def glUniformMatrix4fv(location: Int, transpose: Boolean, value: FloatBuffer): Unit = ()

  def glGenFramebuffers(): Int = 9
  def glBindFramebuffer(target: Int, framebuffer: Int): Unit = ()
  def glDrawBuffer(buf: Int): Unit = ()
  def glDrawBuffers(bufs: Array[Int]): Unit = ()
  def glFramebufferTexture(target: Int, attachment: Int, texture: Int, level: Int): Unit = ()
  def glDeleteFramebuffers(framebuffer: Int): Unit = ()

  def glDrawArrays(mode: Int, first: Int, count: Int): Unit = ()
  def glDrawArraysInstanced(mode: Int, first: Int, count: Int, primcount: Int): Unit = ()

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
  ): Unit = ()

  def glTexImage2D(
      target: Int,
      level: Int,
      internalFormat: Int,
      width: Int,
      height: Int,
      border: Int,
      format: Int,
      texelDataType: Int,
      pixels: FloatBuffer
  ): Unit = ()

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
  ): Unit = ()

  def glBindTexture(target: Int, texture: Int): Unit = ()
  def glGenTextures(): Int = 11

  def glDeleteTextures(texture: Int): Unit = ()
  def glActiveTexture(textureSlot: Int): Unit = ()
  def glGenerateMipmap(target: Int): Unit = ()
  def glTexParameteri(target: Int, pname: Int, param: Int): Unit = ()

  def glDeleteVertexArrays(array: Int): Unit = ()
  def glBindVertexArray(array: Int): Unit = ()
  def glGenVertexArrays(): Int = 13

  def glDeleteBuffers(buffer: Int): Unit = ()
  def glBufferSubData(target: Int, offset: Long, data: ByteBuffer): Unit = ()
  def glBufferData(target: Int, size: Long, usage: Int): Unit = ()
  def glBindBuffer(target: Int, buffer: Int): Unit = ()
  def glGenBuffers(): Int = 17

  def glVertexAttribDivisor(index: Int, divisor: Int): Unit = ()
  def glVertexAttribIPointer(index: Int, size: Int, dataType: Int, stride: Int, pointer: Long): Unit = ()
  def glVertexAttribPointer(
      index: Int,
      size: Int,
      dataType: Int,
      normalized: Boolean,
      stride: Int,
      pointer: Long
  ): Unit = ()
  def glEnableVertexAttribArray(index: Int): Unit = ()

  def glGetError(): Int = 0
  def glClear(mask: Int): Unit = ()
  def glDepthMask(flag: Boolean): Unit = ()
  def glDepthFunc(func: Int): Unit = ()
  def glBlendFunc(sfactor: Int, dfactor: Int): Unit = ()
  def glScissor(x: Int, y: Int, width: Int, height: Int): Unit = ()
  def glViewport(x: Int, y: Int, width: Int, height: Int): Unit = ()
  def glDisable(target: Int): Unit = ()
  def glEnable(target: Int): Unit = ()
  def glIsEnabled(target: Int): Boolean = false

  def glDebugMessageCallback(callback: GLDebugMessageCallbackI, userParam: Long): Unit = ()
}

object RealGL extends GLWrapper {
  def createCapabilities(): GLCapabilitiesWrapper = new RealGLCapabilities(GL.createCapabilities())
  def getCapabilities: GLCapabilitiesWrapper = new RealGLCapabilities(GL.getCapabilities)

  def lockContext(): Unit = CGL.CGLLockContext(CGL.CGLGetCurrentContext())
  def unlockContext(): Unit = CGL.CGLUnlockContext(CGL.CGLGetCurrentContext())

  def glCreateShader(shaderType: Int): Int = GL20.glCreateShader(shaderType)
  def glShaderSource(shader: Int, string: String): Unit = GL20.glShaderSource(shader, string)
  def glCompileShader(shader: Int): Unit = GL20.glCompileShader(shader)
  def glGetShaderi(shader: Int, pname: Int): Int = GL20.glGetShaderi(shader, pname)
  def glGetShaderInfoLog(shader: Int, maxLength: Int): String = GL20.glGetShaderInfoLog(shader, maxLength)
  def glDeleteShader(shader: Int): Unit = GL20.glDeleteShader(shader)

  def glCreateProgram(): Int = GL20.glCreateProgram()
  def glUseProgram(program: Int): Unit = GL20.glUseProgram(program)
  def glLinkProgram(program: Int): Unit = GL20.glLinkProgram(program)
  def glDeleteProgram(program: Int): Unit = GL20.glDeleteProgram(program)

  def glGetProgramInfoLog(program: Int, maxLength: Int): String = GL20.glGetProgramInfoLog(program, maxLength)
  def glAttachShader(program: Int, shader: Int): Unit = GL20.glAttachShader(program, shader)
  def glDetachShader(program: Int, shader: Int): Unit = GL20.glDetachShader(program, shader)
  def glGetUniformLocation(program: Int, name: String): Int = GL20.glGetUniformLocation(program, name)
  def glGetAttribLocation(program: Int, name: String): Int = GL20.glGetAttribLocation(program, name)
  def glBindAttribLocation(program: Int, index: Int, name: String): Unit =
    GL20.glBindAttribLocation(program, index, name)
  def glGetProgrami(program: Int, pname: Int): Int = GL20.glGetProgrami(program, pname)

  def glUniform1i(location: Int, v0: Int): Unit = GL20.glUniform1i(location, v0)
  def glUniform1f(location: Int, v0: Float): Unit = GL20.glUniform1f(location, v0)
  def glUniform2f(location: Int, v0: Float, v1: Float): Unit = GL20.glUniform2f(location, v0, v1)
  def glUniform3f(location: Int, v0: Float, v1: Float, v2: Float): Unit = GL20.glUniform3f(location, v0, v1, v2)
  def glUniform4f(location: Int, v0: Float, v1: Float, v2: Float, v3: Float): Unit =
    GL20.glUniform4f(location, v0, v1, v2, v3)
  def glUniformMatrix4fv(location: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GL20.glUniformMatrix4fv(location, transpose, value)

  def glGenFramebuffers(): Int = GL30.glGenFramebuffers()
  def glBindFramebuffer(target: Int, framebuffer: Int): Unit = GL30.glBindFramebuffer(target, framebuffer)
  def glDrawBuffer(buf: Int): Unit = GL11.glDrawBuffer(buf)
  def glDrawBuffers(bufs: Array[Int]): Unit = GL20.glDrawBuffers(bufs)
  def glFramebufferTexture(target: Int, attachment: Int, texture: Int, level: Int): Unit =
    GL32.glFramebufferTexture(target, attachment, texture, level)
  def glDeleteFramebuffers(framebuffer: Int): Unit = GL30.glDeleteFramebuffers(framebuffer)

  def glDrawArrays(mode: Int, first: Int, count: Int): Unit = GL11.glDrawArrays(mode, first, count)
  def glDrawArraysInstanced(mode: Int, first: Int, count: Int, primcount: Int): Unit =
    GL31.glDrawArraysInstanced(mode, first, count, primcount)

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

  def glTexImage2D(
      target: Int,
      level: Int,
      internalFormat: Int,
      width: Int,
      height: Int,
      border: Int,
      format: Int,
      texelDataType: Int,
      pixels: FloatBuffer
  ): Unit = GL11.glTexImage2D(target, level, internalFormat, width, height, border, format, texelDataType, pixels)

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

  def glBindTexture(target: Int, texture: Int): Unit = GL11.glBindTexture(target, texture)
  def glGenTextures(): Int = GL11.glGenTextures()

  def glDeleteTextures(texture: Int): Unit = GL11.glDeleteTextures(texture)
  def glActiveTexture(textureSlot: Int): Unit = GL13.glActiveTexture(textureSlot)
  def glGenerateMipmap(target: Int): Unit = GL30.glGenerateMipmap(target)
  def glTexParameteri(target: Int, pname: Int, param: Int): Unit = GL11.glTexParameteri(target, pname, param)

  def glDeleteVertexArrays(array: Int): Unit = GL30.glDeleteVertexArrays(array)
  def glBindVertexArray(array: Int): Unit = GL30.glBindVertexArray(array)
  def glGenVertexArrays(): Int = GL30.glGenVertexArrays()

  def glDeleteBuffers(buffer: Int): Unit = GL15.glDeleteBuffers(buffer)
  def glBufferSubData(target: Int, offset: Long, data: ByteBuffer): Unit = GL15.glBufferSubData(target, offset, data)
  def glBufferData(target: Int, size: Long, usage: Int): Unit = GL15.glBufferData(target, size, usage)
  def glBindBuffer(target: Int, buffer: Int): Unit = GL15.glBindBuffer(target, buffer)
  def glGenBuffers(): Int = GL15.glGenBuffers()

  def glVertexAttribDivisor(index: Int, divisor: Int): Unit = GL33.glVertexAttribDivisor(index, divisor)
  def glVertexAttribIPointer(index: Int, size: Int, dataType: Int, stride: Int, pointer: Long): Unit =
    GL30.glVertexAttribIPointer(index, size, dataType, stride, pointer)
  def glVertexAttribPointer(
      index: Int,
      size: Int,
      dataType: Int,
      normalized: Boolean,
      stride: Int,
      pointer: Long
  ): Unit = GL20.glVertexAttribPointer(index, size, dataType, normalized, stride, pointer)
  def glEnableVertexAttribArray(index: Int): Unit = GL20.glEnableVertexAttribArray(index)

  def glGetError(): Int = GL11.glGetError()
  def glClear(mask: Int): Unit = GL11.glClear(mask)
  def glDepthMask(flag: Boolean): Unit = GL11.glDepthMask(flag)
  def glDepthFunc(func: Int): Unit = GL11.glDepthFunc(func)
  def glBlendFunc(sfactor: Int, dfactor: Int): Unit = GL11.glBlendFunc(sfactor, dfactor)
  def glScissor(x: Int, y: Int, width: Int, height: Int): Unit = GL11.glScissor(x, y, width, height)
  def glViewport(x: Int, y: Int, width: Int, height: Int): Unit = GL11.glViewport(x, y, width, height)
  def glDisable(target: Int): Unit = GL11.glDisable(target)
  def glEnable(target: Int): Unit = GL11.glEnable(target)
  def glIsEnabled(target: Int): Boolean = GL11.glIsEnabled(target)

  def glDebugMessageCallback(callback: GLDebugMessageCallbackI, userParam: Long): Unit =
    GL43.glDebugMessageCallback(callback, userParam)
}
