package hexacraft.renderer

import hexacraft.infra.gpu.OpenGL

import scala.collection.mutable

object GpuState {
  def of(states: (OpenGL.State, Boolean)*): GpuState = {
    GpuState(
      enabled = states.filter(_._2).map(_._1),
      disabled = states.filter(!_._2).map(_._1)
    )
  }
}

case class GpuState(enabled: Seq[OpenGL.State] = Nil, disabled: Seq[OpenGL.State] = Nil) {
  def set(): GpuState = {
    val wereEnabled = mutable.ArrayBuffer.empty[OpenGL.State]
    val wereDisabled = mutable.ArrayBuffer.empty[OpenGL.State]

    for s <- enabled do {
      if !OpenGL.glIsEnabled(s) then {
        wereEnabled += s
      }
    }

    for s <- disabled do {
      if OpenGL.glIsEnabled(s) then {
        wereDisabled += s
      }
    }

    for s <- enabled do {
      OpenGL.glEnable(s)
    }
    for s <- disabled do {
      OpenGL.glDisable(s)
    }

    GpuState(enabled = wereEnabled.toSeq, disabled = wereDisabled.toSeq)
  }

  def unset(): Unit = {
    for s <- enabled do {
      OpenGL.glDisable(s)
    }
    for s <- disabled do {
      OpenGL.glEnable(s)
    }
  }
}
