package com.martomate.hexacraft.gui

import com.martomate.hexacraft.gui.SceneStack

import munit.FunSuite

class SceneStackTest extends FunSuite {
  def makeStack = new SceneStack

  test("pushScene should push the given scene".ignore) {}
  test("pushScene should add the scene even if it already exists in the stack".ignore) {}

  test("popScene should pop the scene on top".ignore) {}
  test("popScene should do nothing if the stack is empty".ignore) {}

  test("popScenesUntil should pop scenes from top to bottom as long as the given predicate is fulfilled".ignore) {}

  test("iterator should iterate over the scenes in the order they were added".ignore) {}

  test("length should return the number of scenes on the stack".ignore) {}

  test("apply(i) should be identical to the iterator moving i steps".ignore) {}
}
