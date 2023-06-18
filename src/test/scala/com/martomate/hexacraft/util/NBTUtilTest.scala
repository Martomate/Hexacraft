package com.martomate.hexacraft.util

import com.flowpowered.nbt.*
import munit.FunSuite
import org.joml.Vector3d
import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters.*

class NBTUtilTest extends FunSuite {
  def makeCTag(children: Tag[_]*) = new CompoundTag("tag", new CompoundMap(children.asJava))

  def testGet[T](
      name: String,
      method: (CompoundTag, String, => T) => T,
      tag: CompoundTag,
      expected: T,
      defaultValue: T
  ): Unit = {
    test(s"$name should return the value if the entry exists") {
      assertEquals(method(tag, "tag", defaultValue), expected)
    }

    test(s"$name should return the default value if the entry doesn't exist") {
      assertEquals(method(tag, "tags", defaultValue), defaultValue)
    }

    test(s"$name should return the default value if the name is null") {
      assertEquals(method(tag, null, defaultValue), defaultValue)
    }

    test(s"$name should throw exception if the tag is null") {
      intercept[NullPointerException](method(null, "tag", defaultValue))
    }
  }

  def testGetOption[T](
      name: String,
      method: (CompoundTag, String) => Option[T],
      tag: CompoundTag,
      expected: T
  ): Unit = {
    test(s"$name should return Some(the value) if the entry exists") {
      assertEquals(method(tag, "tag"), Some(expected))
    }

    test(s"$name should return None if the entry doesn't exist") {
      assertEquals(method(tag, "tags"), None)
    }

    test(s"$name should return None if the name is null") {
      assertEquals(method(tag, null), None)
    }

    test(s"$name should throw exception if the tag is null") {
      intercept[NullPointerException](method(null, "tag"))
    }
  }

  private val booleanCTag = makeCTag(new ByteTag("tag", 1.toByte))
  private val byteCTag = makeCTag(new ByteTag("tag", 42.toByte))
  private val shortCTag = makeCTag(new ShortTag("tag", 4242.toShort))
  //  intCTag
  private val longCTag = makeCTag(new LongTag("tag", 4242424242424242L))
  //  floatCTag
  private val doubleCTag = makeCTag(new DoubleTag("tag", 42.42424242424242d))
  private val stringCTag = makeCTag(new StringTag("tag", "42.42424242424242D"))
  private val compCTag = makeCTag(makeCTag(new ByteTag("tag", 42.toByte)))
  private val byteArr = Seq(42, 13, 42, -42).map(_.toByte).toArray
  private val byteArrCTag = makeCTag(new ByteArrayTag("tag", byteArr))
  private val shortArr = Seq(4242, 1313, 4242, -4242).map(_.toShort).toArray
  private val shortArrCTag = makeCTag(new ShortArrayTag("tag", shortArr))
  private val listCTag = makeCTag(
    new ListTag("tag", classOf[StringTag], java.util.Arrays.asList(new StringTag("", "a"), new StringTag("", "bcd")))
  )

  testGet("getBoolean", NBTUtil.getBoolean, booleanCTag, true, false)
  testGet("getByte", NBTUtil.getByte, byteCTag, 42.toByte, 13.toByte)
  testGet("getShort", NBTUtil.getShort, shortCTag, 4242.toShort, 13.toShort)

  testGet("getLong", NBTUtil.getLong, longCTag, 4242424242424242L, 13L)

  testGet("getDouble", NBTUtil.getDouble, doubleCTag, 42.42424242424242d, 13d)
  testGet("getString", NBTUtil.getString, stringCTag, "42.42424242424242D", "13")
  testGetOption("getString (Option)", NBTUtil.getString, stringCTag, "42.42424242424242D")
  testGetOption("getList", NBTUtil.getList, listCTag, Seq(new StringTag("", "a"), new StringTag("", "bcd")))
  testGetOption("getTag (Boolean)", NBTUtil.getTag, booleanCTag, booleanCTag.getValue.get("tag"))
  testGetOption("getTag (Byte)", NBTUtil.getTag, byteCTag, byteCTag.getValue.get("tag"))
  testGetOption("getTag (Short)", NBTUtil.getTag, shortCTag, shortCTag.getValue.get("tag"))

  testGetOption("getTag (Long)", NBTUtil.getTag, longCTag, longCTag.getValue.get("tag"))

  testGetOption("getTag (Double)", NBTUtil.getTag, doubleCTag, doubleCTag.getValue.get("tag"))
  testGetOption("getTag (String)", NBTUtil.getTag, stringCTag, stringCTag.getValue.get("tag"))
  testGetOption("getTag (Compound)", NBTUtil.getTag, compCTag, compCTag.getValue.get("tag"))
  testGetOption("getCompoundTag", NBTUtil.getCompoundTag, compCTag, compCTag.getValue.get("tag"))

  testGetOption("getByteArray", NBTUtil.getByteArray, byteArrCTag, ArraySeq.unsafeWrapArray(byteArr))
  testGetOption("getShortArray", NBTUtil.getShortArray, shortArrCTag, ArraySeq.unsafeWrapArray(shortArr))

  test("setVector should set the correct values") {
    val cTag = makeCTag(new DoubleTag("x", 1.23), new DoubleTag("z", 4.32), new DoubleTag("y", 9.87))
    val vec = new Vector3d()

    assertEquals(NBTUtil.setVector(cTag, vec), vec)

    assertEquals(vec.x, 1.23)
    assertEquals(vec.y, 9.87)
    assertEquals(vec.z, 4.32)
  }

  test("makeVectorTag should set the correct values") {
    val vec = new Vector3d(1.23, 9.87, 4.32)

    val cTag = NBTUtil.makeVectorTag("tag", vec)

    assertEquals(cTag, makeCTag(new DoubleTag("x", 1.23), new DoubleTag("y", 9.87), new DoubleTag("z", 4.32)))
  }

  private val bigTag = makeCTag(new ByteTag("byteTag", 13.toByte), makeCTag(new ShortTag("shortTag", 1212.toShort)))
  test("makeCompoundTag should return a CompoundTag with the provided tag in it") {
    assertEquals(NBTUtil.makeCompoundTag("tag", Seq(bigTag)), makeCTag(bigTag))
  }
}
