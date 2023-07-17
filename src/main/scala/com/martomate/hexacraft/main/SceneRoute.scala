package com.martomate.hexacraft.main

import com.martomate.hexacraft.world.settings.WorldSettings

import java.io.File

enum SceneRoute {
  case Main
  case WorldChooser
  case NewWorld
  case Multiplayer
  case JoinWorld
  case HostWorld
  case Settings
  case Game(saveFile: File, settings: WorldSettings)
}
