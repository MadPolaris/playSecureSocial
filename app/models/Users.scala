package models

import java.sql.Connection

import imadz.model.gen.Tables.USERPROFILE
import imadz.model.gen.tables.pojos.{User, Userprofile}
import imadz.model.gen.tables.records.{UserRecord, UserprofileRecord}
import org.jooq.DSLContext
import org.jooq.impl.DSL
import play.api.Play.current
import play.api.db.DB
import securesocial.core.services.SaveMode
import securesocial.core.{BasicProfile, PasswordInfo}

/**
 * Created by geek on 1/3/15.
 */
object Users {
  def updatePasswordInfo(user: User, info: PasswordInfo): Option[UserprofileRecord] = DB.withConnection { implicit conn =>
    passwordInfoFor(user) map { profile =>
      profile.setPassowrdHashed(info.password)
      profile.setPassowrdHasher(info.hasher)
      info.salt foreach profile.setPassowrdSalt
      dsl.executeUpdate(profile)
      profile
    }
  }


  def passwordInfoFor(user: User): Option[UserprofileRecord] = DB.withConnection { implicit conn =>
    val profile = dsl
      .selectFrom(USERPROFILE)
      .where(USERPROFILE.PROVIDER_ID.eq("userpass"))
      .fetchOne
    if (null == profile) None
    else Some(profile)
  }

  def link(user: User, profile: BasicProfile): User = DB.withConnection { implicit conn =>
    createUserProfile(profile, user.getId)
    user
  }


  def findByEmailAndProvider(email: String, providerId: String) = DB.withConnection { implicit conn =>
    val result = dsl
      .selectFrom(USERPROFILE)
      .where(USERPROFILE.PROVIDER_ID.eq(providerId))
      .and(USERPROFILE.EMAIL.eq(email))
      .fetchOneInto(classOf[Userprofile])
    if (null == result) {
      None
    } else {
      Some(result)
    }
  }

  def findUserProfile(providerId: String, userId: String): Option[Userprofile] = DB.withConnection { implicit conn =>
    val result = dsl
      .selectFrom(USERPROFILE)
      .where(USERPROFILE.PROVIDER_ID.eq(providerId))
      .and(USERPROFILE.LOGICAL_USER_ID.eq(userId))
      .fetchOneInto(classOf[Userprofile])
    if (null == result) {
      None
    } else {
      Some(result)
    }
  }

  private def dsl(implicit conn: Connection): DSLContext = {
    DSL.using(conn)
  }

  def save(profile: BasicProfile, mode: SaveMode): User = DB.withTransaction { implicit conn =>
    mode match {
      case SaveMode.SignUp => createUser(profile)
      case SaveMode.LoggedIn =>
      case SaveMode.PasswordChange => updatePassword(profile)
    }

    findUserProfile(profile.providerId, profile.userId) match {
      case Some(userProfile) => toUser(userProfile.getUserId)
      case None => createUser(profile)
    }
  }

  def updatePassword(profile: BasicProfile)(implicit conn: Connection): Unit = profile.passwordInfo foreach { passwordInfo =>
    dsl.update(USERPROFILE)
      .set(USERPROFILE.PASSOWRD_HASHER, passwordInfo.hasher)
      .set(USERPROFILE.PASSOWRD_HASHED, passwordInfo.password)
      .set(USERPROFILE.PASSOWRD_SALT, passwordInfo.salt.getOrElse(""))
      .where(USERPROFILE.PROVIDER_ID.eq(profile.providerId))
      .and(USERPROFILE.LOGICAL_USER_ID.eq(profile.userId))
      .execute()
  }


  def createUser(profile: BasicProfile): User = DB.withConnection { implicit conn =>
    val newUser = new UserRecord
    dsl.executeInsert(newUser)
    val userId = dsl.lastID

    createUserProfile(profile, userId.intValue)
    toUser(userId.intValue)
  }

  def toUser(userId: Integer): User = {
    val result = new User
    result.setId(userId)
    result
  }

  def createUserProfile(profile: BasicProfile, userId: Integer) = DB.withConnection { implicit conn =>
    val newUserprofile: UserprofileRecord = populateUserProfileRecord(profile, userId)
    dsl.executeInsert(newUserprofile)
  }

  def populateUserProfileRecord(profile: BasicProfile, userId: Integer): UserprofileRecord = {
    val newUserprofile: UserprofileRecord = populate(profile)
    newUserprofile.setUserId(userId)
    newUserprofile
  }

  def populate(profile: BasicProfile): UserprofileRecord = {
    val newUserprofile = new UserprofileRecord
    newUserprofile.setLogicalUserId(profile.userId)
    newUserprofile.setAuthMethod(profile.authMethod.method)
    profile.email foreach newUserprofile.setEmail
    profile.firstName foreach newUserprofile.setFirstName
    profile.lastName foreach newUserprofile.setLastName
    profile.avatarUrl foreach newUserprofile.setAvatarUrl
    profile.fullName foreach newUserprofile.setFullName
    profile.oAuth1Info foreach { info =>
      newUserprofile.setOAuth1Secret(info.secret)
      newUserprofile.setOAuth1Token(info.token)
    }
    profile.oAuth2Info foreach { info =>
      newUserprofile.setOAuth2AccessToken(info.accessToken)
      info.expiresIn foreach (newUserprofile.setOAuth2ExpiresIn(_))
      info.refreshToken foreach newUserprofile.setOAuth2RefreshToken
      info.tokenType foreach newUserprofile.setOAuth2TokenType
    }
    profile.passwordInfo foreach { info =>
      newUserprofile.setPassowrdHashed(info.password)
      newUserprofile.setPassowrdHasher(info.hasher)
      info.salt foreach newUserprofile.setPassowrdSalt
    }
    newUserprofile.setProviderId(profile.providerId)
    newUserprofile
  }
}
