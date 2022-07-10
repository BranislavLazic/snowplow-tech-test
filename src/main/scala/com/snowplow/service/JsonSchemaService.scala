package com.snowplow.service

import cats.effect.IO
import com.snowplow.model.{ JsonSchema, ServiceError }
import com.snowplow.repository.JsonSchemaRepository

class JsonSchemaService(jsonSchemaRepository: JsonSchemaRepository) {

  def uploadSchema(id: String, value: String): IO[Either[ServiceError, Unit]] =
    for {
      exists <- jsonSchemaRepository.findById(id).map(_.exists(_.id == id))
      uploadResult <-
        if (!exists) jsonSchemaRepository.insert(JsonSchema(id, value)).map(_ => Right(()))
        else IO.pure(Left(ServiceError(s"Schema with id $id already exists.")))
    } yield uploadResult

  def downloadSchema(id: String): IO[Option[JsonSchema]] = jsonSchemaRepository.findById(id)
}
