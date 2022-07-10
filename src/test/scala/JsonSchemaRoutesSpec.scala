import cats.effect.IO
import com.snowplow.model.{ JsonSchema, ServiceResponse }
import com.snowplow.repository.JsonSchemaRepository
import com.snowplow.route.JsonSchemaRoutes
import com.snowplow.service.JsonSchemaService
import fs2.Stream
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.implicits._
import org.http4s.{ EmptyBody, Method, Request, Status }

class JsonSchemaRoutesSpec extends BaseSpec {
  private val jsonSchemaRepositoryMock = mock[JsonSchemaRepository[IO]]
  private val service                  = new JsonSchemaRoutes[IO](new JsonSchemaService[IO](jsonSchemaRepositoryMock)).routes

  "JsonSchemaRoutes" should {
    "get a schema by id" in {
      (jsonSchemaRepositoryMock.findById _).expects(*).returning(IO.pure(Some(JsonSchema("config-schema", "{}"))))
      val response = service.orNotFound.run(Request(method = Method.GET, uri = uri"/schema/config-schema"))
      checkResponse[Json](response, Status.Ok) shouldBe true
    }

    "get a schema by id and return HTTP status 404 Not Found" in {
      (jsonSchemaRepositoryMock.findById _).expects(*).returning(IO.pure(None))
      val response = service.orNotFound.run(Request(method = Method.GET, uri = uri"/schema/config-schema"))
      checkResponse[Json](response, Status.NotFound) shouldBe true
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
      (jsonSchemaRepositoryMock.findById _).expects(*).returning(IO.pure(Some(JsonSchema("config-schema", "{}"))))
      val response =
        service.orNotFound.run(
          Request(method = Method.POST, uri = uri"/schema/config-schema").withBodyStream(
            Stream.emits(
              """{"test":"test"}""".asJson.toString.getBytes
            )
          )
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
          Request(method = Method.POST, uri = uri"/schema/config-schema").withBodyStream(
            Stream.emits(
              """{"test":"test"}""".asJson.toString.getBytes
            )
          )
        )
      checkResponse[Json](
        response,
        Status.Ok,
        Some(ServiceResponse("uploadSchema", "config-schema", "success", None).asJson)
      ) shouldBe true
    }
  }
}
