package hexacraft.infra.audio

import java.nio.ShortBuffer

enum SoundBuffer {
  case Mono16(samples: ShortBuffer, sampleRate: Int)
}
