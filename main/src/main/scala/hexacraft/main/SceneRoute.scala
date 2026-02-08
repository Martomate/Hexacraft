package hexacraft.main

import java.io.File

trait SceneRoute

object SceneRoute {
  case object Main extends SceneRoute
  case object WorldChooser extends SceneRoute
  case object NewWorld extends SceneRoute
  case object Multiplayer extends SceneRoute
  case object JoinWorld extends SceneRoute
  case object HostWorld extends SceneRoute
  case class ChoosePlayerName(next: SceneRoute) extends SceneRoute
  case object AddServer extends SceneRoute
  case object Settings extends SceneRoute
  case class Game(
      saveDir: File,
      isHosting: Boolean,
      isOnline: Boolean,
      serverLocation: (String, Int)
  ) extends SceneRoute
}
