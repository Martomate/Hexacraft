package com.martomate.hexacraft.util

import java.io.{File, FileInputStream, FileOutputStream}

import com.flowpowered.nbt._
import com.flowpowered.nbt.stream.{NBTInputStream, NBTOutputStream}
import org.joml.Vector3d

import scala.collection.mutable
import scala.collection.JavaConverters._

object NBTUtil {
  def getBoolean(tag: CompoundTag, key: String, default: =>Boolean): Boolean = {
    getByte(tag, key, if (default) 1 else 0) == 1
  }

  def getByte(tag: CompoundTag, key: String, default: =>Byte): Byte = {
    if (tag == null) default
    else {
      tag.getValue.get(key) match {
        case t: ByteTag => t.getValue.byteValue()
        case _ => default
      }
    }
  }

  def getShort(tag: CompoundTag, key: String, default: =>Short): Short = {
    if (tag == null) default
    else {
      tag.getValue.get(key) match {
        case t: ShortTag => t.getValue.shortValue()
        case _ => default
      }
    }
  }

  def getLong(tag: CompoundTag, key: String, default: =>Long): Long = {
    if (tag == null) default
    else {
      tag.getValue.get(key) match {
        case t: LongTag => t.getValue.longValue()
        case _ => default
      }
    }
  }
  
  def getDouble(tag: CompoundTag, key: String, default: =>Double): Double = {
    if (tag == null) default
    else {
      tag.getValue.get(key) match {
        case t: DoubleTag => t.getValue.doubleValue()
        case _ => default
      }
    }
  }

  def getString(tag: CompoundTag, key: String, default: =>String): String = {
    if (tag == null) default
    else {
      tag.getValue.get(key) match {
        case t: StringTag => t.getValue
        case _ => default
      }
    }
  }

  def getString(tag: CompoundTag, key: String): Option[String] = {
    if (tag == null) None
    else {
      tag.getValue.get(key) match {
        case t: StringTag => Some(t.getValue)
        case _ => None
      }
    }
  }

  def getList(tag: CompoundTag, key: String): Option[Seq[Tag[_]]] = {
    if (tag == null) None
    else {
      tag.getValue.get(key) match {
        case t: ListTag[_] => Some(t.getValue.asScala)
        case _ => None
      }
    }
  }
  
  def getTag(tag: CompoundTag, name: String): Option[Tag[_]] = {
    if (tag == null) None
    else {
      tag.getValue.get(name) match {
        case t: Tag[_] => Some(t)
        case _ => None
      }
    }
  }

  def getCompoundTag(tag: CompoundTag, name: String): Option[CompoundTag] = {
    if (tag == null) None
    else {
      tag.getValue.get(name) match {
        case t: CompoundTag => Some(t)
        case _ => None
      }
    }
  }

  def getByteArray(tag: CompoundTag, name: String): Option[mutable.WrappedArray[Byte]] = {
    NBTUtil.getTag(tag, name).map(_.asInstanceOf[ByteArrayTag].getValue)
  }

  def getShortArray(tag: CompoundTag, name: String): Option[mutable.WrappedArray[Short]] = {
    NBTUtil.getTag(tag, name).map(_.asInstanceOf[ShortArrayTag].getValue)
  }

  def setVector(tag: CompoundTag, vector: Vector3d): Vector3d = {
    val x = NBTUtil.getDouble(tag, "x", vector.x)
    val y = NBTUtil.getDouble(tag, "y", vector.y)
    val z = NBTUtil.getDouble(tag, "z", vector.z)
    vector.set(x, y, z)
  }

  def makeCompoundTag(name: String, children: Seq[Tag[_]]): CompoundTag = {
    val map = new CompoundMap()
    for (tag <- children) map.put(tag)
    new CompoundTag(name, map)
  }

  def makeVectorTag(name: String, vector: Vector3d): CompoundTag = NBTUtil.makeCompoundTag(name, Seq(
    new DoubleTag("x", vector.x),
    new DoubleTag("y", vector.y),
    new DoubleTag("z", vector.z)
  ))
  
  def saveTag(tag: Tag[_], nbtFile: File): Unit = {
    new Thread(() => {
      if (!nbtFile.exists()) {
        nbtFile.getParentFile.mkdirs()
        nbtFile.createNewFile()
      }
      val nbtOut = new NBTOutputStream(new FileOutputStream(nbtFile))
      nbtOut.writeTag(tag)
      nbtOut.close()
    }).start()
  }

  def loadTag(file: File): CompoundTag = {
    if (file.isFile) {
      val stream = new NBTInputStream(new FileInputStream(file))
      val nbt = stream.readTag().asInstanceOf[CompoundTag]
      stream.close()
      nbt
    } else {
      new CompoundTag("", new CompoundMap())
    }
  }
}
