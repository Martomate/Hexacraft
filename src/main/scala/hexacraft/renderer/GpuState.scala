package hexacraft.renderer

import hexacraft.infra.gpu.OpenGL

object GpuState {
  def withEnabled(state: OpenGL.State*): GpuState = GpuState().withEnabled(state*)
  def withDisabled(state: OpenGL.State*): GpuState = GpuState().withDisabled(state*)
}

case class GpuState(enabled: Seq[OpenGL.State] = Nil, disabled: Seq[OpenGL.State] = Nil) {
  def withEnabled(state: OpenGL.State*): GpuState = GpuState(enabled ++ state, disabled)
  def withDisabled(state: OpenGL.State*): GpuState = GpuState(enabled, disabled ++ state)

  def set(): Unit =
    for s <- enabled do OpenGL.glEnable(s)
    for s <- disabled do OpenGL.glDisable(s)

  def unset(): Unit =
    for s <- enabled do OpenGL.glDisable(s)
    for s <- disabled do OpenGL.glEnable(s)
}