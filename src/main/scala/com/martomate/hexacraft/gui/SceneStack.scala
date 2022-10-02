package com.martomate.hexacraft.gui

import scala.collection.Seq

trait SceneStack extends Seq[Scene] {
  def pushScene(scene: Scene): Unit

  def popScene(): Unit

  def popScenesUntil(predicate: Scene => Boolean): Unit
}
