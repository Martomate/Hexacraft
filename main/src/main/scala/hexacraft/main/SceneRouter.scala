package hexacraft.main

import hexacraft.gui.Scene
import hexacraft.util.Channel

trait SceneRouter {
  def route(sceneRoute: SceneRoute): (Scene, Channel.Receiver[SceneRouter.Event])
}

object SceneRouter {
  enum Event {
    case ChangeScene(route: SceneRoute)
    case QuitRequested
  }
}
