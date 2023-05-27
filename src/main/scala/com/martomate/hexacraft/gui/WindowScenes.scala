package com.martomate.hexacraft.gui

trait WindowScenes {
  def popScene(): Unit
  def pushScene(scene: Scene): Unit
  def popScenesUntil(predicate: Scene => Boolean): Unit
}
