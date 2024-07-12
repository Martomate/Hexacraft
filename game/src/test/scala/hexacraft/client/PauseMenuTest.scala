package hexacraft.client

import hexacraft.gui.Event
import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.window.*
import hexacraft.util.{Channel, Tracker}

import munit.FunSuite

class PauseMenuTest extends FunSuite {
  test("Emits Unpause event when Escape key is pressed") {
    OpenGL._enterTestMode()

    val (tx, rx) = Channel[PauseMenu.Event]()
    val tracker = Tracker.fromRx(rx)
    val menu = new PauseMenu(tx)

    menu.handleEvent(Event.KeyEvent(KeyboardKey.Escape, 0, KeyAction.Press, KeyMods.none))

    assertEquals(tracker.events, Seq(PauseMenu.Event.Unpause))
  }

  test("Emits Unpause event when Back to Game button is pressed") {
    OpenGL._enterTestMode()

    val (tx, rx) = Channel[PauseMenu.Event]()
    val tracker = Tracker.fromRx(rx)
    val menu = new PauseMenu(tx)

    menu.handleEvent(Event.MouseClickEvent(MouseButton.Left, MouseAction.Press, KeyMods.none, (0, 0.2f)))
    menu.handleEvent(Event.MouseClickEvent(MouseButton.Left, MouseAction.Release, KeyMods.none, (0, 0.2f)))

    assertEquals(tracker.events, Seq(PauseMenu.Event.Unpause))
  }

  test("Emits QuitGame event when Back to Menu button is pressed") {
    OpenGL._enterTestMode()

    val (tx, rx) = Channel[PauseMenu.Event]()
    val tracker = Tracker.fromRx(rx)
    val menu = new PauseMenu(tx)

    menu.handleEvent(Event.MouseClickEvent(MouseButton.Left, MouseAction.Press, KeyMods.none, (0, -0.4f)))
    menu.handleEvent(Event.MouseClickEvent(MouseButton.Left, MouseAction.Release, KeyMods.none, (0, -0.4f)))

    assertEquals(tracker.events, Seq(PauseMenu.Event.QuitGame))
  }
}
