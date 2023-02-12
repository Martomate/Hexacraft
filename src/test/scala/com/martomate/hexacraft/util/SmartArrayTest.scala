package com.martomate.hexacraft.util

import munit.FunSuite

class SmartArrayTest extends FunSuite {
  test("SmartArray.apply should create a SmartArray with correct default element") {
    val default = "abc"
    val arr = SmartArray(5, default)(s => new Array(s))
    assertEquals(arr(0), default)
    assertEquals(arr(4), default)
  }
  test("SmartArray.apply should use the default value if nothing else exists") {
    val default = "abc"
    val arr = SmartArray(5, default)(s => new Array(s))
    arr(1) = default
    arr(2) = "a great string"
    assertEquals(arr(0), default)
    assertEquals(arr(1), default)
    assertEquals(arr(2), "a great string")
    assertEquals(arr(4), default)
  }
  test("SmartArray.withByteArray should create a SmartArray with correct default element") {
    val default = 3.toByte
    val arr = SmartArray.withByteArray(5, default)
    assertEquals(arr(0), default)
    assertEquals(arr(4), default)
  }
  test("SmartArray.withByteArray should use the default value if nothing else exists") {
    val default = 3.toByte
    val arr = SmartArray.withByteArray(5, default)
    arr(1) = default
    arr(2) = 8.toByte
    assertEquals(arr(0), default)
    assertEquals(arr(1), default)
    assertEquals(arr(2), 8.toByte)
    assertEquals(arr(4), default)
  }
  test("length should return the length of the array") {
    val arr = SmartArray(7, "a")(s => new Array(s))
    assertEquals(arr.length, 7)
    arr(2) = "bcd"
    assertEquals(arr.length, 7)
    arr(2) = "a"
    assertEquals(arr.length, 7)
  }
}
