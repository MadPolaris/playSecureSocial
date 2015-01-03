package controllers

import play.api._
import play.api.mvc._
import models.DemoUser
import play.api.Play.current
import securesocial.core.{Authorization, RuntimeEnvironment}

class Application (override implicit val env: RuntimeEnvironment[DemoUser]) extends securesocial.core.SecureSocial[DemoUser]{

  def index = SecuredAction {
    Ok(views.html.index("Your new application is ready."))
  }

  // a sample action using an authorization implementation
  def onlyTwitter = SecuredAction(WithProvider("twitter")) { implicit request =>
    Ok("You can see this because you logged in using Twitter")
  }
}

// An Authorization implementation that only authorizes uses that logged in using twitter
case class WithProvider(provider: String) extends Authorization[DemoUser] {
  def isAuthorized(user: DemoUser, request: RequestHeader) = {
    user.main.providerId == provider
  }
}