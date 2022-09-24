package com.martomate.hexacraft.gui.comp

import com.martomate.hexacraft.gui.LocationInfo

class Label(text: String, location: LocationInfo, textSize: Float, centered: Boolean = true)
    extends Component:

  private val guiText = Component.makeText(text, location, textSize, centered)
  addText(guiText)

  def withColor(r: Float, g: Float, b: Float): Label =
    guiText.setColor(r, g, b)
    this
