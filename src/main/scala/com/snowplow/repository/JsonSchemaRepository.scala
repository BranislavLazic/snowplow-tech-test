package com.snowplow.repository

import cats.effect.IO
import com.snowplow.model.JsonSchema
import doobie.Transactor
import doobie.implicits._

class JsonSchemaRepository(transactor: Transactor[IO]) {
  def insert(jsonSchema: JsonSchema): IO[Int] =
    sql"""insert into json_schemas (id, value) values (${jsonSchema.id}, ${jsonSchema.value})""".update.run
      .transact(transactor)

  def findById(id: String): IO[Option[JsonSchema]] =
    sql"""select * from json_schemas where id = $id""".query[JsonSchema].option.transact(transactor)
}
