package com.martomate.nbt

import munit.FunSuite
import org.joml.Vector3d

import scala.collection.immutable.ArraySeq

class NbtTest extends FunSuite {
  def testGet[T](
      name: String,
      method: Nbt.MapTag => (String, => T) => T,
      tag: Nbt.MapTag,
      expected: T,
      defaultValue: T
  ): Unit = {
    test(s"$name should return the value if the entry exists") {
      assertEquals(method(tag)("tag", defaultValue), expected)
    }

    test(s"$name should return the default value if the entry doesn't exist") {
      assertEquals(method(tag)("tags", defaultValue), defaultValue)
    }

    test(s"$name should return the default value if the name is null") {
      assertEquals(method(tag)(null, defaultValue), defaultValue)
    }

    test(s"$name should throw exception if the tag is null") {
      intercept[NullPointerException](method(null)("tag", defaultValue))
    }
  }

  def testGetOption[T](
      name: String,
      method: Nbt.MapTag => String => Option[T],
      tag: Nbt.MapTag,
      expected: T
  ): Unit = {
    test(s"$name should return Some(the value) if the entry exists") {
      assertEquals(method(tag)("tag"), Some(expected))
    }

    test(s"$name should return None if the entry doesn't exist") {
      assertEquals(method(tag)("tags"), None)
    }

    test(s"$name should return None if the name is null") {
      assertEquals(method(tag)(null), None)
    }

    test(s"$name should throw exception if the tag is null") {
      intercept[NullPointerException](method(null)("tag"))
    }
  }

  private val booleanCTag = Nbt.makeMap("tag" -> Nbt.ByteTag(1.toByte))
  private val byteCTag = Nbt.makeMap("tag" -> Nbt.ByteTag(42.toByte))
  private val shortCTag = Nbt.makeMap("tag" -> Nbt.ShortTag(4242.toShort))
  //  intCTag
  private val longCTag = Nbt.makeMap("tag" -> Nbt.LongTag(4242424242424242L))
  //  floatCTag
  private val doubleCTag = Nbt.makeMap("tag" -> Nbt.DoubleTag(42.42424242424242d))
  private val stringCTag = Nbt.makeMap("tag" -> Nbt.StringTag("42.42424242424242D"))
  private val compCTag = Nbt.makeMap("tag" -> Nbt.makeMap("tag" -> Nbt.ByteTag(42.toByte)))
  private val byteArr = Seq(42, 13, 42, -42).map(_.toByte).toArray
  private val byteArrCTag = Nbt.makeMap("tag" -> Nbt.ByteArrayTag.of(byteArr))
  private val shortArr = Seq(4242, 1313, 4242, -4242).map(_.toShort).toArray
  private val shortArrCTag = Nbt.makeMap("tag" -> Nbt.ShortArrayTag.of(shortArr))
  private val listCTag = Nbt.makeMap("tag" -> Nbt.ListTag(Seq(Nbt.StringTag("a"), Nbt.StringTag("bcd"))))

  testGet("getBoolean", _.getBoolean, booleanCTag, true, false)
  testGet("getByte", _.getByte, byteCTag, 42.toByte, 13.toByte)
  testGet("getShort", _.getShort, shortCTag, 4242.toShort, 13.toShort)

  testGet("getLong", _.getLong, longCTag, 4242424242424242L, 13L)

  testGet("getDouble", _.getDouble, doubleCTag, 42.42424242424242d, 13d)
  testGet("getString", _.getString, stringCTag, "42.42424242424242D", "13")
  testGetOption("getString (Option)", _.getString, stringCTag, "42.42424242424242D")
  testGetOption("getList", _.getList, listCTag, Seq(Nbt.StringTag("a"), Nbt.StringTag("bcd")))
  testGetOption("getTag (Boolean)", _.getTag, booleanCTag, booleanCTag.vs("tag"))
  testGetOption("getTag (Byte)", _.getTag, byteCTag, byteCTag.vs("tag"))
  testGetOption("getTag (Short)", _.getTag, shortCTag, shortCTag.vs("tag"))

  testGetOption("getTag (Long)", _.getTag, longCTag, longCTag.vs("tag"))

  testGetOption("getTag (Double)", _.getTag, doubleCTag, doubleCTag.vs("tag"))
  testGetOption("getTag (String)", _.getTag, stringCTag, stringCTag.vs("tag"))
  testGetOption("getTag (Compound)", _.getTag, compCTag, compCTag.vs("tag"))
  testGetOption("getMap", _.getMap, compCTag, compCTag.vs("tag"))

  testGetOption("getByteArray", _.getByteArray, byteArrCTag, ArraySeq.unsafeWrapArray(byteArr))
  testGetOption("getShortArray", _.getShortArray, shortArrCTag, ArraySeq.unsafeWrapArray(shortArr))

  test("setVector should set the correct values") {
    val cTag = Nbt.makeMap("x" -> Nbt.DoubleTag(1.23), "z" -> Nbt.DoubleTag(4.32), "y" -> Nbt.DoubleTag(9.87))
    val vec = new Vector3d()

    assertEquals(cTag.setVector(vec), vec)

    assertEquals(vec.x, 1.23)
    assertEquals(vec.y, 9.87)
    assertEquals(vec.z, 4.32)
  }

  test("makeVectorTag should set the correct values") {
    val vec = new Vector3d(1.23, 9.87, 4.32)

    assertEquals(
      Nbt.makeVectorTag(vec),
      Nbt.makeMap(
        "x" -> Nbt.DoubleTag(1.23),
        "y" -> Nbt.DoubleTag(9.87),
        "z" -> Nbt.DoubleTag(4.32)
      )
    )
  }

  test("toBinary works for Maps") {
    assertEquals(
      Nbt.makeMap("ab" -> Nbt.ShortTag(0x1234)).toBinary("cde").toSeq,
      Array[Byte](10, 0, 3, 'c', 'd', 'e', 2, 0, 2, 'a', 'b', 0x12, 0x34, 0).toSeq
    )
  }

  test("fromBinary works for Maps") {
    assertEquals(
      Nbt.fromBinary(Array[Byte](10, 0, 3, 'c', 'd', 'e', 2, 0, 2, 'a', 'b', 0x12, 0x34, 0)),
      "cde" -> Nbt.makeMap("ab" -> Nbt.ShortTag(0x1234))
    )
  }
}
