package com.martomate.hexacraft.gui

import org.lwjgl.glfw.GLFW

enum Event:
  case KeyEvent(key: Int, scancode: Int, action: Event.KeyAction, mods: Event.KeyMods)
  case CharEvent(character: Int)
  case MouseClickEvent(
      button: Event.MouseButton,
      action: Event.MouseAction,
      mods: Event.KeyMods,
      mousePos: (Float, Float)
  )
  case ScrollEvent(xOffset: Float, yOffset: Float)

object Event:
  extension (e: Event.MouseClickEvent)
    def withMouseTranslation(dx: Float, dy: Float): MouseClickEvent =
      e.copy(mousePos = (e.mousePos._1 + dx, e.mousePos._2 + dy))

  enum KeyAction:
    case Press
    case Release
    case Repeat

  object KeyAction:
    def fromGlfw(action: Int): KeyAction =
      action match
        case GLFW.GLFW_PRESS   => Event.KeyAction.Press
        case GLFW.GLFW_RELEASE => Event.KeyAction.Release
        case GLFW.GLFW_REPEAT  => Event.KeyAction.Repeat

  enum MouseAction:
    case Press
    case Release

  object MouseAction:
    def fromGlfw(action: Int): MouseAction =
      action match
        case GLFW.GLFW_PRESS   => Event.MouseAction.Press
        case GLFW.GLFW_RELEASE => Event.MouseAction.Release

  enum MouseButton:
    case Left
    case Right
    case Middle
    case Other(button: Int)

  object MouseButton:
    def fromGlfw(button: Int): MouseButton =
      button match
        case GLFW.GLFW_MOUSE_BUTTON_LEFT   => MouseButton.Left
        case GLFW.GLFW_MOUSE_BUTTON_RIGHT  => MouseButton.Right
        case GLFW.GLFW_MOUSE_BUTTON_MIDDLE => MouseButton.Middle
        case _                             => MouseButton.Other(button)

  opaque type KeyMods = Int

  object KeyMods:
    def fromGlfw(mods: Int): KeyMods = mods

  extension (mods: KeyMods)
    def shiftDown: Boolean = (mods & GLFW.GLFW_MOD_SHIFT) != 0
    def ctrlDown: Boolean = (mods & GLFW.GLFW_MOD_CONTROL) != 0
    def altDown: Boolean = (mods & GLFW.GLFW_MOD_ALT) != 0
