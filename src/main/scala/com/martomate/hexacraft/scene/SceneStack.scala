package com.martomate.hexacraft.scene

trait SceneStack extends Seq[Scene] {
  def pushScene(scene: Scene): Unit

  def popScene(): Unit

  def popScenesUntil(predicate: Scene => Boolean): Unit
}
