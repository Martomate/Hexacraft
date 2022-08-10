package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.world.entity.EntityModel

case class EntityDataForShader(model: EntityModel, parts: Seq[EntityPartDataForShader])
