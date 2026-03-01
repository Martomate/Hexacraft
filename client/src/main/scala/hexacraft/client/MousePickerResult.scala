package hexacraft.client

import hexacraft.world.block.BlockState
import hexacraft.world.coord.BlockRelWorld

case class MousePickerResult(block: BlockState, coords: BlockRelWorld, side: Option[Int])
