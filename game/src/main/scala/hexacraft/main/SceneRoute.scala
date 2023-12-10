package hexacraft.main

import hexacraft.world.settings.WorldSettings

import java.io.File
import java.net.InetAddress

enum SceneRoute {
  case Main
  case WorldChooser
  case NewWorld
  case Multiplayer
  case JoinWorld
  case HostWorld
  case Settings
  case Game(
      saveDir: File,
      settings: WorldSettings,
      isHosting: Boolean,
      isOnline: Boolean,
      serverLocation: (InetAddress, Int)
  )
}
