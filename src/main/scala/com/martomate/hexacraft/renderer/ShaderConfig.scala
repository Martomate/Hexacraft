package com.martomate.hexacraft.renderer

/** @param name
  *   The name used to get this shader using `Shader.get`
  * @param fileName
  *   The name of the shader file. Default is the same as `name`
  * @param attribs
  *   A list of all inputs to the first shader stage. Note: matrices take several spots
  * @param defines
  *   A list of #define statements to include in the beginning of the shader file
  */
case class ShaderConfig(
    name: String,
    fileName: String,
    attribs: Seq[String],
    defines: Seq[(String, String)]
) {
  def withAttribs(attribs: String*): ShaderConfig =
    copy(attribs = attribs)

  def withDefines(defines: (String, String)*): ShaderConfig =
    copy(defines = defines)
}

object ShaderConfig {
  def apply(name: String, fileName: String): ShaderConfig =
    ShaderConfig(name, if (fileName != null) fileName else name, Seq(), Seq())
}
