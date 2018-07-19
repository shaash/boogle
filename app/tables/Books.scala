package tables

import javax.inject.{Inject, Singleton}

import models.BookPage
import play.api.Configuration
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext


/**
  * Books persistence
  */
@Singleton
class Books @Inject()(val configuration: Configuration, val reactiveMongoApi: ReactiveMongoApi,
                      implicit val ec: ExecutionContext) {

  import BookPage._

  val collectionFuture = reactiveMongoApi.database.map(_.collection[JSONCollection]("books"))

  // ensure the index is present
  collectionFuture.map(_.indexesManager.ensure(Index(Seq(("pageContent", IndexType.Text)))))

  /** insert bookpage */
  def insertBookPage(bookPage: BookPage) = {
    collectionFuture.flatMap { collection =>
      collection.insert(bookPage)
    }
  }

  /** delete by book title, this is used while reindexing */
  def deleteBy(title: String) = {
    collectionFuture.flatMap{ collection =>
      collection.remove(Json.obj("title" -> title))
    }
  }

  /** Find one by title */
  def byTitle(title: String, projection: JsObject = Json.obj("title" -> 1)) = {
    collectionFuture.flatMap { collection =>
      collection.find(Json.obj("title" -> title), projection).one[JsObject]
    }
  }

  /** search book pages */
  def searchBookPages(quote: String) = {
    collectionFuture.flatMap { collection =>
      collection.find(Json.obj("$text" -> Json.obj("$search" -> s"""\"$quote\"""")),
        Json.obj("_id" -> 0)).cursor[JsObject]().collect[List]()
    }
  }

}
