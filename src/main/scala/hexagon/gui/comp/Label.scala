package hexagon.gui.comp

class Label(text: String, _location: LocationInfo, textSize: Float, centered: Boolean = true) extends Component(_location) {
  private val guiText = Component.makeText(text, location, textSize, centered)
  addText(guiText)

  def withColor(r: Float, g: Float, b: Float): Label = {
    guiText.setColour(r, g, b)
    this
  }
}
