package hexacraft.server

import hexacraft.game.{GameKeyboard, ServerMessage}
import hexacraft.world.{Camera, EntityEvent, Player}
import hexacraft.world.block.BlockState
import hexacraft.world.coord.BlockRelWorld
import hexacraft.world.entity.Entity

import org.joml.Vector2f

import java.util.UUID
import scala.collection.mutable

case class PlayerData(player: Player, entity: Entity, camera: Camera) {
  var pressedKeys: Seq[GameKeyboard.Key] = Seq.empty
  var mouseMovement: Vector2f = new Vector2f
  val blockUpdatesWaitingToBeSent: mutable.ArrayBuffer[(BlockRelWorld, BlockState)] = mutable.ArrayBuffer.empty
  val entityEventsWaitingToBeSent: mutable.ArrayBuffer[(UUID, EntityEvent)] = mutable.ArrayBuffer.empty
  val messagesWaitingToBeSent: mutable.ArrayBuffer[ServerMessage] = mutable.ArrayBuffer.empty

  var lastSeen: Long = System.currentTimeMillis
  var shouldBeKicked: Boolean = false
}
