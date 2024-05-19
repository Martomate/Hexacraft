package hexacraft.renderer

import hexacraft.infra.gpu.OpenGL

import scala.collection.mutable

object GpuState {
  def build(f: Builder => Builder): GpuState = {
    val b = new Builder
    f(b)
    b.build()
  }

  class Builder private[GpuState] {
    private val states: mutable.Map[OpenGL.State, Boolean] = mutable.Map.empty

    def blend(enabled: Boolean): Builder = this.setState(OpenGL.State.Blend, enabled)
    def depthTest(enabled: Boolean): Builder = this.setState(OpenGL.State.DepthTest, enabled)
    def cullFace(enabled: Boolean): Builder = this.setState(OpenGL.State.CullFace, enabled)
    def scissorTest(enabled: Boolean): Builder = this.setState(OpenGL.State.ScissorTest, enabled)

    private def setState(key: OpenGL.State, value: Boolean): Builder = {
      this.states(key) = value
      this
    }

    private[GpuState] def build(): GpuState = {
      val (enabled, disabled) = states.partition(_._2)
      GpuState(enabled.keys.toSeq, disabled.keys.toSeq)
    }
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
