package controllers

import java.nio.file.Paths
import java.util.UUID
import javax.inject.{Singleton, _}

import akka.actor.ActorRef
import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import services.IndexingService
import tables.{Books, Jobs}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

/**
  * This controller handles APIs for book resource
  */
@Singleton
class BookController @Inject()(@Named("indexing-actor") indexingServiceActor: ActorRef, cc: ControllerComponents,
                               implicit val ex: ExecutionContext, jobs: Jobs, books: Books) extends AbstractController(cc) {

  /**
    * Initiates file indexing for the given file
    *
    * Force reindexing by setting reIndex to true which by default is set to false.
    * Use Reindexing when the content of file has changed since last update.
    */
  def indexBook(reIndex: Boolean) = Action(parse.multipartFormData) { implicit request =>
    val filesCount = request.body.files.size
    val uuid = UUID.randomUUID().toString
    val warnings = request.body.files.flatMap { file =>
      Logger.info(s"Received file ${file.filename} of content type ${file.contentType}")
      file.contentType match {
        case Some("application/pdf") if (reIndex || !fileAlreadyIndexed(file.filename)) =>
          val filePath = Paths.get(s"/tmp/${uuid}")
          file.ref.moveTo(filePath, false)

          jobs.insertJob(uuid)
          indexingServiceActor ! IndexingService.IndexBook(file.filename, filePath, uuid)
          None

        case other =>
          Some(s"file ${file.filename} of content type ${other} not supported or file already indexed")
      }
    }

    filesCount == warnings.size match {
      case true =>
        BadRequest(s"""{"errors":["No files to index"], "warnings": ${Json.toJson(warnings)}}""")
      case false =>
        Ok(s"""{"jobId":"$uuid","status": "Indexing", "warnings": ${Json.toJson(warnings)} }""")
    }

  }

  def fileAlreadyIndexed(fileName: String): Boolean = {
    // TODO : Remove Await, used it for simplicity
    val res = Await.result(books.byTitle(fileName), 30 second)
    res.isDefined
  }

  /** Search books by quote  */
  def searchBooks(quote: String) = Action.async { implicit request =>
    books.searchBookPages(quote).map(v => Ok(Json.toJson(v)))
  }

  /** Get job status by id */
  def jobStatus(jobId: String) = Action.async { implicit request =>
    jobs.getJob(jobId).map(_ match {
      case Some(job) => Ok(job)
      case None => NotFound(s"job $jobId not found")
    })
  }
}
