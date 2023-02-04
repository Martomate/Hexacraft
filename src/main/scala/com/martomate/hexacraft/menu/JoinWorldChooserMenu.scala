package com.martomate.hexacraft.menu

import com.martomate.hexacraft.gui.{GameWindowExtended, LocationInfo, MenuScene}
import com.martomate.hexacraft.gui.comp.{Button, Label, ScrollPane}

import java.net.InetAddress
import scala.util.Random

object JoinWorldChooserMenu {
  case class OnlineWorldInfo(id: Long, name: String, description: String)
  case class OnlineWorldConnectionDetails(address: InetAddress, port: Int, time: Long)
}

class JoinWorldChooserMenu(implicit window: GameWindowExtended) extends MenuScene {
  import JoinWorldChooserMenu.*

  addComponent(
    new Label("Choose world", LocationInfo.from16x9(0, 0.85f, 1, 0.15f), 6).withColor(1, 1, 1)
  )
  private val scrollPane = new ScrollPane(LocationInfo.from16x9(0.285f, 0.225f, 0.43f, 0.635f))
  addComponent(scrollPane)

  addComponent(Button("Back to menu", LocationInfo.from16x9(0.3f, 0.05f, 0.4f, 0.1f)) {
    window.scenes.popScene()
  })

  updateServerList()

  private def updateServerList(): Unit = {
    getWorlds.zipWithIndex
      .map { case (f, i) =>
        Button(f.name, LocationInfo.from16x9(0.3f, 0.75f - 0.1f * i, 0.4f, 0.075f)) {
          val connectionDetails = loadOnlineWorld(f.id)
          println("Will connect to: " + connectionDetails)
//          window.scenes.popScenesUntil(MenuScene.isMainMenu)
//          window.scenes.pushScene(new GameScene(f.saveFile, WorldSettings.none))
        }
      }
      .foreach(b => scrollPane.addComponent(b))
  }

  private def getWorlds: Seq[OnlineWorldInfo] = {
    Seq(
      OnlineWorldInfo(new Random().nextLong(), "Test Online World", "Welcome to my test world!"),
      OnlineWorldInfo(new Random().nextLong(), "Another Online World", "Free bitcakes!")
    )
  }

  def loadOnlineWorld(id: Long): OnlineWorldConnectionDetails = {
    // TODO: connect to the server registry to get this information
    OnlineWorldConnectionDetails(
      InetAddress.getByName("localhost"),
      2345,
      System.currentTimeMillis() + 10
    )
  }
}
