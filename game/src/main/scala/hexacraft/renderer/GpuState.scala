package hexacraft.renderer

import hexacraft.infra.gpu.OpenGL

object GpuState {
  def of(states: (OpenGL.State, Boolean)*): GpuState =
    GpuState(
      enabled = states.filter(_._2).map(_._1),
      disabled = states.filter(!_._2).map(_._1)
    )
}

case class GpuState(enabled: Seq[OpenGL.State] = Nil, disabled: Seq[OpenGL.State] = Nil) {
  def set(): Unit =
    for s <- enabled do OpenGL.glEnable(s)
    for s <- disabled do OpenGL.glDisable(s)

  def unset(): Unit =
    for s <- enabled do OpenGL.glDisable(s)
    for s <- disabled do OpenGL.glEnable(s)
}
