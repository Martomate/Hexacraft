package com.martomate.hexacraft.world.render

import com.flowpowered.nbt.{CompoundTag, LongTag}
import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}
import com.martomate.hexacraft.world.World
import com.martomate.hexacraft.world.camera.{Camera, CameraProjection}
import com.martomate.hexacraft.world.coord.integer._
import com.martomate.hexacraft.world.settings.{WorldGenSettings, WorldInfo, WorldProvider, WorldSettings}
import org.scalameter.api._

object ChunkRenderImplBench extends Bench.LocalTime {
  implicit val cylSize: CylinderSize = new CylinderSize(8)

  // Initializations ------
  val heightAboveGround: Gen[Int] = Gen.range("Height above ground")(-16, 16, 8)

  /*
  val renderers: Gen[ChunkRendererImpl] = for {
    blocksPerChunk <- blockCounts
    world = new TestWorld(ChunkRelWorld(3, 4, 5).extendedNeighbors(1),
                          (c, w) => new TestChunk(c, w).fillWithBlocks(blocksPerChunk))
    chunk = world.getChunk(ChunkRelWorld(3, 4, 5)).get
  } yield new ChunkRendererImpl(chunk, world)
  */

  var renderers: Gen[ChunkRendererImpl] = setupRenderers

  // Measurements ------
  performance of "ChunkRenderer" in {
    performance of "updateContent" in {
      measure.method("blocks").config(
        exec.benchRuns := 50,
        exec.minWarmupRuns := 10,
        reports.regression.significance := 5e-2
      ) in {
        using(renderers) setUp { renderer =>
          renderer.updateContent() // to initialize lighting
        } in { renderer =>
          renderer.updateContent()
        }
      }

      measure.method("lighting").config(
        exec.benchRuns := 50,
        exec.minWarmupRuns := 10,
        reports.regression.significance := 5e-2
      ) in {
        var renderer: ChunkRendererImpl = null

        using(heightAboveGround) setUp { aboveGround =>
          renderer = makeRenderer(aboveGround)
        } in { _ =>
          renderer.updateContent()
        }
      }
    }
  }

  // Helper stuff ------
  def setupRenderers: Gen[ChunkRendererImpl] = for {
    aboveGround <- heightAboveGround
  } yield makeRenderer(aboveGround)

  private def makeRenderer(aboveGround: Int): ChunkRendererImpl = {
    val world = initWorld(aboveGround)
    val chunk = world.getChunk(ChunkRelWorld(0, (world.provideColumn(ColumnRelWorld(0, 0)).heightMap(0, 0) + aboveGround * 2) >> 4, 0)).get
    new ChunkRendererImpl(chunk, world)
  }

  def initWorld(heightAboveGround: Int): World = {
    val w = new World(new TestWorldSettingsProvider(12398734587123L))
    val cam = new Camera(new CameraProjection(1,1,1,1))
    cam.position.y = w.provideColumn(ColumnRelWorld(0, 0)).heightMap(0, 0) + heightAboveGround
    cam.updateViewMatrix()
    for (_ <- 1 to 20) {
      w.tick(cam)
      Thread.sleep(10)
    }
    w
  }

  private class TestWorldSettingsProvider(seed: Long) extends WorldProvider {
    override def getWorldInfo: WorldInfo = new WorldInfo(
      "bench world",
      cylSize,
      WorldGenSettings.fromNBT(
        NBTUtil.makeCompoundTag("", Seq(new LongTag("seed", seed))),
        WorldSettings(Some("bench world"), Some(8), Some(seed))
      ),
      NBTUtil.makeCompoundTag("", Seq.empty),
      NBTUtil.makeCompoundTag("", Seq.empty))

    override def loadState(path: String): CompoundTag = NBTUtil.makeCompoundTag("", Seq.empty)

    override def saveState(tag: CompoundTag, path: String): Unit = ()
  }
}
