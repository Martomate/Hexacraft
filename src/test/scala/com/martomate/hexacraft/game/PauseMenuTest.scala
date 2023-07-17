package com.martomate.hexacraft.game

import com.martomate.hexacraft.GameMouse
import com.martomate.hexacraft.gui.Event
import com.martomate.hexacraft.infra.gpu.OpenGL
import com.martomate.hexacraft.infra.window.{KeyAction, KeyboardKey, KeyMods, MouseAction, MouseButton}
import com.martomate.hexacraft.main.RealGameMouse
import com.martomate.hexacraft.util.Tracker

import munit.FunSuite

class PauseMenuTest extends FunSuite {
  test("Emits Unpause event when Escape key is pressed") {
    OpenGL._enterTestMode()

    given GameMouse = new RealGameMouse
    val tracker = Tracker.withStorage[PauseMenu.Event]
    val menu = new PauseMenu(tracker)

    menu.handleEvent(Event.KeyEvent(KeyboardKey.Escape, 0, KeyAction.Press, KeyMods.none))

    assertEquals(tracker.events, Seq(PauseMenu.Event.Unpause))
  }

  test("Emits Unpause event when Back to Game button is pressed") {
    OpenGL._enterTestMode()

    given GameMouse = new RealGameMouse

    val tracker = Tracker.withStorage[PauseMenu.Event]
    val menu = new PauseMenu(tracker)

    menu.handleEvent(Event.MouseClickEvent(MouseButton.Left, MouseAction.Press, KeyMods.none, (0, 0.2f)))
    menu.handleEvent(Event.MouseClickEvent(MouseButton.Left, MouseAction.Release, KeyMods.none, (0, 0.2f)))

    assertEquals(tracker.events, Seq(PauseMenu.Event.Unpause))
  }

  test("Emits QuitGame event when Back to Menu button is pressed") {
    OpenGL._enterTestMode()

    given GameMouse = new RealGameMouse

    val tracker = Tracker.withStorage[PauseMenu.Event]
    val menu = new PauseMenu(tracker)

    menu.handleEvent(Event.MouseClickEvent(MouseButton.Left, MouseAction.Press, KeyMods.none, (0, -0.4f)))
    menu.handleEvent(Event.MouseClickEvent(MouseButton.Left, MouseAction.Release, KeyMods.none, (0, -0.4f)))

    assertEquals(tracker.events, Seq(PauseMenu.Event.QuitGame))
  }
}
