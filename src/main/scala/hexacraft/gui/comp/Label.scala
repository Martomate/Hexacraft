package hexacraft.gui.comp

import hexacraft.gui.LocationInfo

class Label(text: String, location: LocationInfo, textSize: Float, centered: Boolean = true) extends Component:

  private val guiText = Component.makeText(text, location, textSize, centered, shadow = true)
  addText(guiText)

  def withColor(r: Float, g: Float, b: Float): Label =
    guiText.setColor(r, g, b)
    this
