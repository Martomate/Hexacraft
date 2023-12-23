package hexacraft.world.entity

import hexacraft.renderer.TextureSingle
import hexacraft.world.{BlocksInWorld, CollisionDetector, CylinderSize, HexBox}

import com.martomate.nbt.Nbt
import hexacraft.world.coord.{BlockCoords, CylCoords}
import org.joml.Vector3f

class SheepEntity(
    model: EntityModel,
    initData: EntityBaseData,
    private val ai: EntityAI
)(using CylinderSize)
    extends Entity(initData, model) {
  override val boundingBox: HexBox = new HexBox(0.4f, 0, 0.75f)

  override def tick(world: BlocksInWorld, collisionDetector: CollisionDetector): Unit = {
    ai.tick(world, data, boundingBox)
    data.velocity.add(ai.acceleration())

    data.velocity.x *= 0.9
    data.velocity.z *= 0.9

    EntityPhysicsSystem(world, collisionDetector).update(data, boundingBox)
    model.tick()
  }

  override def toNBT: Nbt.MapTag =
    super.toNBT
      .withField("type", Nbt.StringTag("sheep"))
      .withField("ai", ai.toNBT)
}

class SheepFactory(makeModel: () => EntityModel) extends EntityFactory:
  override def atStartPos(pos: CylCoords)(using CylinderSize): SheepEntity =
    val model = makeModel()
    new SheepEntity(model, new EntityBaseData(position = pos), SimpleWalkAI.create)

  override def fromNBT(tag: Nbt.MapTag)(using CylinderSize): SheepEntity =
    val model = makeModel()
    val baseData = EntityBaseData.fromNBT(tag)
    val ai: EntityAI =
      tag.getMap("ai") match
        case Some(t) => SimpleWalkAI.fromNBT(t)
        case None    => SimpleWalkAI.create
    new SheepEntity(model, baseData, ai)

class SheepEntityModel(
    val head: BasicEntityPart,
    val body: BasicEntityPart,
    val frontRightLeg: BasicEntityPart,
    val frontLeftLeg: BasicEntityPart,
    val backRightLeg: BasicEntityPart,
    val backLeftLeg: BasicEntityPart,
    val textureName: String
) extends EntityModel:
  override val parts: Seq[EntityPart] = Seq(head, body, frontRightLeg, frontLeftLeg, backRightLeg, backLeftLeg)

  private val animation = new SheepAnimation(this)

  override def tick(): Unit = animation.tick()

  override def texture: TextureSingle =
    TextureSingle.getTexture(
      "textures/entities/" + textureName
    )

class SheepAnimation(model: SheepEntityModel):
  var time = 0f

  def tick(): Unit =
    time += 1f / 60

    val phase = time * 2 * math.Pi

    model.frontRightLeg.rotation.z = -0.5f * math.sin(phase).toFloat
    model.frontLeftLeg.rotation.z = 0.5f * math.sin(phase).toFloat

    model.backRightLeg.rotation.z = 0.5f * math.sin(phase).toFloat
    model.backLeftLeg.rotation.z = -0.5f * math.sin(phase).toFloat

object SheepEntityModel:
  private def makeHexBox(r: Int, b: Float, h: Int): HexBox =
    HexBox(r / 32f * 0.5f, b / 32f * 0.5f, (h + b) / 32f * 0.5f)

  private def makePartPosition(xp: Double, yp: Double, zp: Double): BlockCoords.Offset =
    BlockCoords.Offset(xp / 32.0, yp / 32.0, zp / 32.0)

  def create(textureName: String): SheepEntityModel =
    val legLength = 32
    val legRadius = 6
    val bodyLength = 48
    val bodyRadius = 16
    val headDepth = 16
    val headRadius = 12

    val headOffset = 2
    val headYOffset = 2
    val legOffset = bodyRadius * 0.25f / CylinderSize.y60
    val legYOffset = 3

    val px = 2.0 / 3
    val pz = 1.0 / 3

    val headBounds = makeHexBox(headRadius, -headDepth / 2f, headDepth)
    val bodyBounds = makeHexBox(bodyRadius, 0, bodyLength)
    val legBounds = makeHexBox(legRadius, 0, legLength)

    val headDistXZ = 0.5 * bodyLength + headOffset
    val bodyDist = 0.5 * bodyLength
    val legDist = 0.5 * bodyLength - legRadius

    val headY = legLength + legYOffset + (headRadius + headYOffset) * CylinderSize.y60
    val bodyY = legLength + legYOffset
    val legY = legLength

    val headPos = makePartPosition(headDistXZ * px, headY, -headDistXZ * pz).toCylCoordsOffset
    val bodyPos = makePartPosition(-bodyDist * px, bodyY, bodyDist * pz).toCylCoordsOffset
    val frontRightLegPos = makePartPosition(legDist * px, legY, legOffset - legDist * pz).toCylCoordsOffset
    val frontLeftLegPos = makePartPosition(legDist * px, legY, -legOffset - legDist * pz).toCylCoordsOffset
    val backRightLegPos = makePartPosition(-legDist * px, legY, legOffset + legDist * pz).toCylCoordsOffset
    val backLeftLegPos = makePartPosition(-legDist * px, legY, -legOffset + legDist * pz).toCylCoordsOffset

    val pi = math.Pi.toFloat

    new SheepEntityModel(
      head = BasicEntityPart(headBounds, headPos, Vector3f(0, pi / 2, pi / 2), (0, 168)),
      body = BasicEntityPart(bodyBounds, bodyPos, Vector3f(0, pi / 2, -pi / 2), (0, 88)),
      frontRightLeg = BasicEntityPart(legBounds, frontRightLegPos, Vector3f(pi, 0, 0), (36, 44)),
      frontLeftLeg = BasicEntityPart(legBounds, frontLeftLegPos, Vector3f(pi, 0, 0), (0, 44)),
      backRightLeg = BasicEntityPart(legBounds, backRightLegPos, Vector3f(pi, 0, 0), (36, 0)),
      backLeftLeg = BasicEntityPart(legBounds, backLeftLegPos, Vector3f(pi, 0, 0), (0, 0)),
      textureName
    )
