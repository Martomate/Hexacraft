package com.martomate.hexacraft.gui.menu.main

import java.io.{File, FileInputStream}

import com.flowpowered.nbt.CompoundTag
import com.flowpowered.nbt.stream.NBTInputStream
import com.martomate.hexacraft.Main
import com.martomate.hexacraft.gui.comp.{Button, Label, ScrollPane}
import com.martomate.hexacraft.gui.location.LocationInfo
import com.martomate.hexacraft.gui.menu.MenuScene
import com.martomate.hexacraft.scene.GameScene
import com.martomate.hexacraft.util.NBTUtil
import com.martomate.hexacraft.world.WorldSettings

class WorldChooserMenu extends MenuScene {
  addComponent(new Label("Choose world", LocationInfo(0, 0.85f, 1, 0.15f), 6).withColor(1, 1, 1))
  private val scrollPane = new ScrollPane(LocationInfo(0.285f, 0.225f, 0.43f, 0.635f))
  getWorlds.zipWithIndex.map {
    case (f, i) =>
      new Button(f.name, LocationInfo(0.3f, 0.75f - 0.1f * i, 0.4f, 0.075f))({
        Main.popScenesUntilMainMenu()
        Main.pushScene(new GameScene(f.saveFile, WorldSettings.none))
      })
  }.foreach(b => scrollPane.addComponent(b))
  addComponent(scrollPane)

  addComponent(new Button("Back to menu", LocationInfo(0.3f, 0.1f, 0.19f, 0.1f))({ Main.popScene() }))
  addComponent(new Button("New world", LocationInfo(0.51f, 0.1f, 0.19f, 0.1f))({ Main.pushScene(new NewWorldMenu) }))

  private def getWorlds: Seq[WorldInfo] = {
    val baseFolder = new File(Main.saveFolder, "saves")
    if (baseFolder.exists()) baseFolder.listFiles().filter(f => new File(f, "world.dat").exists()).map(saveFile => WorldInfo(saveFile))
    else Seq.empty[WorldInfo]
  }

}

case class WorldInfo(saveFile: File, name: String)

object WorldInfo {
  def apply(saveFile: File): WorldInfo = {
    val nbtFile = new File(saveFile, "world.dat")
    val stream = new NBTInputStream(new FileInputStream(nbtFile))
    val nbt = stream.readTag().asInstanceOf[CompoundTag]
    stream.close()

    val name = NBTUtil.getCompoundTag(nbt, "general").flatMap(general => NBTUtil.getString(general, "worldName")).getOrElse(saveFile.getName)
    new WorldInfo(saveFile, name)
  }
}