package com.martomate.hexacraft.renderer

object Shaders {
  object ShaderNames {
    val Block: String = "block"
    val BlockSide: String = "blockSide"
    val Entity: String = "entity"
    val EntitySide: String = "entitySide"
    val GuiBlock: String = "gui_block"
    val GuiBlockSide: String = "gui_blockSide"
    val SelectedBlock: String = "selected_block"
    val Sky: String = "sky"
    val Crosshair: String = "crosshair"
    val Image: String = "image"
    val Color: String = "color"
    val Font: String = "font"
    val WorldCombiner: String = "world_combiner"
  }

  def registerAll(): Unit = {
    Shader.register(
      ShaderConfig(ShaderNames.Block, "block")
        .withAttribs(
          "position",
          "texCoords",
          "normal",
          "vertexIndex",
          "faceIndex",
          "blockPos",
          "blockTex",
          "blockHeight",
          "brightness"
        )
        .withDefines("isSide" -> "0")
    )
    Shader.register(
      ShaderConfig(ShaderNames.BlockSide, "block")
        .withAttribs(
          "position",
          "texCoords",
          "normal",
          "vertexIndex",
          "faceIndex",
          "blockPos",
          "blockTex",
          "blockHeight",
          "brightness"
        )
        .withDefines("isSide" -> "1")
    )
    Shader.register(
      ShaderConfig(ShaderNames.Entity, "entity_part")
        .withAttribs(
          "position",
          "texCoords",
          "normal",
          "vertexIndex",
          "faceIndex",
          "modelMatrix",
          "",
          "",
          "",
          "texOffset",
          "texDim",
          "blockTex",
          "brightness"
        )
        .withDefines("isSide" -> "0")
    )
    Shader.register(
      ShaderConfig(ShaderNames.EntitySide, "entity_part")
        .withAttribs(
          "position",
          "texCoords",
          "normal",
          "vertexIndex",
          "faceIndex",
          "modelMatrix",
          "",
          "",
          "",
          "texOffset",
          "texDim",
          "blockTex",
          "brightness"
        )
        .withDefines("isSide" -> "1")
    )
    Shader.register(
      ShaderConfig(ShaderNames.GuiBlock, "gui_block")
        .withAttribs(
          "position",
          "texCoords",
          "normal",
          "vertexIndex",
          "faceIndex",
          "blockPos",
          "blockTex",
          "blockHeight",
          "brightness"
        )
        .withDefines("isSide" -> "0")
    )
    Shader.register(
      ShaderConfig(ShaderNames.GuiBlockSide, "gui_block")
        .withAttribs(
          "position",
          "texCoords",
          "normal",
          "vertexIndex",
          "faceIndex",
          "blockPos",
          "blockTex",
          "blockHeight",
          "brightness"
        )
        .withDefines("isSide" -> "1")
    )

    Shader.register(
      ShaderConfig(ShaderNames.SelectedBlock, "selected_block")
        .withAttribs("position", "blockPos", "color", "blockHeight")
    )
    Shader.register(ShaderConfig(ShaderNames.Sky, "sky").withAttribs("position"))
    Shader.register(ShaderConfig(ShaderNames.Crosshair, "crosshair").withAttribs("position"))
    Shader.register(ShaderConfig(ShaderNames.Image, "image").withAttribs("position"))
    Shader.register(ShaderConfig(ShaderNames.Color, "color").withAttribs("position"))
    Shader.register(ShaderConfig(ShaderNames.Font, "font").withAttribs("position", "textureCoords"))
    Shader.register(
      ShaderConfig(ShaderNames.WorldCombiner, "world_combiner").withAttribs("position")
    )
  }
}
