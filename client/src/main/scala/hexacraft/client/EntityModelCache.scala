package hexacraft.client

import hexacraft.nbt.Nbt
import hexacraft.world.entity.EntityModel

import scala.collection.mutable

/** Keeps track of the entity models received from the server, and of the ones that need to be fetched.
  *
  * Models are identified by IDs chosen by the server, where the same ID always means the same model, so a model only
  * has to be fetched once.
  */
class EntityModelCache {
  import EntityModelCache.Lookup

  private val models = mutable.HashMap.empty[String, EntityModel]
  private val unavailableIds = mutable.HashSet.empty[String]
  private val idsToRequest = mutable.LinkedHashSet.empty[String]
  private val requestedIds = mutable.HashSet.empty[String]

  /** Looks up the model, and makes sure it will be requested if it's not available yet */
  def lookup(id: String): Lookup = {
    models.get(id) match {
      case Some(model) =>
        Lookup.Available(model)
      case None if unavailableIds.contains(id) =>
        Lookup.Unavailable
      case None =>
        if !requestedIds.contains(id) then {
          idsToRequest += id
        }
        Lookup.Pending
    }
  }

  /** Returns the IDs that should be requested from the server. Each ID is only returned once. */
  def takeIdsToRequest(): Seq[String] = {
    val ids = idsToRequest.toSeq
    idsToRequest.clear()
    requestedIds ++= ids
    ids
  }

  /** Stores the models from the server's response to a request for the given IDs.
    *
    * IDs that are missing from the response (or that have an invalid model) are marked as unavailable, so they are not
    * requested again.
    */
  def receive(requested: Seq[String], response: Map[String, Nbt.MapTag]): Unit = {
    for id <- requested do {
      requestedIds -= id

      response.get(id) match {
        case Some(tag) =>
          Nbt.decode[EntityModel](tag) match {
            case Some(model) =>
              models(id) = model
            case None =>
              println(s"Received an invalid entity model (id: $id)")
              unavailableIds += id
          }
        case None =>
          println(s"The server did not have the requested entity model (id: $id)")
          unavailableIds += id
      }
    }
  }
}

object EntityModelCache {
  enum Lookup {
    case Available(model: EntityModel)

    /** The model has been (or will be) requested, but it has not been received yet */
    case Pending

    /** The server could not provide the model */
    case Unavailable
  }
}
