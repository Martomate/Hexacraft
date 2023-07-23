package com.martomate.hexacraft.world.player

import com.martomate.hexacraft.nbt.{Nbt, NBTUtil}
import com.martomate.hexacraft.world.{CylinderSize, FakeBlockLoader}
import com.martomate.hexacraft.world.block.{BlockLoader, Blocks}
import com.martomate.hexacraft.world.coord.fp.CylCoords

import munit.FunSuite

class PlayerTest extends FunSuite {
  given CylinderSize = CylinderSize(8)
  given BlockLoader = new FakeBlockLoader
  implicit val Blocks: Blocks = new Blocks

  test("saving should save all the fields as NBT") {
    val player = Player.atStartPos(CylCoords(3.2, 4.56, -1.7))

    val nbt2 = player.toNBT
    assertEquals(nbt2.getName, "player")

    val nbt = Nbt.from(nbt2)
    assert(nbt.getCompoundTag("position").isDefined)
    assert(nbt.getCompoundTag("rotation").isDefined)
    assert(nbt.getCompoundTag("velocity").isDefined)

    assertEquals(nbt.getByte("flying", 1), 0.toByte)
    assertEquals(nbt.getShort("selectedItemSlot", 100), 0.toShort)

    assert(nbt.getCompoundTag("inventory").isDefined)
  }

  test("saving should be possible to load the player from NBT") {
    val playerBefore = Player.atStartPos(CylCoords(3.2, 4.56, -1.7))
    playerBefore.velocity.set(4, -5, .6)
    playerBefore.rotation.set(0.1, 0.2, -0.3)
    playerBefore.flying = true
    playerBefore.selectedItemSlot = 7
    playerBefore.inventory(3) = Blocks.Stone

    val playerAfter = Player.fromNBT(playerBefore.toNBT)

    assert(playerAfter.velocity.distance(playerBefore.velocity) < 1e-9)
    assert(playerAfter.position.distance(playerBefore.position) < 1e-9)
    assert(playerAfter.rotation.distance(playerBefore.rotation) < 1e-9)
    assertEquals(playerAfter.selectedItemSlot, playerBefore.selectedItemSlot)
    assertEquals(playerAfter.flying, playerBefore.flying)
    assertEquals(playerAfter.inventory, playerBefore.inventory)
  }

  test("fromNBT should have good default values") {
    val player = Player.fromNBT(NBTUtil.makeCompoundTag("player", Seq()))

    assert(player.position.length() < 1e-9)
    assert(player.rotation.length() < 1e-9)
    assert(player.velocity.length() < 1e-9)

    assert(!player.flying)
    assertEquals(player.selectedItemSlot, 0)

    assertEquals(player.inventory, Inventory.default)
  }
}
