package hexagon.util

import java.io.File

import org.jnbt._
import java.io.FileOutputStream

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
  
  def getTag(tag: CompoundTag, name: String): Option[Tag] = {
    if (tag == null) None
    else {
      tag.getValue.get(name) match {
        case t: Tag => Some(t)
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
  
  def makeCompoundTag(name: String, children: Seq[Tag]): CompoundTag = {
    val map = new java.util.HashMap[String, Tag]()
    for (tag <- children) map.put(tag.getName, tag)
    new CompoundTag(name, map)
  }
  
  def saveTag(tag: Tag, nbtFile: File): Unit = {
    new Thread(() => {
      if (!nbtFile.exists()) {
        nbtFile.getParentFile.mkdirs()
        nbtFile.createNewFile()
      }
      val nbtOut = new NBTOutputStream(new FileOutputStream(nbtFile))
      nbtOut.writeTag(tag)
      nbtOut.close()
    }).start
  }
}
