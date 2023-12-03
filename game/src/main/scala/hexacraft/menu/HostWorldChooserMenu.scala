package hexacraft.menu

import hexacraft.gui.{LocationInfo, MenuScene}
import hexacraft.gui.comp.{Button, Label, ScrollPane}
import hexacraft.infra.fs.FileSystem
import hexacraft.menu.HostWorldChooserMenu.Event

import java.io.File

object HostWorldChooserMenu {
  enum Event:
    case Host(worldInfo: WorldInfo)
    case GoBack
}

class HostWorldChooserMenu(saveFolder: File, fs: FileSystem)(onEvent: HostWorldChooserMenu.Event => Unit)
    extends MenuScene {
  addComponent(new Label("Choose world", LocationInfo.from16x9(0, 0.85f, 1, 0.15f), 6).withColor(1, 1, 1))

  private val scrollPane = new ScrollPane(LocationInfo.from16x9(0.285f, 0.225f, 0.43f, 0.635f), 0.025f * 2)

  for (f, i) <- getWorlds.zipWithIndex
  do
    scrollPane.addComponent(
      Button(f.name, LocationInfo.from16x9(0.3f, 0.75f - 0.1f * i, 0.4f, 0.075f)) {
        onEvent(Event.Host(f))
        // TODO: the network manager should repeatedly connect to the server registry.
        //  This will be blocking until a client wants to connect or after a timeout
        //  If this is not done in a certain time period the server will be deregistered from the server registry
      }
    )
  addComponent(scrollPane)

  addComponent(Button("Back to menu", LocationInfo.from16x9(0.3f, 0.05f, 0.4f, 0.1f))(onEvent(Event.GoBack)))

  private def getWorlds: Seq[WorldInfo] =
    val baseFolder = new File(saveFolder, "saves")
    if baseFolder.exists() then
      baseFolder
        .listFiles()
        .filter(f => new File(f, "world.dat").exists())
        .map(saveFile => WorldInfo.fromFile(saveFile, fs))
        .toSeq
    else Seq.empty[WorldInfo]

}
