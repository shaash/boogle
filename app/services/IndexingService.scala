package services

import java.io.File
import java.nio.file.Path
import javax.inject.{Inject, Singleton}

import akka.actor.{Actor, Props}
import models.BookPage
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import play.api.Logger
import services.IndexingService.IndexBook
import tables.{Books, Jobs}

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/**
  * Indexing service
  */
@Singleton
class IndexingService @Inject()(jobs: Jobs, books: Books, implicit val ex: ExecutionContext) extends Actor {

  // Handle messages for indexing service
  override def receive: Receive = {
    case indexBook: IndexBook =>
      Logger.info(s"Indexing book ${indexBook.title} for jobId ${indexBook.jobId}")
      // Read the file from the given path
      val file = new File(s"/tmp/${indexBook.jobId}")

      val pdf = PDDocument.load(file)
      val pageCount = pdf.getPages.getCount
      val stripper = new PDFTextStripper

      // Make sure to delete it if we already have the book indexed. This would happen if reIndex is set to true
      books.deleteBy(indexBook.title).map { _ =>
        indexPage(pdf, stripper, 1, indexBook.title, pageCount)
        pdf.close()

        // Delete the temporary file after indexing
        file.delete()


        // Update the status of job once done
        jobs.updateJob(indexBook.jobId, "Indexed")
      }

  }

  @tailrec
  private def indexPage(pdf: PDDocument, stripper: PDFTextStripper, pageNumber: Int, title: String, pageCount: Int): Unit = {
    Try {
      stripper.setStartPage(pageNumber)
      stripper.setEndPage(pageNumber)
      val text = stripper.getText(pdf)
      books.insertBookPage(BookPage(title, text, pageNumber))
      Logger.debug(s"Inserted document $title => $pageNumber")
    } match {
      case Success(_) =>
        if (pageCount > pageNumber)
          indexPage(pdf, stripper, pageNumber + 1, title, pageCount)
      case Failure(e) =>
        Logger.warn(s"indexing of page failed for $title at pageNumber $pageNumber with error ${e.getMessage}")
    }
  }
}

object IndexingService {
  def props = Props[IndexingService]

  case class IndexBook(title: String, path: Path, jobId: String)

}


