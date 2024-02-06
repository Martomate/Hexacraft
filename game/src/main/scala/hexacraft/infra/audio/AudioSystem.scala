package hexacraft.infra.audio

import hexacraft.infra.audio.AudioSystem.Event
import hexacraft.infra.fs.Bundle
import hexacraft.util.{EventDispatcher, Tracker}

import org.joml.Vector3f
import org.lwjgl.openal.{AL, AL10, ALC, ALC10, ALCapabilities, ALCCapabilities}
import org.lwjgl.stb.{STBVorbis, STBVorbisInfo}
import org.lwjgl.system.{MemoryStack, MemoryUtil}

import java.nio.{ByteBuffer, IntBuffer, ShortBuffer}

object AudioSystem {
  def create(): AudioSystem = new AudioSystem(new RealAL)

  def createNull(): AudioSystem = new AudioSystem(new NullAL)

  enum Event {
    case Initialized
    case StartedPlaying
  }
}

class AudioSystem(al: ALWrapper) {
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

  def createSoundSource(bufferId: Int): Int = {
    val sourceId = al.alGenSources()
    al.alSourcei(sourceId, AL10.AL_BUFFER, bufferId)
    sourceId
  }

  def setSoundSourcePosition(sourceId: Int, pos: Vector3f): Unit = {
    al.alSource3f(sourceId, AL10.AL_POSITION, pos.x, pos.y, pos.z)
  }

  def startPlayingSound(sourceId: Int): Unit = {
    al.alSourcePlay(sourceId)

    dispatcher.notify(AudioSystem.Event.StartedPlaying)
  }

  def loadSoundBuffer(filename: String) = {
    val bytes = Bundle.locate(filename).get.readBytes()
    val data = ByteBuffer.allocateDirect(bytes.length)
    data.put(bytes)
    data.flip()

    val (info, buf) = readVorbis(data)

    val bufferId = al.alGenBuffers()
    al.alBufferData(bufferId, AL10.AL_FORMAT_MONO16, buf, 44100)

    bufferId
  }

  private def readVorbis(data: ByteBuffer): (STBVorbisInfo, ShortBuffer) = {
    val stack = MemoryStack.stackPush
    try {
      val error = stack.mallocInt(1)
      val decoder = STBVorbis.stb_vorbis_open_memory(data, error, null)
      if decoder == 0L then {
        throw new RuntimeException("Failed to open Ogg Vorbis file. Error: " + error.get(0))
      }
      val info = STBVorbisInfo.create()
      STBVorbis.stb_vorbis_get_info(decoder, info)
      val channels = info.channels
      val lengthSamples = STBVorbis.stb_vorbis_stream_length_in_samples(decoder)
      val pcm = MemoryUtil.memAllocShort(lengthSamples)
      pcm.limit(STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm) * channels)
      STBVorbis.stb_vorbis_close(decoder)
      (info, pcm)
    } finally {
      if stack != null then {
        stack.close()
      }
    }
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
