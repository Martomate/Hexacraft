package hexacraft.game

import org.joml.Vector2f

import java.nio.charset.Charset

enum NetworkPacket {
  case GetWorldInfo
  case GetState(path: String)
  case PlayerRightClicked
  case PlayerLeftClicked
  case PlayerMovedMouse(distance: Vector2f)
  case PlayerPressedKeys(keys: Seq[GameKeyboard.Key])
}

object NetworkPacket {
  def deserialize(bytes: Array[Byte], charset: Charset): NetworkPacket = {
    val message = String(bytes, charset)
    if message == "get_world_info" then {
      NetworkPacket.GetWorldInfo
    } else if message.startsWith("get_state ") then {
      val path = message.substring(10)
      NetworkPacket.GetState(path)
    } else if message == "right_mouse_clicked" then {
      PlayerRightClicked
    } else if message == "left_mouse_clicked" then {
      PlayerLeftClicked
    } else if message.startsWith("mouse_moved ") then {
      val args = message.substring(12).split(' ')
      val dx = args(0).toFloat
      val dy = args(1).toFloat
      PlayerMovedMouse(new Vector2f(dx, dy))
    } else if message.startsWith("keys_pressed ") then {
      val keys = message.substring(13).split(' ').toSeq.filter(_ != "").map(s => GameKeyboard.Key.valueOf(s))
      PlayerPressedKeys(keys)
    } else {
      val bytesHex = bytes.map(b => Integer.toHexString(b & 0xff)).mkString("Array(", ", ", ")")
      throw new IllegalArgumentException(s"unknown packet type (message: '$message', raw: $bytesHex)")
    }
  }

  extension (p: NetworkPacket) {
    def serialize(charset: Charset): Array[Byte] =
      val str = p match {
        case NetworkPacket.GetWorldInfo            => "get_world_info"
        case NetworkPacket.GetState(path)          => s"get_state $path"
        case NetworkPacket.PlayerRightClicked      => "right_mouse_clicked"
        case NetworkPacket.PlayerLeftClicked       => "left_mouse_clicked"
        case NetworkPacket.PlayerMovedMouse(dist)  => s"mouse_moved ${dist.x} ${dist.y}"
        case NetworkPacket.PlayerPressedKeys(keys) => s"keys_pressed ${keys.mkString(" ")}"
      }
      str.getBytes(charset)
  }
}
