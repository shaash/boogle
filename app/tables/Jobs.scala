package tables

import javax.inject.Inject

import play.api.Configuration
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.play.json.collection.JSONCollection
import javax.inject.Singleton

import play.modules.reactivemongo.json.ImplicitBSONHandlers._

import scala.concurrent.ExecutionContext
/**
  * Books persistence
  */
@Singleton
class Jobs @Inject()(val configuration: Configuration, val reactiveMongoApi: ReactiveMongoApi,
                     implicit val ec: ExecutionContext) {
  val collectionFuture = reactiveMongoApi.database.map(_.collection[JSONCollection]("jobs"))

  // ensure the index is present
  collectionFuture.map(_.indexesManager.ensure(Index(Seq(("jobId", IndexType.Ascending)))))

  /** insert job */
  def insertJob(jobId: String) = {
    collectionFuture.flatMap { collection =>
      collection.insert(Json.obj("jobId" -> jobId, "status" -> "Indexing"))
    }
  }

  /** update job */
  def updateJob(jobId: String, status: String) = {
    collectionFuture.flatMap { collection =>
      collection.update(Json.obj("jobId" -> jobId), Json.obj("$set" -> Json.obj("status" -> status)))
    }
  }

  /** get job */
  def getJob(jobId: String) = {
    collectionFuture.flatMap { collection =>
      collection.find(Json.obj("jobId" -> jobId), Json.obj("_id" -> 0)).one[JsObject]
    }
  }

}
