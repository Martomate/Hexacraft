package com.martomate.hexacraft.world.entity

import com.martomate.hexacraft.nbt.NBTUtil
import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.coord.fp.CylCoords

import com.flowpowered.nbt.CompoundTag
import org.joml.Vector3d

trait EntityFactory:
  def atStartPos(pos: CylCoords)(using CylinderSize, Blocks): Entity

  def fromNBT(tag: CompoundTag)(using CylinderSize, Blocks): Entity
