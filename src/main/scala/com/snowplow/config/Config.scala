package com.snowplow.config

import cats.effect.IO
import io.circe.config.parser
import io.circe.generic.auto._

case class DbConfig(url: String, username: String, password: String, poolSize: Int)
case class ServerConfig(host: String, port: Int)
case class Config(dbConfig: DbConfig, serverConfig: ServerConfig)

object Config {
  def load(): IO[Config] =
    for {
      dbConfig     <- parser.decodePathF[IO, DbConfig]("db")
      serverConfig <- parser.decodePathF[IO, ServerConfig]("server")
    } yield Config(dbConfig, serverConfig)
}
