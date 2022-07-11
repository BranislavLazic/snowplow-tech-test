import cats.effect.IO
import com.snowplow.model.{ JsonSchema, ServiceResponse }
import com.snowplow.repository.JsonSchemaRepository
import com.snowplow.route.JsonSchemaRoutes
import com.snowplow.service.JsonSchemaService
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.implicits._
import org.http4s.{ EmptyBody, Method, Request, Status, Uri }
import org.http4s.circe._
import io.circe.literal._

import scala.util.Random

class JsonSchemaRoutesSpec extends BaseSpec {
  private val jsonSchemaRepositoryMock = mock[JsonSchemaRepository[IO]]
  private val service                  = new JsonSchemaRoutes[IO](new JsonSchemaService[IO](jsonSchemaRepositoryMock)).routes

  private val testSchema =
    """
      |{
      |  "$schema": "http://json-schema.org/draft-04/schema#",
      |  "type": "object",
      |  "properties": {
      |    "source": {
      |      "type": "string"
      |    },
      |    "destination": {
      |      "type": "string"
      |    },
      |    "timeout": {
      |      "type": "integer",
      |      "minimum": 0,
      |      "maximum": 32767
      |    },
      |    "chunks": {
      |      "type": "object",
      |      "properties": {
      |        "size": {
      |          "type": "integer"
      |        },
      |        "number": {
      |          "type": "integer"
      |        }
      |      },
      |      "required": ["size"]
      |    }
      |  },
      |  "required": ["source", "destination"]
      |}
      |""".stripMargin

  "JsonSchemaRoutes" should {
    "get a schema by id" in {
      (jsonSchemaRepositoryMock.findById _).expects(*).returning(IO.pure(Some(JsonSchema("config-schema", testSchema))))
      val response = service.orNotFound.run(Request(method = Method.GET, uri = uri"/schema/config-schema"))
      checkResponse[Json](response, Status.Ok) shouldBe true
    }

    "get a schema by id and return HTTP status 404 Not Found" in {
      (jsonSchemaRepositoryMock.findById _).expects(*).returning(IO.pure(None))
      val response = service.orNotFound.run(Request(method = Method.GET, uri = uri"/schema/config-schema"))
      checkResponse[Json](response, Status.NotFound) shouldBe true
    }

    "return HTTP status 400 Bad Request when schema id is blank" in {
      (jsonSchemaRepositoryMock.findById _).expects(*).returning(IO.pure(None))
      val response = service.orNotFound.run(Request(method = Method.GET, uri = uri"/schema/"))
      checkResponse[Json](
        response,
        Status.BadRequest,
        Some(
          ServiceResponse(
            "validateSchemaId",
            "",
            "error",
            Some(s"Schema id cannot be blank.")
          ).asJson
        )
      ) shouldBe true
    }

    "return HTTP status 400 Bad Request when schema id has more than 512 characters" in {
      (jsonSchemaRepositoryMock.findById _).expects(*).returning(IO.pure(None))
      val schemaId = Iterator.continually(Random.nextPrintableChar()).filter(_.isLetter).take(513).mkString
      val response = service.orNotFound.run(
        Request(
          method = Method.GET,
          uri = Uri.unsafeFromString(s"/schema/$schemaId")
        )
      )
      checkResponse[Json](
        response,
        Status.BadRequest,
        Some(
          ServiceResponse(
            "validateSchemaId",
            schemaId,
            "error",
            Some(s"Schema id cannot contain more than 512 characters.")
          ).asJson
        )
      ) shouldBe true
    }

    "return HTTP status 400 Bad Request when body is empty" in {
      val response =
        service.orNotFound.run(
          Request(method = Method.POST, uri = uri"/schema/config-schema", body = EmptyBody)
        )
      checkResponse[Json](
        response,
        Status.BadRequest,
        Some(ServiceResponse("uploadSchema", "config-schema", "error", Some("Invalid JSON")).asJson)
      ) shouldBe true
    }

    "return HTTP status 409 Conflict when schema already exists" in {
      (jsonSchemaRepositoryMock.findById _).expects(*).returning(IO.pure(Some(JsonSchema("config-schema", testSchema))))
      val response =
        service.orNotFound.run(
          Request(method = Method.POST, uri = uri"/schema/config-schema").withEntity(json"""{"test":"test"}""")
        )
      checkResponse[Json](
        response,
        Status.Conflict,
        Some(
          ServiceResponse(
            "uploadSchema",
            "config-schema",
            "error",
            Some(s"Schema with id config-schema already exists.")
          ).asJson
        )
      ) shouldBe true
    }

    "create the schema" in {
      (jsonSchemaRepositoryMock.findById _).expects(*).returning(IO.pure(None))
      (jsonSchemaRepositoryMock.insert _).expects(*).returning(IO.pure(1))
      val response =
        service.orNotFound.run(
          Request(method = Method.POST, uri = uri"/schema/config-schema").withEntity(
            json"""$testSchema"""
          )
        )
      checkResponse[Json](
        response,
        Status.Ok,
        Some(ServiceResponse("uploadSchema", "config-schema", "success", None).asJson.dropNullValues)
      ) shouldBe true
    }

    "fail to validate the schema against the document and return HTTP status code 400" in {
      (jsonSchemaRepositoryMock.findById _).expects(*).returning(IO.pure(Some(JsonSchema("config-schema", testSchema))))
      val response =
        service.orNotFound.run(
          Request(method = Method.POST, uri = uri"/validate/config-schema").withEntity(
            json"""{
                     "source": "/home/alice/image.iso",
                     "destination": "/mnt/storage",
                     "timeout": null,
                     "chunks": {
                       "size": 1024,
                       "number": null
                     }
                   }
                """
          )
        )
      checkResponse[Json](
        response,
        Status.BadRequest,
        Some(
          ServiceResponse(
            "validateDocument",
            "config-schema",
            "error",
            Some(
              """Property '/root/chunks/number' instance type (null) does not match any allowed primitive type (allowed: ["integer"])"""
            )
          ).asJson
        )
      ) shouldBe true
    }

    "validate the schema against the document successfully and return HTTP status code 200" in {
      (jsonSchemaRepositoryMock.findById _).expects(*).returning(IO.pure(Some(JsonSchema("config-schema", testSchema))))
      val response =
        service.orNotFound.run(
          Request(method = Method.POST, uri = uri"/validate/config-schema").withEntity(
            json"""{
                     "source": "/home/alice/image.iso",
                     "destination": "/mnt/storage",
                     "timeout": 5,
                     "chunks": {
                       "size": 1024,
                       "number": 5
                     }
                   }
                """
          )
        )
      checkResponse[Json](
        response,
        Status.Ok,
        Some(
          ServiceResponse("validateDocument", "config-schema", "success").asJson.dropNullValues
        )
      ) shouldBe true
    }
  }
}
