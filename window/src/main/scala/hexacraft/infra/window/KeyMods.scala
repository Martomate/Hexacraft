package hexacraft.infra.window

import org.lwjgl.glfw.GLFW

opaque type KeyMods = Int

object KeyMods {
  def fromGlfw(mods: Int): KeyMods = mods

  def none: KeyMods = 0

  extension (mods: KeyMods)
    def shiftDown: Boolean = (mods & GLFW.GLFW_MOD_SHIFT) != 0
    def ctrlDown: Boolean = (mods & GLFW.GLFW_MOD_CONTROL) != 0
    def altDown: Boolean = (mods & GLFW.GLFW_MOD_ALT) != 0
    def superDown: Boolean = (mods & GLFW.GLFW_MOD_SUPER) != 0
}
