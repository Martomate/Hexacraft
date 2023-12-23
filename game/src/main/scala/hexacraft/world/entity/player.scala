package hexacraft.world.entity

import hexacraft.renderer.TextureSingle
import hexacraft.world.{BlocksInWorld, CollisionDetector, CylinderSize, HexBox}

import com.martomate.nbt.Nbt
import hexacraft.world.coord.{BlockCoords, CylCoords}
import org.joml.Vector3f

class PlayerEntity(
    model: EntityModel,
    initData: EntityBaseData,
    private val ai: EntityAI
)(using CylinderSize)
    extends Entity(initData, model) {
  override val boundingBox: HexBox = new HexBox(0.2f, 0, 1.75f)

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
      .withField("type", Nbt.StringTag("player"))
      .withField("ai", ai.toNBT)
}

class ControlledPlayerEntity(model: EntityModel, initData: EntityBaseData) extends Entity(initData, model) {
  def setPosition(pos: CylCoords): Unit = this.data.position = pos
}

class PlayerFactory(makeModel: () => EntityModel) extends EntityFactory:
  override def atStartPos(pos: CylCoords)(using CylinderSize): PlayerEntity =
    val model = makeModel()
    new PlayerEntity(model, new EntityBaseData(position = pos), SimpleWalkAI.create)

  override def fromNBT(tag: Nbt.MapTag)(using CylinderSize): PlayerEntity =
    val model = makeModel()
    val baseData = EntityBaseData.fromNBT(tag)
    val ai: EntityAI =
      tag.getMap("ai") match
        case Some(t) => SimpleWalkAI.fromNBT(t)
        case None    => SimpleWalkAI.create

    new PlayerEntity(model, baseData, ai)

class PlayerEntityModel(
    val head: BasicEntityPart,
    val leftBodyHalf: BasicEntityPart,
    val rightBodyHalf: BasicEntityPart,
    val rightArm: BasicEntityPart,
    val leftArm: BasicEntityPart,
    val rightLeg: BasicEntityPart,
    val leftLeg: BasicEntityPart,
    val textureName: String
) extends EntityModel:
  override val parts: Seq[EntityPart] = Seq(head, leftBodyHalf, rightBodyHalf, rightArm, leftArm, rightLeg, leftLeg)

  private val animation = new PlayerAnimation(this)

  override def tick(): Unit = animation.tick()

  override def texture: TextureSingle =
    TextureSingle.getTexture(
      "textures/entities/" + textureName
    )

class PlayerAnimation(model: PlayerEntityModel):
  private var time = 0f

  def tick(): Unit =
    time += 1f / 60

    val phase = time * 2 * math.Pi

    model.rightArm.rotation.z = -0.5f * math.sin(phase).toFloat
    model.leftArm.rotation.z = 0.5f * math.sin(phase).toFloat

    model.rightLeg.rotation.z = 0.5f * math.sin(phase).toFloat
    model.leftLeg.rotation.z = -0.5f * math.sin(phase).toFloat

object PlayerEntityModel:
  private def makeHexBox(r: Int, b: Float, h: Int): HexBox =
    HexBox(r / 32f * 0.5f, b / 32f * 0.5f, (h + b) / 32f * 0.5f)

  private def makePartPosition(xp: Double, yp: Double, zp: Double): BlockCoords.Offset =
    BlockCoords.Offset(xp / 32.0, yp / 32.0, zp / 32.0)

  def create(textureName: String): PlayerEntityModel =
    val legLength = 48
    val legRadius = 8
    val bodyLength = 40
    val bodyRadius = 8
    val armLength = 40
    val armRadius = 8
    val headDepth = 20
    val headRadius = 16

    val headBounds = makeHexBox(headRadius, -headDepth / 2f, headDepth)
    val bodyBounds = makeHexBox(bodyRadius, 0, bodyLength)
    val armBounds = makeHexBox(armRadius, -armRadius * CylinderSize.y60.toFloat, armLength)
    val legBounds = makeHexBox(legRadius, 0, legLength)

    val headY = bodyLength + legLength + headRadius * CylinderSize.y60
    val headPos = makePartPosition(0, headY, 0).toCylCoordsOffset

    val rightBodyPos = makePartPosition(0, legLength, 0.5 * bodyRadius).toCylCoordsOffset
    val leftBodyPos = makePartPosition(0, legLength, -0.5 * bodyRadius).toCylCoordsOffset

    val armY = legLength + bodyLength - armRadius * CylinderSize.y60
    val rightArmPos = makePartPosition(0, armY, bodyRadius + 0.5 * armRadius).toCylCoordsOffset
    val leftArmPos = makePartPosition(0, armY, -bodyRadius - 0.5 * armRadius).toCylCoordsOffset

    val rightLegPos = makePartPosition(0, legLength, 0.5 * legRadius).offset(0, 0, 0.001f).toCylCoordsOffset
    val leftLegPos = makePartPosition(0, legLength, -0.5 * legRadius).offset(0, 0, -0.001).toCylCoordsOffset

    val pi = math.Pi.toFloat

    PlayerEntityModel(
      head = BasicEntityPart(headBounds, headPos, Vector3f(0, pi / 2, pi / 2), (0, 176)),
      leftBodyHalf = BasicEntityPart(bodyBounds, leftBodyPos, Vector3f(0, 0, 0), (0, 120)),
      rightBodyHalf = BasicEntityPart(bodyBounds, rightBodyPos, Vector3f(0, 0, 0), (48, 120)),
      rightArm = BasicEntityPart(armBounds, rightArmPos, Vector3f(pi, 0, 0), (48, 64)),
      leftArm = BasicEntityPart(armBounds, leftArmPos, Vector3f(pi, 0, 0), (0, 64)),
      rightLeg = BasicEntityPart(legBounds, rightLegPos, Vector3f(pi, 0, 0), (48, 0)),
      leftLeg = BasicEntityPart(legBounds, leftLegPos, Vector3f(pi, 0, 0), (0, 0)),
      textureName
    )
