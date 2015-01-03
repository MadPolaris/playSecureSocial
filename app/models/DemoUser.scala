package models

import securesocial.core.BasicProfile

/**
 * Created by Scala on 14-12-23.
 */

case class DemoUser(main: BasicProfile, identities: List[BasicProfile])  {
  override def equals(that: Any): Boolean = false
}

object DemoUser {

}