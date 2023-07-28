package hexacraft.world.entity

import hexacraft.nbt.NBTUtil
import hexacraft.world.{BlocksInWorld, CylinderSize}
import hexacraft.world.block.Blocks
import hexacraft.world.coord.fp.CylCoords

import com.flowpowered.nbt.CompoundTag
import org.joml.Vector3d

trait EntityFactory:
  def atStartPos(pos: CylCoords)(using CylinderSize, Blocks): Entity

  def fromNBT(tag: CompoundTag)(using CylinderSize, Blocks): Entity
