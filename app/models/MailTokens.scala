package models

import java.sql.Timestamp
import java.util

import imadz.model.gen.Tables.EMAIL_TOKEN
import imadz.model.gen.tables.pojos.EmailToken
import imadz.model.gen.tables.records.EmailTokenRecord
import org.joda.time.DateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import play.api.Play.current
import play.api.db.DB
import securesocial.core.providers.MailToken
/**
 * Created by geek on 1/3/15.
 */
object MailTokens {



  implicit class TimestampOps(ts: Timestamp) {
    def asDateTime = new DateTime().withMillis(ts.getTime);
  }

  implicit class DateTimeOps(dt: DateTime) {
    def asTimestamp = new Timestamp(dt.getMillis)
  }

  implicit class UUIDOpts(uuid: String) {
    def asBinary: Array[Byte] = hex2bytes(uuid)

    def as: String = {
      val hexChars: String = uuid.replaceAll("-", "")
      val zeroArray = new Array[Char](256 - hexChars.length)
      util.Arrays.fill(zeroArray, '0')
      hexChars + new String(zeroArray)
    }

    def asUUID: String =
      uuid.substring(0, 8) :: uuid.substring(8, 12) :: uuid.substring(12, 16) :: uuid.substring(16, 20) :: uuid.substring(20) :: Nil mkString "-"
  }

  implicit class BinaryUUIDOpts(uuid: Array[Byte]) {
    def asHexString: String = bytes2hex(uuid).asUUID
  }

  implicit class MailTokenOpts(token: MailToken) {
    def asEmailToken: EmailTokenRecord = {
      val result = new EmailTokenRecord
      result.setEmail(token.email)
      result.setExpirationTime(token.expirationTime.asTimestamp)
      result.setCreationTime(token.creationTime.asTimestamp)
      result.setSignUp(token.isSignUp)
      result.setUuid(token.uuid.asBinary)
      result
    }
  }

  def toMailToken: EmailToken => MailToken = emailToken =>
    MailToken(
      emailToken.getUuid.asHexString,
      emailToken.getEmail,
      emailToken.getCreationTime.asDateTime,
      emailToken.getExpirationTime.asDateTime,
      emailToken.getSignUp)

  private def hex2bytes(hex: String): Array[Byte] = {
    hex.replaceAll("[^0-9A-Fa-f]", "").sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)
  }

  private def bytes2hex(bytes: Array[Byte], sep: Option[String] = None): String = sep match {
    case None => bytes.map("%02x".format(_)).mkString
    case _ => bytes.map("%02x".format(_)).mkString(sep.get)
  }

  def deleteToken(tokenUuid: String): Option[EmailToken] = DB.withConnection { implicit conn =>
    val create: DSLContext = DSL.using(conn)
    val result = create
      .selectFrom(EMAIL_TOKEN)
      .where(EMAIL_TOKEN.UUID.eq(tokenUuid.as.asBinary))
      .fetchOne

    if (null == result) {
      None
    } else {
      create.executeDelete(result)
      Some(result.into(classOf[EmailToken]))
    }
  }


  def save(token: MailToken): MailToken = DB.withConnection { implicit conn =>
    DSL.using(conn).executeInsert(token.asEmailToken)
    token
  }

  def findToken(token: String) = DB.withConnection { implicit conn =>
    val result = DSL.using(conn)
      .selectFrom(EMAIL_TOKEN)
      .where(EMAIL_TOKEN.UUID.eq(token.as.asBinary))
      .fetchOneInto(classOf[EmailToken])

    if (null == result) None
    else Some(result)
  }

  def deleteExpiredTokens(): Unit = DB.withConnection { implicit conn =>
    DSL.using(conn).delete(EMAIL_TOKEN).where(EMAIL_TOKEN.EXPIRATION_TIME.lt(new Timestamp(System.currentTimeMillis))).execute
  }
}
