package models

import play.api.libs.json.Json

/**
  * BookPage model
  */
case class BookPage(title: String, // title same as filename as of now is uniquely identifies the file
                    pageContent: String, // Content of the pdf page
                    pageNumber: Int) // This is the pageNumber for identification, this could mismatch with the pageNumber present in the pdf content itself

object BookPage {
  implicit val bookPageFormatter = Json.format[BookPage]
}

