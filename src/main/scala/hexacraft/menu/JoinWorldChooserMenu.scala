package hexacraft.menu

import hexacraft.{GameMouse, GameWindow}
import hexacraft.gui.{LocationInfo, MenuScene}
import hexacraft.gui.comp.{Button, Label, ScrollPane}

import java.net.InetAddress
import scala.util.Random

object JoinWorldChooserMenu {
  enum Event:
    case Join(address: InetAddress, port: Int)
    case GoBack

  private case class OnlineWorldInfo(id: Long, name: String, description: String)
  private case class OnlineWorldConnectionDetails(address: InetAddress, port: Int, time: Long)
}

class JoinWorldChooserMenu(onEvent: JoinWorldChooserMenu.Event => Unit)(using GameMouse, GameWindow) extends MenuScene {
  import JoinWorldChooserMenu.*

  addComponent(new Label("Choose world", LocationInfo.from16x9(0, 0.85f, 1, 0.15f), 6).withColor(1, 1, 1))
  private val scrollPane = new ScrollPane(LocationInfo.from16x9(0.285f, 0.225f, 0.43f, 0.635f), 0.025f * 2)
  addComponent(scrollPane)

  addComponent(Button("Back to menu", LocationInfo.from16x9(0.3f, 0.05f, 0.4f, 0.1f))(onEvent(Event.GoBack)))

  updateServerList()

  private def updateServerList(): Unit =
    for (f, i) <- getWorlds.zipWithIndex
    do
      scrollPane.addComponent(
        Button(f.name, LocationInfo.from16x9(0.3f, 0.75f - 0.1f * i, 0.4f, 0.075f)) {
          val connectionDetails = loadOnlineWorld(f.id)
          onEvent(Event.Join(connectionDetails.address, connectionDetails.port))
        }
      )

  private def getWorlds: Seq[OnlineWorldInfo] =
    Seq(
      OnlineWorldInfo(new Random().nextLong(), "Test Online World", "Welcome to my test world!"),
      OnlineWorldInfo(new Random().nextLong(), "Another Online World", "Free bitcakes!")
    )

  private def loadOnlineWorld(id: Long): OnlineWorldConnectionDetails =
    // TODO: connect to the server registry to get this information
    OnlineWorldConnectionDetails(
      InetAddress.getByName("localhost"),
      2345,
      System.currentTimeMillis() + 10
    )
}
