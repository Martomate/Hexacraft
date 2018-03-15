package hexacraft.world.storage

import java.io.{File, FileInputStream}

import com.flowpowered.nbt.CompoundTag
import com.flowpowered.nbt.stream.NBTInputStream
import hexacraft.block.{Block, BlockState}
import hexacraft.world.coord.{BlockCoords, BlockRelChunk}
import hexacraft.world.gen.noise.NoiseInterpolator3D

class ChunkGenerator(chunk: Chunk) {
  def loadData(): ChunkData = {
    val world = chunk.world
    val coords = chunk.coords
    val file = new File(world.saveDir, "chunks/" + coords.value + ".dat")
    val storage: ChunkStorage = new DenseChunkStorage(chunk.coords)
    if (file.isFile) {
      val stream = new NBTInputStream(new FileInputStream(file))
      val nbt = stream.readTag().asInstanceOf[CompoundTag]
      stream.close()
      storage.fromNBT(nbt)
    } else {
      val column = world.getColumn(coords.getColumnRelWorld).get

      val noiseInterp = new NoiseInterpolator3D(4, 4, 4, (i, j, k) => {
        val c = BlockCoords(coords.X * 16 + i * 4, coords.Y * 16 + j * 4, coords.Z * 16 + k * 4, world.size).toCylCoord
        world.worldGenerator.blockGenerator.genNoiseFromCyl(c) + world.worldGenerator.blockDensityGenerator.genNoiseFromCyl(c) * 0.4
      })

      for (i <- 0 until 16; j <- 0 until 16; k <- 0 until 16) {
        val noise = noiseInterp(i, j, k)
        val yToGo = coords.Y * 16 + j - column.heightMap(i)(k)
        val limit = if (yToGo < -6) -0.4 else if (yToGo < 0) -0.4 - (6 + yToGo) * 0.025 else 4
        if (noise > limit) storage.setBlock(new BlockState(BlockRelChunk(i, j, k, world.size).withChunk(coords),
          if (yToGo < -5) Block.Stone else if (yToGo < -1) Block.Dirt else Block.Grass))
      }
    }
    val data = new ChunkData
    data.storage = storage
    data
  }
}

class ChunkData {
  var storage: ChunkStorage = _

  def optimizeStorage(): Unit = {
    if (storage.isDense) {
      if (storage.numBlocks < 48) {
        storage = storage.toSparse
      }
    } else {
      if (storage.numBlocks > 64) {
        storage = storage.toDense
      }
    }
  }
}