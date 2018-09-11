package com.martomate.hexacraft

import com.martomate.hexacraft.scene.Scene

import scala.collection.mutable.ArrayBuffer

class SceneStack extends Seq[Scene] {
  private val sceneList = ArrayBuffer.empty[Scene]
  def pushScene(scene: Scene): Unit = {
    if (scene != null) {
      sceneList += scene
    }
  }
  def popScene(): Unit = {
    if (sceneList.nonEmpty) {
      val index = sceneList.size - 1
      sceneList(index).unload()
      sceneList.remove(index)
    }
  }

  def popScenesUntil(predicate: Scene => Boolean): Unit = {
    while (sceneList.nonEmpty && !predicate(sceneList.last)) popScene()
  }

  override def iterator: Iterator[Scene] = sceneList.iterator

  override def length: Int = sceneList.length

  override def apply(idx: Int): Scene = sceneList(idx)
}
