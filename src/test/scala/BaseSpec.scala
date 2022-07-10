import cats.effect.IO
import cats.effect.unsafe.IORuntime
import org.http4s.{ EntityDecoder, Response, Status }
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

trait BaseSpec extends AnyWordSpec with Matchers with MockFactory {
  protected implicit val runtime: IORuntime = IORuntime.global

  def checkResponse[A](actual: IO[Response[IO]], expectedStatus: Status, expectedBody: Option[A] = None)(implicit
      ev: EntityDecoder[IO, A]
  ): Boolean = {
    val actualResp  = actual.unsafeRunSync()
    val statusCheck = actualResp.status == expectedStatus
    val bodyCheck = expectedBody.fold[Boolean](
      actualResp.body.compile.toVector.unsafeRunSync().isEmpty
    )(expected => actualResp.as[A].unsafeRunSync() == expected)
    statusCheck && (bodyCheck || expectedBody.isEmpty)
  }
}
