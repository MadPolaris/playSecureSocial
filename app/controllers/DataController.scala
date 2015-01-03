package controllers

import models.{Datas, DemoUser}
import securesocial.core.RuntimeEnvironment

/**
 * Created by Barry on 12/7/14.
 */
class DataController (override implicit val env: RuntimeEnvironment[DemoUser]) extends securesocial.core.SecureSocial[DemoUser]{


  def list = SecuredAction {implicit request =>

    Ok(views.html.data(Datas.listAll))
  }
}
