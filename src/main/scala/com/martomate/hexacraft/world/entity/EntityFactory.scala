package com.martomate.hexacraft.world.entity

import com.flowpowered.nbt.CompoundTag
import com.martomate.hexacraft.nbt.NBTUtil
import com.martomate.hexacraft.world.{BlocksInWorld, CylinderSize}
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.coord.fp.CylCoords
import org.joml.Vector3d

trait EntityFactory:
  def atStartPos(pos: CylCoords)(using CylinderSize, Blocks): Entity

  def fromNBT(tag: CompoundTag)(using CylinderSize, Blocks): Entity
