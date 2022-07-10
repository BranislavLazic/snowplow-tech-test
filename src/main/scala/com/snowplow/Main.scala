package com.snowplow

import cats.effect.{ ExitCode, IO, IOApp, Resource }
import com.snowplow.config.{ Config, Database, DbConfig }
import com.snowplow.repository.JsonSchemaRepository
import com.snowplow.route.JsonSchemaRoutes
import com.snowplow.service.JsonSchemaService
import doobie.ExecutionContexts
import org.flywaydb.core.Flyway
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits._
import org.http4s.server.Server

object Main extends IOApp {

  private def initMigrations(dbConfig: DbConfig): Resource[IO, Unit] = Resource.eval(
    IO
      .delay {
        Flyway
          .configure()
          .dataSource(dbConfig.url, dbConfig.username, dbConfig.password)
          .load()
          .migrate()
      }
      .as(())
  )

  private def initServer: Resource[IO, Server] =
    for {
      config    <- Resource.eval(Config.load())
      _         <- initMigrations(config.dbConfig)
      dbExecCtx <- ExecutionContexts.fixedThreadPool[IO](config.dbConfig.poolSize)
      xa        <- Resource.eval(Database.transactor(config.dbConfig, dbExecCtx))
      srv <- BlazeServerBuilder[IO]
        .bindHttp(config.serverConfig.port, config.serverConfig.host)
        .withHttpApp(
          new JsonSchemaRoutes[IO].routes(new JsonSchemaService[IO](new JsonSchemaRepository[IO](xa))).orNotFound
        )
        .resource
    } yield srv

  override def run(args: List[String]): IO[ExitCode] =
    for {
      exitCode <- initServer.use(_ => IO.never).as(ExitCode.Success)
    } yield exitCode

}
