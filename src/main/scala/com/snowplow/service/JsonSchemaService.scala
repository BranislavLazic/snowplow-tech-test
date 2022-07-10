package com.snowplow.service

import cats.effect.Async
import cats.implicits._
import com.snowplow.model.{ JsonSchema, ServiceResponse }
import com.snowplow.repository.JsonSchemaRepository

class JsonSchemaService[F[_]](jsonSchemaRepository: JsonSchemaRepository[F])(implicit F: Async[F]) {

  def uploadSchema(id: String, value: String): F[ServiceResponse] =
    for {
      exists <-
        jsonSchemaRepository.findById(id).map(_.exists(_.id == id))
      uploadResult <-
        if (exists)
          F.pure(
            ServiceResponse("uploadSchema", id, "error", Some(s"Schema with id $id already exists."))
          )
        else jsonSchemaRepository.insert(JsonSchema(id, value)).map(_ => ServiceResponse("uploadSchema", id, "success"))

    } yield uploadResult

  def downloadSchema(id: String): F[Option[JsonSchema]] = jsonSchemaRepository.findById(id)
}
