package hexacraft.game

import hexacraft.nbt.{Nbt, NbtCodec}
import hexacraft.world.Inventory
import hexacraft.world.coord.ColumnRelWorld

import org.joml.Vector2f

import java.nio.ByteBuffer
import java.util.UUID
import scala.collection.immutable.ArraySeq
import scala.util.Try

case class ServerMessage(
    text: String,
    sender: ServerMessage.Sender
)

object ServerMessage {
  enum Sender {
    case Server
    case Player(id: UUID, name: String)
  }

  object Sender {
    given NbtCodec[Sender] with {
      override def encode(value: Sender): Nbt.MapTag = {
        val kind = value match {
          case Sender.Server       => "server"
          case Sender.Player(_, _) => "player"
        }
        val data = value match {
          case Sender.Server =>
            Nbt.makeMap()
          case Sender.Player(id, name) =>
            Nbt.makeMap("id" -> Nbt.StringTag(id.toString), "name" -> Nbt.StringTag(name))
        }
        data.withField("kind", Nbt.StringTag(kind))
      }

      override def decode(tag: Nbt.MapTag): Option[Sender] = {
        tag.getString("kind") match {
          case Some("server") =>
            Some(Sender.Server)
          case Some("player") =>
            for {
              id <- tag.getString("id")
              id <- Try(UUID.fromString(id)).toOption
              name <- tag.getString("name")
            } yield Sender.Player(id, name)
          case _ => None
        }
      }
    }
  }

  given NbtCodec[ServerMessage] with {
    override def encode(value: ServerMessage): Nbt.MapTag = {
      Nbt.makeMap(
        "text" -> Nbt.StringTag(value.text),
        "sender" -> Nbt.encode(value.sender)
      )
    }

    override def decode(tag: Nbt.MapTag): Option[ServerMessage] = {
      for {
        text <- tag.getString("text")
        sender <- Nbt.decode[Sender](tag.getMap("sender").getOrElse(Nbt.makeMap()))
      } yield ServerMessage(text, sender)
    }
  }
}

enum NetworkPacket {
  case Login(id: UUID, name: String)
  case Logout

  case GetWorldInfo
  case LoadColumnData(coords: ColumnRelWorld)
  case LoadWorldData

  case GetPlayerState
  case GetEvents
  case GetWorldLoadingEvents(maxChunksToLoad: Int)

  case PlayerRightClicked
  case PlayerLeftClicked
  case PlayerToggledFlying
  case PlayerSetSelectedItemSlot(slot: Short)
  case PlayerUpdatedInventory(inventory: Inventory)
  case PlayerMovedMouse(distance: Vector2f)
  case PlayerPressedKeys(keys: Seq[GameKeyboard.Key])

  case RunCommand(command: String, args: Seq[String])
}

object NetworkPacket {
  def deserialize(bytes: Array[Byte]): NetworkPacket = {
    val tag = Nbt.fromBinary(bytes)._2.asMap.get
    Nbt.decode[NetworkPacket](tag).get
  }

  extension (p: NetworkPacket) {
    def serialize(): Array[Byte] = Nbt.encode(p).toBinary()
  }

