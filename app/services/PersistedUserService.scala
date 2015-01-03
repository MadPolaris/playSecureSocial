package services

import imadz.model.gen.tables.pojos.{User, Userprofile}
import imadz.model.gen.tables.records.UserprofileRecord
import models.MailTokens.toMailToken
import models.{MailTokens, Users}
import securesocial.core._
import securesocial.core.providers.MailToken
import securesocial.core.services.{SaveMode, UserService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by geek on 1/3/15.
 */
class PersistedUserService extends UserService[User] {
  implicit def string2Option(str: String): Option[String] = if (null == str) None else Some(str)

  implicit def int2Option(value: Integer): Option[Int] = if (null == value) None else Some(value)

  override def find(providerId: String, userId: String): Future[Option[BasicProfile]] = Future {
    Users.findUserProfile(providerId, userId).map(toBasicProfile)
  }

  override def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = Future {
    Users.findByEmailAndProvider(email, providerId).map(toBasicProfile)
  }

  override def deleteToken(uuid: String): Future[Option[MailToken]] = Future {
    MailTokens deleteToken (uuid) map toMailToken
  }

  override def link(current: User, to: BasicProfile): Future[User] = Future {
    Users.link(current, to)
  }

  override def passwordInfoFor(user: User): Future[Option[PasswordInfo]] = Future {
    Users.passwordInfoFor(user) map { profile => PasswordInfo(profile.getPassowrdHasher, profile.getPassowrdHashed, profile.getPassowrdSalt)}
  }

  override def save(profile: BasicProfile, mode: SaveMode): Future[User] = Future {
    Users.save(profile, mode)
  }

  override def findToken(token: String): Future[Option[MailToken]] = Future {
    MailTokens findToken token map toMailToken
  }

  override def deleteExpiredTokens(): Unit = {
    MailTokens.deleteExpiredTokens
  }

  override def updatePasswordInfo(user: User, info: PasswordInfo): Future[Option[BasicProfile]] = Future {
    Users.updatePasswordInfo(user, info) map toBasicProfile2
  }

  override def saveToken(token: MailToken): Future[MailToken] = Future {
    MailTokens.save(token)
  }

  def passwordOf(userprofile: Userprofile): Option[PasswordInfo] = {
    if (null == userprofile.getPassowrdHashed
      && null == userprofile.getPassowrdHasher) {
      None
    } else {
      Some(PasswordInfo(
        hasher = userprofile.getPassowrdHasher,
        password = userprofile.getPassowrdHashed,
        salt = if (null == userprofile.getPassowrdSalt) None else Some(userprofile.getPassowrdSalt)
      ))
    }
  }

  def toBasicProfile: Userprofile => BasicProfile = { profile =>
    BasicProfile(providerId = profile.getProviderId,
      userId = profile.getLogicalUserId,
      firstName = profile.getFirstName,
      lastName = profile.getLastName,
      fullName = profile.getFullName,
      email = profile.getEmail,
      authMethod = AuthenticationMethod(profile.getAuthMethod),
      avatarUrl = profile.getAvatarUrl,
      oAuth1Info = oAuth1InfoOf(profile),
      oAuth2Info = oAuth2InfoOf(profile),
      passwordInfo = passwordOf(profile)
    )
  }

  def toBasicProfile2: UserprofileRecord => BasicProfile = { profile =>
    BasicProfile(providerId = profile.getProviderId,
      userId = profile.getLogicalUserId,
      firstName = profile.getFirstName,
      lastName = profile.getLastName,
      fullName = profile.getFullName,
      email = profile.getEmail,
      authMethod = AuthenticationMethod(profile.getAuthMethod),
      avatarUrl = profile.getAvatarUrl,
      oAuth1Info = oAuth1InfoOf(profile),
      oAuth2Info = oAuth2InfoOf(profile)
    )
  }

  def oAuth1InfoOf(profile: Userprofile): Option[OAuth1Info] =
    if (null == profile.getOAuth1Token && null == profile.getOAuth1Secret) None
    else Some(OAuth1Info(profile.getOAuth1Token, profile.getOAuth1Secret))

  def oAuth2InfoOf(profile: Userprofile): Option[OAuth2Info] =
    if (null == profile.getOAuth2AccessToken) None
    else Some(OAuth2Info(profile.getOAuth2AccessToken, profile.getOAuth2TokenType, profile.getOAuth2ExpiresIn, profile.getOAuth2RefreshToken))

  def oAuth1InfoOf(profile: UserprofileRecord): Option[OAuth1Info] =
    if (null == profile.getOAuth1Token && null == profile.getOAuth1Secret) None
    else Some(OAuth1Info(profile.getOAuth1Token, profile.getOAuth1Secret))

  def oAuth2InfoOf(profile: UserprofileRecord): Option[OAuth2Info] =
    if (null == profile.getOAuth2AccessToken) None
    else Some(OAuth2Info(profile.getOAuth2AccessToken, profile.getOAuth2TokenType, profile.getOAuth2ExpiresIn, profile.getOAuth2RefreshToken))
}
