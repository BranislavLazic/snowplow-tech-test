package com.snowplow.route

import cats.effect.Async
import cats.implicits._
import com.snowplow.model.{ JsonSchema, ServiceResponse }
import com.snowplow.service.JsonSchemaService
import io.circe.Json
import io.circe.generic.auto._
import org.http4s.Header.Raw
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.typelevel.ci.CIString

class JsonSchemaRoutes[F[_]](jsonSchemaService: JsonSchemaService[F])(implicit F: Async[F]) extends Http4sDsl[F] {

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "schema" / schemaId => handleJsonUpload(jsonSchemaService, schemaId, req)
    case GET -> Root / "schema" / schemaId        => handleSchemaDownload(jsonSchemaService, schemaId)
  }

  private def handleJsonUpload(
      jsonSchemaService: JsonSchemaService[F],
      schemaId: String,
      req: Request[F]
  ): F[Response[F]] =
    req
      .attemptAs[Json]
      .map(json => jsonSchemaService.uploadSchema(schemaId, json.noSpaces))
      .foldF(
        _ => BadRequest(ServiceResponse("uploadSchema", schemaId, "error", Some("Invalid JSON"))),
        _.flatMap {
          case sr @ ServiceResponse(_, _, "error", _) => Conflict(sr)
          case sr                                     => Ok(sr)
        }
      )

  private def handleSchemaDownload(jsonSchemaService: JsonSchemaService[F], schemaId: String): F[Response[F]] =
    jsonSchemaService.downloadSchema(schemaId).flatMap {
      case Some(JsonSchema(_, content)) =>
        Ok(content)
          .map(_.withContentType(`Content-Type`(MediaType.application.`octet-stream`)))
          .map(_.withHeaders(Raw(CIString("Content-Disposition"), s"attachment;filename=$schemaId.json")))
      case None => NotFound()
    }
}