  given NbtCodec[NetworkPacket] with {
    override def decode(tag: Nbt.MapTag): Option[NetworkPacket] = {
      val (packetName, packetDataTag) = tag.vs.head
      val root = packetDataTag.asMap.get

      val packet = packetName match {
        case "login" =>
          val idBytes = root.getByteArray("id").get
          val name = root.getString("name").get

          if idBytes.length != 16 then {
            throw new RuntimeException("UUIDs must be 16 bytes")
          }

          val bb = ByteBuffer.wrap(idBytes.unsafeArray)
          val msb = bb.getLong
          val lsb = bb.getLong
          val id = new UUID(msb, lsb)

          NetworkPacket.Login(id, name)
        case "logout" =>
          NetworkPacket.Logout
        case "get_world_info" =>
          NetworkPacket.GetWorldInfo
        case "load_column_data" =>
          val coords = ColumnRelWorld(root.getLong("coords", -1))
          NetworkPacket.LoadColumnData(coords)
        case "load_world_data" =>
          NetworkPacket.LoadWorldData
        case "get_player_state" =>
          NetworkPacket.GetPlayerState
        case "get_events" =>
          NetworkPacket.GetEvents
        case "get_world_loading_events" =>
          val maxChunksToLoad = root.getShort("max_chunks", 1)
          NetworkPacket.GetWorldLoadingEvents(maxChunksToLoad)
        case "right_mouse_clicked" =>
          NetworkPacket.PlayerRightClicked
        case "left_mouse_clicked" =>
          NetworkPacket.PlayerLeftClicked
        case "toggle_flying" =>
          NetworkPacket.PlayerToggledFlying
        case "set_selected_inventory_slot" =>
          val slot = root.getShort("slot", 0)
          NetworkPacket.PlayerSetSelectedItemSlot(slot)
        case "inventory_updated" =>
          val inv = root.getMap("inventory").get
          NetworkPacket.PlayerUpdatedInventory(Inventory.fromNBT(inv))
        case "mouse_moved" =>
          val dx = root.getFloat("dx", 0)
          val dy = root.getFloat("dy", 0)
          NetworkPacket.PlayerMovedMouse(new Vector2f(dx, dy))
        case "keys_pressed" =>
          val keyNames = root.getList("keys").get.map(_.asInstanceOf[Nbt.StringTag].v)
          val keys = keyNames.map(GameKeyboard.Key.valueOf)
          NetworkPacket.PlayerPressedKeys(keys)
        case "run_command" =>
          val commandNbt = root.getMap("command").get
          val name = commandNbt.getString("name").get
          val args = commandNbt.getList("args").get.map(_.asInstanceOf[Nbt.StringTag].v)
          NetworkPacket.RunCommand(name, args)
        case _ =>
          throw new IllegalArgumentException(s"unknown packet type '$packetName'")
      }

      Some(packet)
    }

    override def encode(p: NetworkPacket): Nbt.MapTag = {
      val name: String = p match {
        case NetworkPacket.Login(_, _)                  => "login"
        case NetworkPacket.Logout                       => "logout"
        case NetworkPacket.GetWorldInfo                 => "get_world_info"
        case NetworkPacket.LoadColumnData(_)            => "load_column_data"
        case NetworkPacket.LoadWorldData                => "load_world_data"
        case NetworkPacket.GetPlayerState               => "get_player_state"
        case NetworkPacket.GetEvents                    => "get_events"
        case NetworkPacket.GetWorldLoadingEvents(_)     => "get_world_loading_events"
        case NetworkPacket.PlayerRightClicked           => "right_mouse_clicked"
        case NetworkPacket.PlayerLeftClicked            => "left_mouse_clicked"
        case NetworkPacket.PlayerToggledFlying          => "toggle_flying"
        case NetworkPacket.PlayerSetSelectedItemSlot(_) => "set_selected_inventory_slot"
        case NetworkPacket.PlayerUpdatedInventory(_)    => "inventory_updated"
        case NetworkPacket.PlayerMovedMouse(_)          => "mouse_moved"
        case NetworkPacket.PlayerPressedKeys(_)         => "keys_pressed"
        case NetworkPacket.RunCommand(_, _)             => "run_command"
      }

      val tag: Nbt.MapTag = p match {
        case NetworkPacket.Logout | NetworkPacket.GetWorldInfo | NetworkPacket.LoadWorldData |
            NetworkPacket.PlayerRightClicked | NetworkPacket.PlayerLeftClicked | NetworkPacket.GetPlayerState |
            NetworkPacket.PlayerToggledFlying | NetworkPacket.GetEvents =>
          Nbt.emptyMap

        case NetworkPacket.Login(id, name) =>
          val bb = ByteBuffer.allocate(16)
          bb.putLong(id.getMostSignificantBits)
          bb.putLong(id.getLeastSignificantBits)
          val idBytes = ArraySeq.ofByte(bb.array)

          Nbt.makeMap(
            "id" -> Nbt.ByteArrayTag(idBytes),
            "name" -> Nbt.StringTag(name)
          )
        case NetworkPacket.LoadColumnData(coords) =>
          Nbt.makeMap(
            "coords" -> Nbt.LongTag(coords.value)
          )
        case NetworkPacket.PlayerSetSelectedItemSlot(slot) =>
          Nbt.makeMap(
            "slot" -> Nbt.ShortTag(slot)
          )
        case NetworkPacket.PlayerUpdatedInventory(inv) =>
          Nbt.makeMap(
            "inventory" -> inv.toNBT
          )
        case NetworkPacket.PlayerMovedMouse(dist) =>
          Nbt.makeMap(
            "dx" -> Nbt.FloatTag(dist.x),
            "dy" -> Nbt.FloatTag(dist.y)
          )
        case NetworkPacket.PlayerPressedKeys(keys) =>
          Nbt.makeMap(
            "keys" -> Nbt.ListTag(keys.map(key => Nbt.StringTag(key.toString)))
          )
        case NetworkPacket.RunCommand(command, args) =>
          Nbt.makeMap(
            "command" -> Nbt.makeMap(
              "name" -> Nbt.StringTag(command),
              "args" -> Nbt.ListTag(args.map(arg => Nbt.StringTag(arg)))
            )
          )

        case NetworkPacket.GetWorldLoadingEvents(maxChunksToLoad) =>
          Nbt.makeMap(
            "max_chunks" -> Nbt.ShortTag(maxChunksToLoad.toShort)
          )
      }

      Nbt.makeMap(name -> tag)
    }
  }
}
