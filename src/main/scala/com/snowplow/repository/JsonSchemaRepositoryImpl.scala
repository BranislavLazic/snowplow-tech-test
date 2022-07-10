package com.snowplow.repository

import cats.effect.Async
import com.snowplow.model.JsonSchema
import doobie.Transactor
import doobie.implicits._

class JsonSchemaRepositoryImpl[F[_]: Async](transactor: Transactor[F]) extends JsonSchemaRepository[F] {
  def insert(jsonSchema: JsonSchema): F[Int] =
    sql"""insert into json_schemas (id, content) values (${jsonSchema.id}, ${jsonSchema.content})""".update.run
      .transact(transactor)

  def findById(id: String): F[Option[JsonSchema]] =
    sql"""select * from json_schemas where id = $id""".query[JsonSchema].option.transact(transactor)
}
