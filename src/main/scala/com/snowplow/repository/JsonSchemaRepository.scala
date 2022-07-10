package com.snowplow.repository

import com.snowplow.model.JsonSchema

trait JsonSchemaRepository[F[_]] {
  def insert(jsonSchema: JsonSchema): F[Int]
  def findById(id: String): F[Option[JsonSchema]]
}
