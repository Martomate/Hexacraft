package hexacraft.infra.audio

import hexacraft.util.{EventDispatcher, Tracker}

import org.joml.Vector3f
import org.lwjgl.openal.*

import java.nio.{ByteBuffer, IntBuffer, ShortBuffer}

object AudioSystem {
  def create(): AudioSystem = new AudioSystem(new RealAL)

  def createNull(): AudioSystem = new AudioSystem(new NullAL)

  enum Event {
    case Initialized
    case StartedPlaying
  }

  opaque type SourceId <: AnyVal = Int
  opaque type BufferId <: AnyVal = Int

  object SourceId {
    private[AudioSystem] def fromInt(id: Int): SourceId = id

    extension (id: SourceId) {
      private[AudioSystem] def toInt: Int = id
    }
  }

  object BufferId {
    private[AudioSystem] def fromInt(id: Int): BufferId = id

    extension (id: BufferId) {
      private[AudioSystem] def toInt: Int = id
    }
  }
}

class AudioSystem(al: ALWrapper) {
  import AudioSystem.*

  private val dispatcher = new EventDispatcher[Event]
  def trackEvents(tracker: Tracker[Event]): Unit = dispatcher.track(tracker)

  def init(): Unit = {
    val device = al.alcOpenDevice(null.asInstanceOf[ByteBuffer])
    if device == 0L then {
      throw new IllegalStateException("Failed to open the default OpenAL device")
    }
    val deviceCaps = al.createCapabilities(device)

    val context = al.alcCreateContext(device, null.asInstanceOf[IntBuffer])
    if context == 0L then {
      throw new IllegalStateException("Failed to create OpenAL context")
    }
    al.alcMakeContextCurrent(context)

    al.createCapabilities(deviceCaps)

    dispatcher.notify(AudioSystem.Event.Initialized)
  }

  def setListenerPosition(pos: Vector3f): Unit = {
    al.alListener3f(AL10.AL_POSITION, pos.x, pos.y, pos.z)
  }

  def setListenerOrientation(at: Vector3f, up: Vector3f): Unit = {
    al.alListenerfv(AL10.AL_ORIENTATION, Array(at.x, at.y, at.z, up.x, up.y, up.z))
  }

  def createSoundSource(bufferId: BufferId): SourceId = {
    val sourceId = al.alGenSources()
    al.alSourcei(sourceId, AL10.AL_BUFFER, bufferId.toInt)
    SourceId.fromInt(sourceId)
  }

  def setSoundSourcePosition(sourceId: SourceId, pos: Vector3f): Unit = {
    al.alSource3f(sourceId.toInt, AL10.AL_POSITION, pos.x, pos.y, pos.z)
  }

  def startPlayingSound(sourceId: SourceId): Unit = {
    al.alSourcePlay(sourceId.toInt)

    dispatcher.notify(AudioSystem.Event.StartedPlaying)
  }

  def loadSoundBufferMono16(samples: ShortBuffer, sampleRate: Int): BufferId = {
    val bufferId = al.alGenBuffers()
    al.alBufferData(bufferId, AL10.AL_FORMAT_MONO16, samples, sampleRate)

    BufferId.fromInt(bufferId)
  }
}

trait ALWrapper {
  def alcOpenDevice(deviceSpecifier: ByteBuffer): Long
  def createCapabilities(device: Long): ALCCapabilities
  def alcCreateContext(device: Long, attrList: IntBuffer): Long
  def alcMakeContextCurrent(context: Long): Boolean
  def createCapabilities(alcCaps: ALCCapabilities): ALCapabilities

  def alListener3f(paramName: Int, value1: Float, value2: Float, value3: Float): Unit
  def alListenerfv(paramName: Int, values: Array[Float]): Unit

  def alGenSources(): Int
  def alSourcei(source: Int, param: Int, value: Int): Unit
  def alSource3f(source: Int, param: Int, v1: Float, v2: Float, v3: Float): Unit
  def alSourcePlay(source: Int): Unit

  def alGenBuffers(): Int
  def alBufferData(bufferName: Int, format: Int, data: ShortBuffer, frequency: Int): Unit
}

class RealAL extends ALWrapper {
  def alcOpenDevice(deviceSpecifier: ByteBuffer): Long = {
    ALC10.alcOpenDevice(deviceSpecifier)
  }

  def createCapabilities(device: Long): ALCCapabilities = {
    ALC.createCapabilities(device)
  }

  def alcCreateContext(device: Long, attrList: IntBuffer): Long = {
    ALC10.alcCreateContext(device, attrList)
  }

  def alcMakeContextCurrent(context: Long): Boolean = {
    ALC10.alcMakeContextCurrent(context)
  }

  def createCapabilities(alcCaps: ALCCapabilities): ALCapabilities = {
    AL.createCapabilities(alcCaps)
  }

  def alListener3f(paramName: Int, value1: Float, value2: Float, value3: Float): Unit = {
    AL10.alListener3f(paramName, value1, value2, value3)
  }

  def alListenerfv(paramName: Int, values: Array[Float]): Unit = {
    AL10.alListenerfv(paramName, values)
  }

  def alGenSources(): Int = {
    AL10.alGenSources()
  }

  def alSourcei(source: Int, param: Int, value: Int): Unit = {
    AL10.alSourcei(source, param, value)
  }

  def alSource3f(source: Int, param: Int, v1: Float, v2: Float, v3: Float): Unit = {
    AL10.alSource3f(source, param, v1, v2, v3)
  }

  def alSourcePlay(source: Int): Unit = {
    AL10.alSourcePlay(source)
  }

  def alGenBuffers(): Int = {
    AL10.alGenBuffers()
  }

  def alBufferData(bufferName: Int, format: Int, data: ShortBuffer, frequency: Int): Unit = {
    AL10.alBufferData(bufferName, format, data, frequency)
  }
}

class NullAL extends ALWrapper {
  def alcOpenDevice(deviceSpecifier: ByteBuffer): Long = 1L
  def createCapabilities(device: Long): ALCCapabilities = null
  def alcCreateContext(device: Long, attrList: IntBuffer): Long = 1L
  def alcMakeContextCurrent(context: Long): Boolean = false
  def createCapabilities(alcCaps: ALCCapabilities): ALCapabilities = null

  def alListener3f(paramName: Int, value1: Float, value2: Float, value3: Float): Unit = ()
  def alListenerfv(paramName: Int, values: Array[Float]): Unit = ()

  def alGenSources(): Int = 1
  def alSourcei(source: Int, param: Int, value: Int): Unit = ()
  def alSource3f(source: Int, param: Int, v1: Float, v2: Float, v3: Float): Unit = ()
  def alSourcePlay(source: Int): Unit = ()

  def alGenBuffers(): Int = 1
  def alBufferData(bufferName: Int, format: Int, data: ShortBuffer, frequency: Int): Unit = ()
}
