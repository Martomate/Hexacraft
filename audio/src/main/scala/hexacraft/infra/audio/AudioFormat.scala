package hexacraft.infra.audio

trait AudioFormat {
  def decode(bytes: Array[Byte]): SoundBuffer
}

object AudioFormat {
  object Vorbis extends AudioFormat {
    override def decode(bytes: Array[Byte]): SoundBuffer = {
      VorbisDecoder.decode(bytes)
    }
  }
}
