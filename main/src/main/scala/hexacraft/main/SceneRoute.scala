package hexacraft.main

import hexacraft.world.WorldSettings

import java.io.File
import java.util.UUID

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
      settings: WorldSettings,
      isHosting: Boolean,
      isOnline: Boolean,
      serverLocation: (String, Int)
  )
}
