package hexacraft.renderer

/** @param fileName
  *   The name of the shader file. Default is the same as `name`
  * @param attribs
  *   A list of all inputs to the first shader stage. Note: matrices take several spots
  * @param defines
  *   A list of #define statements to include in the beginning of the shader file
  */
case class ShaderConfig(fileName: String, attribs: Seq[String], defines: Seq[(String, String)]):
  def withAttribs(attribs: String*): ShaderConfig = copy(attribs = attribs)
  def withDefines(defines: (String, String)*): ShaderConfig = copy(defines = defines)

object ShaderConfig:
  def apply(fileName: String): ShaderConfig = ShaderConfig(fileName, Nil, Nil)
