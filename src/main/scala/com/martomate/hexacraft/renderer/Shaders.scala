package com.martomate.hexacraft.renderer

object Shaders {
  object ShaderNames {
    val Block: String = "block"
    val BlockSide: String = "blockSide"
    val Entity: String = "entity"
    val EntitySide: String = "entitySide"
    val GuiBlock: String = "gui_block"
    val GuiBlockSide: String = "gui_blockSide"
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

  }
}
