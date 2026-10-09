package hexacraft.world.entity

import hexacraft.nbt.{Nbt, NbtDecoder, NbtEncoder}

import org.joml.{Vector3d, Vector3f, Vector3fc}

/** An entity model is a collection of hexagonal prisms (parts).
  * It is immutable, so it can be shared by all entities of the same type.
  * Each part is placed relative to its parent part (if any).
  * Parents have to come before their children in `parts`.
  * A part with an empty box is not rendered, it only serves as a pivot for its children.
  */
case class EntityModel(parts: IndexedSeq[EntityPart]) {
  private val indicesByName: Map[String, Int] = parts.map(_.name).zipWithIndex.toMap
  require(indicesByName.size == parts.size, "Part names must be unique")

  /** The index of each part's parent, or -1 if the part has no parent */
  val parentIndices: IndexedSeq[Int] = parts.zipWithIndex.map { (part, idx) =>
    part.parent match {
      case Some(parent) =>
        val parentIdx = indicesByName.getOrElse(parent, -1)
        require(parentIdx != -1, s"The parent of '${part.name}' ('$parent') is not in the model")
        require(parentIdx < idx, s"The parent of '${part.name}' has to come before it")
        parentIdx
      case None => -1
    }
  }

  def indexOf(name: String): Int = {
    indicesByName.getOrElse(name, throw new IllegalArgumentException(s"The model has no part named '$name'"))
  }

  def part(name: String): EntityPart = parts(indexOf(name))

  def hasPart(name: String): Boolean = indicesByName.contains(name)
}

object EntityModel {

  /** The size of a model pixel in world units (1/32 of a block) */
  val pixelSize: Double = 1.0 / 64

  given NbtEncoder[EntityModel] with {
    override def encode(model: EntityModel): Nbt.MapTag = {
      Nbt.makeMap("parts" -> Nbt.ListTag(model.parts.map(Nbt.encode(_))))
    }
  }

  /** Fails (returns None) if any part is invalid or if the parts don't form a valid model */
  given NbtDecoder[EntityModel] with {
    override def decode(tag: Nbt.MapTag): Option[EntityModel] = {
      for {
        partTags <- tag.getList("parts")
        parts = partTags.map(_.asMap.flatMap(Nbt.decode[EntityPart]))
        if parts.forall(_.isDefined)
        model <- {
          try Some(EntityModel(parts.flatten.toIndexedSeq))
          catch case _: IllegalArgumentException => None
        }
      } yield model
    }
  }
}

/** A hexagonal prism, with sizes in model pixels. `radius` is the big radius of the hexagon. */
case class HexPrism(radius: Int, length: Int) {
  def isEmpty: Boolean = radius == 0 || length == 0
}

object HexPrism {
  val empty: HexPrism = HexPrism(0, 0)
}

/** A part defines a coordinate system: it is placed at `position` in its parent's coordinate system (or the entity's,
  * if it has no parent) and rotated by `rotation` (in the order z, x, y) around that point. Its prism and its children
  * are placed independently in this coordinate system: the prism starts `prismOffset` along the y-axis, and the
  * children use their own `position` and `rotation`. All lengths are in model pixels.
  */
case class EntityPart(
    name: String,
    prism: HexPrism,
    position: Vector3fc,
    rotation: Vector3fc,
    parent: Option[String] = None,
    prismOffset: Float = 0
) {

  /** Pivot parts (with an empty prism) are not rendered */
  def isVisible: Boolean = !prism.isEmpty
}

object EntityPart {
  given NbtEncoder[EntityPart] with {
    override def encode(part: EntityPart): Nbt.MapTag = {
      Nbt
        .makeMap(
          "name" -> Nbt.StringTag(part.name),
          "prism" -> Nbt.makeMap(
            "radius" -> Nbt.IntTag(part.prism.radius),
            "length" -> Nbt.IntTag(part.prism.length)
          ),
          "prism_offset" -> Nbt.FloatTag(part.prismOffset),
          "position" -> Nbt.makeVectorTag(Vector3d(part.position)),
          "rotation" -> Nbt.makeVectorTag(Vector3d(part.rotation))
        )
        .withOptionalField("parent", part.parent.map(Nbt.StringTag(_)))
    }
  }

  /** The name and the prism are required. The position, rotation and prism offset are zero if they are missing. */
  given NbtDecoder[EntityPart] with {
    override def decode(tag: Nbt.MapTag): Option[EntityPart] = {
      for {
        name <- tag.getString("name")
        prismTag <- tag.getMap("prism")
        radius <- getInt(prismTag, "radius")
        length <- getInt(prismTag, "length")
      } yield EntityPart(
        name,
        HexPrism(radius, length),
        Vector3f(tag.getMap("position").map(_.setVector(Vector3d())).getOrElse(Vector3d())),
        Vector3f(tag.getMap("rotation").map(_.setVector(Vector3d())).getOrElse(Vector3d())),
        tag.getString("parent"),
        tag.getFloat("prism_offset", 0)
      )
    }
  }

  private def getInt(tag: Nbt.MapTag, key: String): Option[Int] = {
    tag.getTag(key).collect { case Nbt.IntTag(v) => v }
  }
}
