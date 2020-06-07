package com.martomate.hexacraft.util

import com.flowpowered.nbt._
import org.joml.Vector3d
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters._

class NBTUtilTest extends AnyFlatSpec with Matchers {
  def makeCTag(children: Tag[_]*) = new CompoundTag("tag", new CompoundMap(children.asJava))

  def testGet[T](name: String, method: (CompoundTag, String, =>T) => T, tag: CompoundTag, expected: T, defaultValue: T): Unit = {
    name should "return the value if the entry exists" in {
      method(tag, "tag", defaultValue) shouldBe expected
    }

    it should "return the default value if the entry doesn't exist" in {
      method(tag, "tags", defaultValue) shouldBe defaultValue
    }

    it should "return the default value if the tag or name is null" in {
      method(null, "tag", defaultValue) shouldBe defaultValue
      method(tag, null, defaultValue) shouldBe defaultValue
    }
  }

  def testGetOption[T](name: String, method: (CompoundTag, String) => Option[T], tag: CompoundTag, expected: T): Unit = {
    name should "return Some(the value) if the entry exists" in {
      method(tag, "tag") shouldBe Some(expected)
    }

    it should "return None if the entry doesn't exist" in {
      method(tag, "tags") shouldBe None
    }

    it should "return None if the tag or name is null" in {
      method(null, "tag") shouldBe None
      method(tag, null) shouldBe None
    }
  }

  val booleanCTag = makeCTag(new ByteTag("tag", 1.toByte))
  val byteCTag = makeCTag(new ByteTag("tag", 42.toByte))
  val shortCTag = makeCTag(new ShortTag("tag", 4242.toShort))
  //  intCTag
  val longCTag = makeCTag(new LongTag("tag", 4242424242424242L))
  //  floatCTag
  val doubleCTag = makeCTag(new DoubleTag("tag", 42.42424242424242D))
  val stringCTag = makeCTag(new StringTag("tag", "42.42424242424242D"))
  val compCTag = makeCTag(makeCTag(new ByteTag("tag", 42.toByte)))
  val byteArr = Seq(42, 13, 42, -42).map(_.toByte).toArray
  val byteArrCTag = makeCTag(new ByteArrayTag("tag", byteArr))
  val shortArr = Seq(4242, 1313, 4242, -4242).map(_.toShort).toArray
  val shortArrCTag = makeCTag(new ShortArrayTag("tag", shortArr))
  val listCTag = makeCTag(new ListTag("tag", classOf[StringTag], java.util.Arrays.asList(
    new StringTag("", "a"), new StringTag("", "bcd"))))

  testGet("getBoolean", NBTUtil.getBoolean, booleanCTag, true, false)
  testGet("getByte", NBTUtil.getByte, byteCTag, 42.toByte, 13.toByte)
  testGet("getShort", NBTUtil.getShort, shortCTag, 4242.toShort, 13.toShort)

  testGet("getLong", NBTUtil.getLong, longCTag, 4242424242424242L, 13L)

  testGet("getDouble", NBTUtil.getDouble, doubleCTag, 42.42424242424242D, 13D)
  testGet("getString", NBTUtil.getString, stringCTag, "42.42424242424242D", "13")
  testGetOption("getString (Option)", NBTUtil.getString, stringCTag, "42.42424242424242D")
  testGetOption("getList", NBTUtil.getList, listCTag, Seq(
    new StringTag("", "a"), new StringTag("", "bcd")))
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

  "setVector" should "set the correct values" in {
    val cTag = makeCTag(
      new DoubleTag("x", 1.23),
      new DoubleTag("z", 4.32),
      new DoubleTag("y", 9.87))
    val vec = new Vector3d()

    NBTUtil.setVector(cTag, vec) shouldBe vec

    vec.x shouldBe 1.23
    vec.y shouldBe 9.87
    vec.z shouldBe 4.32
  }

  "makeVectorTag" should "set the correct values" in {
    val vec = new Vector3d(1.23, 9.87, 4.32)

    val cTag = NBTUtil.makeVectorTag("tag", vec)

    cTag shouldBe makeCTag(
      new DoubleTag("x", 1.23),
      new DoubleTag("y", 9.87),
      new DoubleTag("z", 4.32))
  }

  val bigTag = makeCTag(new ByteTag("byteTag", 13.toByte), makeCTag(new ShortTag("shortTag", 1212.toShort)))
  "makeCompoundTag" should "return a CompoundTag with the provided tag in it" in {
    NBTUtil.makeCompoundTag("tag", Seq(bigTag)) shouldBe makeCTag(bigTag)
  }
}
