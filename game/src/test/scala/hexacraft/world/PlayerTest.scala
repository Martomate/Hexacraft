package hexacraft.world

import hexacraft.nbt.Nbt
import hexacraft.world.{CylinderSize, Inventory, Player}
import hexacraft.world.block.Block
import hexacraft.world.coord.CylCoords

import munit.FunSuite

import java.util.UUID

class PlayerTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  test("saving should save all the fields as NBT") {
    val player = Player.atStartPos(UUID.randomUUID(), CylCoords(3.2, 4.56, -1.7))

    val nbt = player.toNBT
    assert(nbt.getMap("position").isDefined)
    assert(nbt.getMap("rotation").isDefined)
    assert(nbt.getMap("velocity").isDefined)

    assertEquals(nbt.getByte("flying", 1), 0.toByte)
    assertEquals(nbt.getShort("selectedItemSlot", 100), 0.toShort)

    assert(nbt.getMap("inventory").isDefined)
  }

  test("saving should be possible to load the player from NBT") {
    val playerBefore = Player.atStartPos(UUID.randomUUID(), CylCoords(3.2, 4.56, -1.7))
    playerBefore.velocity.set(4, -5, .6)
    playerBefore.rotation.set(0.1, 0.2, -0.3)
    playerBefore.flying = true
    playerBefore.selectedItemSlot = 7
    playerBefore.inventory = playerBefore.inventory.updated(3, Block.Stone)

    val playerAfter = Player.fromNBT(UUID.randomUUID(), playerBefore.toNBT)

    assert(playerAfter.velocity.distance(playerBefore.velocity) < 1e-9)
    assert(playerAfter.position.distance(playerBefore.position) < 1e-9)
    assert(playerAfter.rotation.distance(playerBefore.rotation) < 1e-9)
    assertEquals(playerAfter.selectedItemSlot, playerBefore.selectedItemSlot)
    assertEquals(playerAfter.flying, playerBefore.flying)
    assertEquals(playerAfter.inventory, playerBefore.inventory)
  }

  test("fromNBT should have good default values") {
    val player = Player.fromNBT(UUID.randomUUID(), Nbt.emptyMap)

    assert(player.position.length() < 1e-9)
    assert(player.rotation.length() < 1e-9)
    assert(player.velocity.length() < 1e-9)

    assert(!player.flying)
    assertEquals(player.selectedItemSlot, 0)

    assertEquals(player.inventory, Inventory.default)
  }
}
