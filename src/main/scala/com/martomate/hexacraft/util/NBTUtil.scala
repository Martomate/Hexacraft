package com.martomate.hexacraft.util

import java.io.{File, FileInputStream, FileOutputStream}

import com.flowpowered.nbt._
import com.flowpowered.nbt.stream.{NBTInputStream, NBTOutputStream}

import scala.collection.mutable

object NBTUtil {
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

  def makeCompoundTag(name: String, children: Seq[Tag[_]]): CompoundTag = {
    val map = new CompoundMap()
    for (tag <- children) map.put(tag)
    new CompoundTag(name, map)
  }
  
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