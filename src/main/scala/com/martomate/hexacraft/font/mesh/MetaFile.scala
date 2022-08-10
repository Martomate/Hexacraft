package com.martomate.hexacraft.font.mesh

import com.martomate.hexacraft.util.FileUtils

import java.io.{BufferedReader, IOException}
import java.net.URL
import java.util
import java.util.{HashMap, Map}

/**
 * Provides functionality for getting the values from a font file.
 *
 * @author Karl
 *
 */
object MetaFile {
  private val PAD_TOP: Int = 0
  private val PAD_LEFT: Int = 1
  private val PAD_BOTTOM: Int = 2
  private val PAD_RIGHT: Int = 3
  private val DESIRED_PADDING: Int = 3
  private val SPLITTER: String = " "
  private val NUMBER_SEPARATOR: String = ","
}

/**
 * Opens a font file in preparation for reading.
 *
 * @param file
 *            - the font file.
 */
class MetaFile (val file: URL) {
  private var verticalPerPixelSize: Double = .0
  private var horizontalPerPixelSize: Double = .0
  private var spaceWidth: Double = .0
  private var padding: Array[Int] = null
  private var paddingWidth: Int = 0
  private var paddingHeight: Int = 0
  private val metaData: util.Map[Integer, Character] = new util.HashMap[Integer, Character]
  private var reader: BufferedReader = null
  private val values: util.Map[String, String] = new util.HashMap[String, String]

  openFile(file)
  loadPaddingData()
  loadLineSizes()
  loadCharacterData(getValueOfVariable("scaleW"))
  close()

  def getSpaceWidth: Double = spaceWidth

  def getCharacter(ascii: Int): Character = metaData.get(ascii)

  /**
   * Read in the next line and store the variable values.
   *
   * @return {@code true} if the end of the file hasn't been reached.
   */
  private def processNextLine: Boolean = {
    values.clear()
    var line: String = null
    try line = reader.readLine
    catch {
      case e1: IOException =>

    }
    if (line == null) return false
    for (part <- line.split(MetaFile.SPLITTER)) {
      val valuePairs: Array[String] = part.split("=")
      if (valuePairs.length == 2) values.put(valuePairs(0), valuePairs(1))
    }
    true
  }

  /**
   * Gets the {@code int} value of the variable with a certain name on the
   * current line.
   *
   * @param variable
   *            - the name of the variable.
   * @return The value of the variable.
   */
  private def getValueOfVariable(variable: String): Int = values.get(variable).toInt

  /**
   * Gets the array of ints associated with a variable on the current line.
   *
   * @param variable
   *            - the name of the variable.
   * @return The int array of values associated with the variable.
   */
  private def getValuesOfVariable(variable: String): Array[Int] = {
    val numbers: Array[String] = values.get(variable).split(MetaFile.NUMBER_SEPARATOR)
    val actualValues: Array[Int] = new Array[Int](numbers.length)
    for (i <- 0 until actualValues.length) {
      actualValues(i) = numbers(i).toInt
    }
    actualValues
  }

  /**
   * Closes the font file after finishing reading.
   */
  private def close(): Unit = {
    try reader.close()
    catch {
      case e: IOException =>
        e.printStackTrace()
    }
  }

  /**
   * Opens the font file, ready for reading.
   *
   * @param file
   *            - the font file.
   */
  private def openFile(file: URL): Unit = {
    try reader = FileUtils.getBufferedReader(file)
    catch {
      case e: Exception =>
        e.printStackTrace()
        System.err.println("Couldn't read font meta file!")
    }
  }

  /**
   * Loads the data about how much padding is used around each character in
   * the texture atlas.
   */
  private def loadPaddingData(): Unit = {
    processNextLine
    this.padding = getValuesOfVariable("padding")
    this.paddingWidth = padding(MetaFile.PAD_LEFT) + padding(MetaFile.PAD_RIGHT)
    this.paddingHeight = padding(MetaFile.PAD_TOP) + padding(MetaFile.PAD_BOTTOM)
  }

  /**
   * Loads information about the line height for this font in pixels, and uses
   * this as a way to find the conversion rate between pixels in the texture
   * atlas and screen-space.
   */
  private def loadLineSizes(): Unit = {
    processNextLine
    val lineHeightPixels: Int = getValueOfVariable("lineHeight") - paddingHeight
    verticalPerPixelSize = TextMeshCreator.LINE_HEIGHT / lineHeightPixels.toDouble
    horizontalPerPixelSize = verticalPerPixelSize
  }

  /**
   * Loads in data about each character and stores the data in the
   * {@link Character} class.
   *
   * @param imageWidth
   *            - the width of the texture atlas in pixels.
   */
  private def loadCharacterData(imageWidth: Int): Unit = {
    processNextLine
    processNextLine
    while ( {
      processNextLine
    }) {
      val c: Character = loadCharacter(imageWidth)
      if (c != null) metaData.put(c.getId, c)
    }
  }

  /**
   * Loads all the data about one character in the texture atlas and converts
   * it all from 'pixels' to 'screen-space' before storing. The effects of
   * padding are also removed from the data.
   *
   * @param imageSize
   *            - the size of the texture atlas in pixels.
   * @return The data about the character.
   */
  private def loadCharacter(imageSize: Int): Character = {
    val id: Int = getValueOfVariable("id")
    if (id == TextMeshCreator.SPACE_ASCII) {
      this.spaceWidth = (getValueOfVariable("xadvance") - paddingWidth) * horizontalPerPixelSize
      return null
    }
    val xTex: Double = (getValueOfVariable("x").toDouble + (padding(MetaFile.PAD_LEFT) - MetaFile.DESIRED_PADDING)) / imageSize
    val yTex: Double = (getValueOfVariable("y").toDouble + (padding(MetaFile.PAD_TOP) - MetaFile.DESIRED_PADDING)) / imageSize
    val width: Int = getValueOfVariable("width") - (paddingWidth - (2 * MetaFile.DESIRED_PADDING))
    val height: Int = getValueOfVariable("height") - (paddingHeight - (2 * MetaFile.DESIRED_PADDING))
    val quadWidth: Double = width * horizontalPerPixelSize
    val quadHeight: Double = height * verticalPerPixelSize
    val xTexSize: Double = width.toDouble / imageSize
    val yTexSize: Double = height.toDouble / imageSize
    val xOff: Double = (getValueOfVariable("xoffset") + padding(MetaFile.PAD_LEFT) - MetaFile.DESIRED_PADDING) * horizontalPerPixelSize
    val yOff: Double = (getValueOfVariable("yoffset") + (padding(MetaFile.PAD_TOP) - MetaFile.DESIRED_PADDING)) * verticalPerPixelSize
    val xAdvance: Double = (getValueOfVariable("xadvance") - paddingWidth) * horizontalPerPixelSize
    new Character(id, xTex, yTex, xTexSize, yTexSize, xOff, yOff, quadWidth, quadHeight, xAdvance)
  }
}
