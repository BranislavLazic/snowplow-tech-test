package com.snowplow.route

import cats.effect.IO
import com.snowplow.service.JsonSchemaService
import org.http4s.{ HttpRoutes, Response }
import org.http4s.dsl.Http4sDsl
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec._

object JsonSchemaRoutes extends Http4sDsl[IO] {

  def routes(jsonSchemaService: JsonSchemaService): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ POST -> Root / "schema" / schemaId => Ok("")
    case GET -> Root / "schema" / schemaId =>
      handleSchemaDownload(schemaId, jsonSchemaService)
  }

  private def handleSchemaDownload(schemaId: String, jsonSchemaService: JsonSchemaService): IO[Response[IO]] =
    jsonSchemaService.downloadSchema(schemaId).flatMap {
      case Some(value) => Ok(value)
      case None        => NotFound()
    }
}
