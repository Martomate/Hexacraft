package com.martomate.hexacraft.gui.comp

import com.martomate.hexacraft.gui.location.LocationInfo

class Label(text: String, _location: LocationInfo, textSize: Float, centered: Boolean = true) extends Component(_location) {
  private val guiText = Component.makeText(text, location, textSize, centered)
  addText(guiText)

  def withColor(r: Float, g: Float, b: Float): Label = {
    guiText.setColour(r, g, b)
    this
  }
}
