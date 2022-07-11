package com.snowplow

import cats.effect.{ ExitCode, IO, IOApp, Resource }
import com.snowplow.config.{ Config, Database, DbConfig }
import com.snowplow.repository.PgJsonSchemaRepository
import com.snowplow.route.JsonSchemaRoutes
import com.snowplow.service.JsonSchemaService
import doobie.ExecutionContexts
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits._
import org.http4s.server.Server

object Main extends IOApp {

  private def initMigrations(dbConfig: DbConfig): Resource[IO, MigrateResult] = Resource.eval(
    IO.blocking {
      Flyway
        .configure()
        .dataSource(dbConfig.url, dbConfig.username, dbConfig.password)
        .load()
        .migrate()
    }
  )

  private def initServer: Resource[IO, Server] =
    for {
      config     <- Resource.eval(Config.load())
      _          <- initMigrations(config.dbConfig)
      dbExecCtx  <- ExecutionContexts.fixedThreadPool[IO](config.dbConfig.poolSize)
      transactor <- Resource.eval(Database.transactor(config.dbConfig, dbExecCtx))
      server <- BlazeServerBuilder[IO]
        .bindHttp(config.serverConfig.port, config.serverConfig.host)
        .withHttpApp(
          new JsonSchemaRoutes[IO](
            new JsonSchemaService[IO](new PgJsonSchemaRepository[IO](transactor))
          ).routes.orNotFound
        )
        .resource
    } yield server

  override def run(args: List[String]): IO[ExitCode] = initServer.use(_ => IO.never).as(ExitCode.Success)

}
