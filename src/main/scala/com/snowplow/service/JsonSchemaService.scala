package com.snowplow.service

import cats.effect.Async
import cats.implicits._
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.snowplow.model.{ JsonSchema, ServiceResponse }
import com.snowplow.repository.JsonSchemaRepository
import io.circe.Json

import scala.jdk.CollectionConverters.IteratorHasAsScala

class JsonSchemaService[F[_]](jsonSchemaRepository: JsonSchemaRepository[F])(implicit F: Async[F]) {

  def uploadSchema(id: String, value: String): F[ServiceResponse] =
    for {
      exists <-
        jsonSchemaRepository.findById(id).map(_.exists(_.id == id))
      uploadResult <-
        if (exists)
          F.pure(ServiceResponse("uploadSchema", id, "error", Some(s"Schema with id $id already exists.")))
        else
          jsonSchemaRepository.insert(JsonSchema(id, value)).map(_ => ServiceResponse("uploadSchema", id, "success"))

    } yield uploadResult

  def downloadSchema(id: String): F[Option[JsonSchema]] = jsonSchemaRepository.findById(id)

  def validateSchema(id: String, json: Json): F[Option[ServiceResponse]] = {
    val incomingSchema = JsonLoader.fromString(json.dropNullValues.noSpaces)
    val validator      = JsonSchemaFactory.byDefault().getValidator
    for {
      schema <- jsonSchemaRepository.findById(id)
      valid <- F.pure(
        schema
          .map(s => JsonLoader.fromString(s.content))
          .map(node => validator.validate(node, incomingSchema, true))
          .map(processingReport =>
            if (processingReport.isSuccess) ServiceResponse("validateDocument", id, "success")
            else ServiceResponse("validateDocument", id, "error", Some(buildValidationError(processingReport)))
          )
      )
    } yield valid
  }

  private def buildValidationError(processingReport: ProcessingReport): String =
    processingReport
      .iterator()
      .asScala
      .toSeq
      .map { msg =>
        val msgAsJson = msg.asJson()
        val pointer   = msgAsJson.at("/instance/pointer").asText()
        val message   = msgAsJson.at("/message").asText()
        s"Property '/root$pointer' $message"
      }
      .mkString(", ")
}
