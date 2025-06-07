package hexacraft.gui.comp

import hexacraft.gui.LocationInfo
import hexacraft.text.Text

class Label(guiText: Text, override val bounds: LocationInfo) extends Component with Boundable {
  addText(guiText)

  def withColor(r: Float, g: Float, b: Float): Label = {
    guiText.setColor(r, g, b)
    this
  }
}

object Label {
  def apply(text: String, location: LocationInfo, textSize: Float, centered: Boolean = true): Label = {
    new Label(Component.makeText(text, location, textSize, centered, shadow = true), location)
  }
}
