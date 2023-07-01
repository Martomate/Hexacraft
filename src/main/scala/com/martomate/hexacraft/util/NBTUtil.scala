package com.martomate.hexacraft.util

import com.flowpowered.nbt.*
import com.flowpowered.nbt.stream.{NBTInputStream, NBTOutputStream}
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, IOException}
import java.nio.file.Files
import java.util
import java.util.stream.Collectors
import org.joml.Vector3d
import scala.collection.immutable.{ArraySeq, ListMap}
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

object NBTUtil {
  def makeCompoundTag(name: String, children: Seq[Tag[?]]): CompoundTag =
    Nbt.MapTag(ListMap.from(children.map(t => t.getName -> Nbt.convertTag(t)))).toCompoundTag(name)

  def makeListTag[T <: Nbt](name: String, clazz: Class[T], children: Seq[T]): Nbt.ListTag[T] =
    Nbt.ListTag[T](children)

  def makeVectorTag(name: String, vector: Vector3d): CompoundTag = NBTUtil.makeCompoundTag(
    name,
    Seq(
      new DoubleTag("x", vector.x),
      new DoubleTag("y", vector.y),
      new DoubleTag("z", vector.z)
    )
  )
}
