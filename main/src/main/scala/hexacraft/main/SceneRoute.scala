package hexacraft.main

import java.io.File

enum SceneRoute {
  case Main
  case WorldChooser
  case NewWorld
  case Multiplayer
  case JoinWorld
  case HostWorld
  case ChoosePlayerName(next: SceneRoute)
  case AddServer
  case Settings
  case Game(
      saveDir: File,
      isHosting: Boolean,
      isOnline: Boolean,
      serverLocation: (String, Int)
  )
}
