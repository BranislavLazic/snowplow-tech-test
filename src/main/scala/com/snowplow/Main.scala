package com.snowplow

import cats.effect.{ ExitCode, IO, IOApp }
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s._
import com.snowplow.route.JsonSchemaRoutes
import org.http4s.implicits._

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(JsonSchemaRoutes.routes.orNotFound)
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)
}
