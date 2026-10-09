package hexacraft.server.entity

import hexacraft.nbt.Nbt
import hexacraft.world.entity.EntityModel

import java.security.MessageDigest
import java.util.HexFormat
import java.util.concurrent.ConcurrentHashMap

/** The entity models that the server can send to clients.
  *
  * Each model has an ID, which is a hash of the encoded model. Entities refer to their model by this ID, and the clients
  * fetch the models they don't already have. Since the same ID always means the same model, the clients can cache the
  * models, and it's safe to register more models while the server is running.
  */
object EntityModels {
  private val encodedModelsById = new ConcurrentHashMap[String, Nbt.MapTag]()

  private val modelsByType: Map[String, EntityModel] = Map(
    "player" -> PlayerEntityModel.model,
    "sheep" -> SheepEntityModel.model,
    "boat" -> BoatEntityModel.model
  )

  private val modelIdsByType: Map[String, String] =
    modelsByType.map((entityType, model) => entityType -> register(model))

  def forType(entityType: String): Option[EntityModel] = modelsByType.get(entityType)

  def idForType(entityType: String): Option[String] = modelIdsByType.get(entityType)

  /** Makes the model available to clients, and returns its ID */
  def register(model: EntityModel): String = {
    val encoded = Nbt.encode(model)
    val id = modelId(encoded)
    encodedModelsById.putIfAbsent(id, encoded)
    id
  }

  /** The encoded model with the given ID, if it has been registered */
  def encodedModel(id: String): Option[Nbt.MapTag] = Option(encodedModelsById.get(id))

  /** The first 128 bits of the SHA-256 hash of the encoded model, in hexadecimal */
  private def modelId(encodedModel: Nbt.MapTag): String = {
    val hash = MessageDigest.getInstance("SHA-256").digest(encodedModel.toBinary())
    HexFormat.of().formatHex(hash, 0, 16)
  }
}
