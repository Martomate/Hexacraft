package com.martomate.hexacraft.world.entity

import com.flowpowered.nbt.CompoundTag
import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.coord.fp.CylCoords
import org.joml.Vector3d

trait EntityFactory[E <: Entity]:
  def atStartPos(pos: CylCoords)(using EntityModelLoader, CylinderSize, Blocks): E

  def fromNBT(tag: CompoundTag)(using EntityModelLoader, CylinderSize, Blocks): E
