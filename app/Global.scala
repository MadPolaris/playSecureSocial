/**
 * Created by Scala on 14-8-31.
 */

import models.DemoUser
import play.api._
import play.api.data.Form
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import play.twirl.api.{Html, Txt}
import securesocial.controllers.{RegistrationInfo, ViewTemplates, MailTemplates}
import securesocial.core.providers._
import securesocial.core.{BasicProfile, RuntimeEnvironment}
import services.DemoUserService
import scala.collection.immutable.ListMap
import java.lang.reflect.Constructor

object Global extends GlobalSettings {

  /**
   * Demo application's custom Runtime Environment
   */
  object DemoRuntimeEnvironment extends RuntimeEnvironment.Default[DemoUser] {
    override lazy val userService: DemoUserService = new DemoUserService

    override lazy val mailTemplates: MailTemplates = new MailTemplates.Default(this) {
      override def getSignUpEmail(token: String)(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html]) =
        (None, Some(views.html.mail.signUpEmail(token)))

      override def getWelcomeEmail(user: BasicProfile)(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html]) = {
        (None, Some(views.html.mail.welcomeEmail(user)))
      }
    }

    override lazy val viewTemplates: ViewTemplates = new ViewTemplates.Default(this) {
      override def getSignUpPage(form: Form[RegistrationInfo], token: String)(implicit request: RequestHeader, lang: Lang): Html = {
        views.html.registration.signUp(form, token)
      }
      override def getStartSignUpPage(form: Form[String])(implicit request: RequestHeader, lang: Lang): Html = {
        views.html.registration.startSignUp(form)
      }
      override def getLoginPage(form: Form[(String, String)],
                                msg: Option[String] = None)(implicit request: RequestHeader, lang: Lang): Html = {
        views.html.login(form, msg)
      }
    }
    override lazy val providers = ListMap(
      include(new FacebookProvider(routes, cacheService, oauth2ClientFor(FacebookProvider.Facebook))),
      include(new GitHubProvider(routes, cacheService, oauth2ClientFor(GitHubProvider.GitHub))),
      include(new GoogleProvider(routes, cacheService, oauth2ClientFor(GoogleProvider.Google))),
      include(new LinkedInProvider(routes, cacheService, oauth1ClientFor(LinkedInProvider.LinkedIn))),
      include(new TwitterProvider(routes, cacheService, oauth1ClientFor(TwitterProvider.Twitter))),
      include(new UsernamePasswordProvider[DemoUser](userService, None, viewTemplates, passwordHashers))
    )
  }

  /**
   * Dependency injection on Controllers using Cake Pattern
   *
   * @param controllerClass
   * @tparam A
   * @return
   */
  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    val instance = controllerClass.getConstructors.find { c =>
      val params = c.getParameterTypes
      params.length == 1 && params(0) == classOf[RuntimeEnvironment[DemoUser]]
    }.map {
      _.asInstanceOf[Constructor[A]].newInstance(DemoRuntimeEnvironment)
    }
    instance.getOrElse(super.getControllerInstance(controllerClass))
  }

}