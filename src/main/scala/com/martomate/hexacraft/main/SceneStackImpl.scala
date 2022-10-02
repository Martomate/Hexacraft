package com.martomate.hexacraft.main

import com.martomate.hexacraft.gui.{Scene, SceneStack}

import scala.collection.mutable.ArrayBuffer

class SceneStackImpl extends SceneStack:
  private val sceneList = ArrayBuffer.empty[Scene]

  def pushScene(scene: Scene): Unit =
    if scene != null
    then sceneList += scene

  def popScene(): Unit =
    if sceneList.nonEmpty
    then
      val index = sceneList.size - 1
      sceneList(index).unload()
      sceneList.remove(index)

  def popScenesUntil(predicate: Scene => Boolean): Unit =
    while sceneList.nonEmpty && !predicate(sceneList.last)
    do popScene()

  override def iterator: Iterator[Scene] = sceneList.iterator

  override def length: Int = sceneList.length

  override def apply(idx: Int): Scene = sceneList(idx)
