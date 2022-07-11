package com.snowplow.validator

import cats.data.Validated
import cats.implicits._
import com.snowplow.model.ServiceResponse

object SchemaIdValidator {
  private val maxSchemaIdLength = 512
  private val action            = "validateSchemaId"

  def validate(schemaId: String): Validated[ServiceResponse, String] =
    nonBlank(schemaId).andThen(_ => maxLength(schemaId))

  private def nonBlank(schemaId: String): Validated[ServiceResponse, String] =
    if (schemaId.isBlank)
      ServiceResponse(
        action,
        schemaId,
        "error",
        Some(s"Schema id cannot be blank.")
      ).invalid
    else schemaId.valid

  private def maxLength(schemaId: String): Validated[ServiceResponse, String] =
    if (schemaId.length > maxSchemaIdLength)
      ServiceResponse(
        action,
        schemaId,
        "error",
        Some(s"Schema id cannot contain more than $maxSchemaIdLength characters.")
      ).invalid
    else schemaId.valid
}
