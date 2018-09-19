package com.martomate.hexacraft.world.column

import com.martomate.hexacraft.world.chunk.{ChunkAddedOrRemovedListener, ChunkBlockListener, ChunkEventListener}

trait ChunkColumnListener extends ChunkAddedOrRemovedListener with ChunkEventListener with ChunkBlockListener {

}
