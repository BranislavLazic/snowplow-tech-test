package com.snowplow.route

import cats.data.Validated.{ Invalid, Valid }
import cats.effect.Async
import cats.implicits._
import com.snowplow.model.{ JsonSchema, ServiceResponse }
import com.snowplow.service.JsonSchemaService
import com.snowplow.validator.SchemaIdValidator
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.Header.Raw
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.typelevel.ci.CIString
import io.circe.parser._

class JsonSchemaRoutes[F[_]](jsonSchemaService: JsonSchemaService[F])(implicit F: Async[F]) extends Http4sDsl[F] {

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "schema" / schemaId =>
      withValidSchemaId(schemaId)(handleSchemaUpload(schemaId, req))
    case GET -> Root / "schema" / schemaId =>
      withValidSchemaId(schemaId)(handleSchemaDownload(schemaId))
    case req @ POST -> Root / "validate" / schemaId =>
      withValidSchemaId(schemaId)(handleSchemaValidation(schemaId, req))
  }

  private def handleSchemaUpload(schemaId: String, req: Request[F]): F[Response[F]] =
    req
      .attemptAs[Json]
      .map(json => jsonSchemaService.uploadSchema(schemaId, json.deepDropNullValues.noSpaces))
      .foldF(
        _ => BadRequest(ServiceResponse("uploadSchema", schemaId, "error", Some("Invalid JSON"))),
        _.flatMap {
          case sr @ ServiceResponse(_, _, "error", _) => Conflict(sr)
          case sr                                     => Ok(sr.asJson.dropNullValues)
        }
      )

  private def handleSchemaDownload(schemaId: String): F[Response[F]] =
    jsonSchemaService.downloadSchema(schemaId).flatMap {
      case Some(JsonSchema(_, content)) =>
        parse(content)
          .map { contentAsJson =>
            Ok(contentAsJson)
              .map(
                _.withContentType(`Content-Type`(MediaType.application.`octet-stream`))
                  .withHeaders(
                    Raw(CIString("Content-Disposition"), s"attachment;filename=$schemaId.json")
                  )
              )
          }
          .getOrElse(BadRequest("Failed to parse the schema content"))
      case None => NotFound()
    }

  private def handleSchemaValidation(schemaId: String, req: Request[F]): F[Response[F]] =
    req
      .attemptAs[Json]
      .map(json => jsonSchemaService.validateSchema(schemaId, json.deepDropNullValues))
      .foldF(
        _ => BadRequest(ServiceResponse("uploadSchema", schemaId, "error", Some("Invalid JSON"))),
        _.flatMap {
          case Some(sr @ ServiceResponse(_, _, "error", _)) => BadRequest(sr)
          case Some(sr)                                     => Ok(sr.asJson.dropNullValues)
          case None                                         => NotFound()
        }
      )

  private def withValidSchemaId(schemaId: String)(f: F[Response[F]]): F[Response[F]] =
    SchemaIdValidator.validate(schemaId) match {
      case Valid(_)    => f
      case Invalid(sr) => BadRequest(sr)
    }
}
