package com.martomate.hexacraft.world.player

import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}
import com.martomate.hexacraft.world.FakeBlockLoader
import com.martomate.hexacraft.world.block.{BlockFactory, BlockLoader, Blocks}
import com.martomate.hexacraft.world.coord.fp.CylCoords
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PlayerTest extends AnyFlatSpec with Matchers {
  given CylinderSize = new CylinderSize(8)
  given BlockLoader = new FakeBlockLoader
  given BlockFactory = new BlockFactory
  implicit val Blocks: Blocks = new Blocks

  "saving" should "save all the fields as NBT" in {
    val player = Player.atStartPos(CylCoords(3.2, 4.56, -1.7))

    val nbt = player.toNBT
    nbt.getName shouldBe "player"

    NBTUtil.getCompoundTag(nbt, "position") should not be None
    NBTUtil.getCompoundTag(nbt, "rotation") should not be None
    NBTUtil.getCompoundTag(nbt, "velocity") should not be None

    NBTUtil.getByte(nbt, "flying", 1) shouldBe 0
    NBTUtil.getShort(nbt, "selectedItemSlot", 100) shouldBe 0

    NBTUtil.getCompoundTag(nbt, "inventory") should not be None
  }

  it should "be possible to load the player from NBT" in {
    val playerBefore = Player.atStartPos(CylCoords(3.2, 4.56, -1.7))
    playerBefore.velocity.set(4, -5, .6)
    playerBefore.rotation.set(0.1, 0.2, -0.3)
    playerBefore.flying = true
    playerBefore.selectedItemSlot = 7
    playerBefore.inventory(3) = Blocks.Stone

    val playerAfter = Player.fromNBT(playerBefore.toNBT)

    playerAfter.velocity.distance(playerBefore.velocity) should be < 1e-9
    playerAfter.position.distance(playerBefore.position) should be < 1e-9
    playerAfter.rotation.distance(playerBefore.rotation) should be < 1e-9
    playerAfter.selectedItemSlot shouldBe playerBefore.selectedItemSlot
    playerAfter.flying shouldBe playerBefore.flying
    playerAfter.inventory shouldBe playerBefore.inventory
  }

  "fromNBT" should "have good default values" in {
    val player = Player.fromNBT(NBTUtil.makeCompoundTag("player", Seq()))

    player.position.length() should be < 1e-9
    player.rotation.length() should be < 1e-9
    player.velocity.length() should be < 1e-9

    player.flying shouldBe false
    player.selectedItemSlot shouldBe 0

    player.inventory shouldBe Inventory.default
  }
}
