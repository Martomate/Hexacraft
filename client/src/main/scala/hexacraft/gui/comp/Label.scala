package hexacraft.gui.comp

import hexacraft.gui.{LocationInfo, TickContext}
import hexacraft.text.Text

class Label(guiText: Text, override val bounds: LocationInfo) extends Component with Boundable {
  addText(guiText)

  def withColor(r: Float, g: Float, b: Float): Label = {
    guiText.setColor(r, g, b)
    this
  }

  def withFade(fadeTimeMs: Long, fadeDurationMs: Long) = new Label(guiText, bounds) {
    override def tick(ctx: TickContext): Unit = {
      val now = System.currentTimeMillis()
      if now > fadeTimeMs then {
        if now > fadeTimeMs + fadeDurationMs then {
          guiText.setOpacity(0)
        } else {
          val a = 1.0f - (now - fadeTimeMs).toFloat / fadeDurationMs
          guiText.setOpacity(a)
        }
      }
    }
  }
}

object Label {
  def apply(text: String, location: LocationInfo, textSize: Float, centered: Boolean = true): Label = {
    new Label(Component.makeText(text, location, textSize, centered, shadow = true), location)
  }
}
