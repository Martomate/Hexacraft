package com.martomate.hexacraft.gui

import com.martomate.hexacraft.gui.SceneStack
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SceneStackTest extends AnyFlatSpec with Matchers {
  def makeStack = new SceneStack

  "pushScene" should "push the given scene"
  it should "add the scene even if it already exists in the stack"

  "popScene" should "pop the scene on top"
  it should "do nothing if the stack is empty"

  "popScenesUntil" should "pop scenes from top to bottom as long as the given predicate is fulfilled"

  "iterator" should "iterate over the scenes in the order they were added"

  "length" should "return the number of scenes on the stack"

  "apply(i)" should "be identical to the iterator moving i steps"
}
