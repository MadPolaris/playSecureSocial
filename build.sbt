import com.github.sbtliquibase.SbtLiquibase

import com.github.sbtliquibase.Import._

name := """playSecureSocial"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala).enablePlugins(SbtLiquibase)

scalaVersion := "2.11.4"

resolvers += Resolver.sonatypeRepo("snapshots")

val JOOQ_VERSION = "3.4.0"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  
  // WebJars pull in client-side web libraries
  "org.webjars" % "bootstrap" % "3.2.0",
  "org.webjars" % "flot" % "0.8.0",

  //persistence layer
  "mysql" % "mysql-connector-java" % "5.1.31",
  "org.jooq" % "jooq" % JOOQ_VERSION,
  "org.jooq" % "jooq-meta" % JOOQ_VERSION,
  "org.jooq" % "jooq-scala" % JOOQ_VERSION,
  "org.jooq" % "jooq-codegen" % JOOQ_VERSION,
  "javax.persistence" % "persistence-api" % "1.0.2",
  "javax.validation" % "validation-api" % "1.1.0.Final",
  "com.fasterxml.uuid" % "java-uuid-generator" % "3.1.3",
  "mysql" % "mysql-connector-java" % "5.1.31" % "jooq",

  //Secure Social
  "ws.securesocial" %% "securesocial" % "master-SNAPSHOT" withSources,
  "ws.securesocial" %% "securesocial" % "master-SNAPSHOT" classifier "assets"
)

seq(jooqSettings: _*)

jooqVersion := JOOQ_VERSION

val dbName = "secure_social"

val dbUser = "root"

val dbPass = "1q2w3e4r5t"

jooqOptions := Seq(
  "jdbc.driver" -> "com.mysql.jdbc.Driver",
  "jdbc.url" -> "jdbc:mysql://localhost:3306/secure_social",
  "jdbc.user" -> dbUser,
  "jdbc.password" -> dbPass,
  "generator.database.name" -> "org.jooq.util.mysql.MySQLDatabase",
  "generator.database.inputSchema" -> "secure_social",
  "generator.target.packageName" -> "imadz.model.gen",
  "generator.generate.pojos" -> "false",
  "generator.generate.daos" -> "false",
  "generator.generate.jpaAnnotations" -> "true",
  "generator.generate.validationAnnotations" -> "false",
  "generator.generate.generatedAnnotation" -> "true"
)

liquibaseChangelog := "conf/migrations/changelog.xml"

liquibaseUsername := dbUser

liquibasePassword := dbPass

liquibaseDriver   := "com.mysql.jdbc.Driver"

liquibaseUrl      := "jdbc:mysql://localhost:3306/secure_social?createDatabaseIfNotExist=true"
