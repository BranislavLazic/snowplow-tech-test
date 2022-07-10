package com.snowplow.config

import cats.effect.IO
import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import doobie.hikari.HikariTransactor

import scala.concurrent.ExecutionContext

object Database {
  def transactor(dbConfig: DbConfig, executionContext: ExecutionContext): IO[HikariTransactor[IO]] = {
    val config = new HikariConfig()
    config.setJdbcUrl(dbConfig.url)
    config.setUsername(dbConfig.username)
    config.setPassword(dbConfig.password)
    IO.pure(HikariTransactor.apply[IO](new HikariDataSource(config), executionContext))
  }

}
