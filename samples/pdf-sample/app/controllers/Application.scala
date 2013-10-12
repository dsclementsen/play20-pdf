package controllers


import play.api.mvc._
import util.pdf.PDF


/**
 * Mains pages
 */
object Application extends Controller {

  def index = Action {
    Ok(views.html.index.render("Your new application is ready."))
  }

  def document() = Action.async {
    PDF.ok(views.html.document.render("Your new application is ready."))
  }
}
