package com.martomate.hexacraft.world.loader

import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld

class ChunkLoadingPrioritizerSimpleTest extends ChunkLoadingPrioritizerTest {
  override def make(origin: PosAndDir,
           distSqFunc: (PosAndDir, ChunkRelWorld) => Double,
           maxDist: Double): ChunkLoadingPrioritizer =
    new ChunkLoadingPrioritizerSimple(origin, distSqFunc, maxDist)
}
