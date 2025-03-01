package hexacraft.world

import hexacraft.world.coord.CylCoords

import org.joml.Vector3d

case class Pose(pos: CylCoords, forward: Vector3d = new Vector3d())
