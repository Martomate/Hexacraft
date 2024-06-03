package hexacraft.game

import hexacraft.world.Inventory
import hexacraft.world.coord.{ChunkRelWorld, ColumnRelWorld, CylCoords}

import com.martomate.nbt.Nbt
import org.joml.Vector2f

enum ServerCommand {
  case SpawnEntity(entityName: String, position: CylCoords)
  case KillAllEntities
}

enum NetworkPacket {
  case GetWorldInfo
  case LoadChunkData(coords: ChunkRelWorld)
  case LoadColumnData(coords: ColumnRelWorld)
  case LoadWorldData
  case GetPlayerState
  case GetEvents
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
    val (packetName, packetDataTag) = Nbt.fromBinary(bytes)
    val root = packetDataTag.asInstanceOf[Nbt.MapTag]

    packetName match {
      case "get_world_info" =>
        NetworkPacket.GetWorldInfo
      case "load_chunk_data" =>
        val coords = ChunkRelWorld(root.getLong("coords", -1))
        NetworkPacket.LoadChunkData(coords)
      case "load_column_data" =>
        val coords = ColumnRelWorld(root.getLong("coords", -1))
        NetworkPacket.LoadColumnData(coords)
      case "load_world_data" =>
        NetworkPacket.LoadWorldData
      case "get_player_state" =>
        NetworkPacket.GetPlayerState
      case "get_events" =>
        NetworkPacket.GetEvents
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
  }

  extension (p: NetworkPacket) {
    def serialize(): Array[Byte] =
      val name: String = p match {
        case NetworkPacket.GetWorldInfo                 => "get_world_info"
        case NetworkPacket.LoadChunkData(_)             => "load_chunk_data"
        case NetworkPacket.LoadColumnData(_)            => "load_column_data"
        case NetworkPacket.LoadWorldData                => "load_world_data"
        case NetworkPacket.GetPlayerState               => "get_player_state"
        case NetworkPacket.GetEvents                    => "get_events"
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
        case NetworkPacket.GetWorldInfo | NetworkPacket.LoadWorldData | NetworkPacket.PlayerRightClicked |
            NetworkPacket.PlayerLeftClicked | NetworkPacket.GetPlayerState | NetworkPacket.PlayerToggledFlying |
            NetworkPacket.GetEvents =>
          Nbt.emptyMap

        case NetworkPacket.LoadChunkData(coords) =>
          Nbt.makeMap(
            "coords" -> Nbt.LongTag(coords.value)
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
      }

      tag.toBinary(name)
  }
}
