package io.paradoxical.finatra.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.internal.marshalling.MessageBodyManager
import io.paradoxical.finatra.Framework.ApiDescription
import javax.inject.Inject
import scala.util.Try

private[finatra] class RestApiInternal extends Controller with ApiDescription {
  /**
   * The body parser from finatra
   */
  @Inject var bodyParser: MessageBodyManager = _

  /**
   * The finatra json object mapper
   */
  @Inject var objectMapper: ObjectMapper with ScalaObjectMapper = _

  /**
   * A utility to help read body parameters from the request
   *
   * @param request
   */
  implicit class RichRequest(request: Request) {
    def getBody[T: Manifest]: Try[T] = {
      BodyParsing.getBodyFromRequest[T](request, bodyParser)
    }

    def getParamType[T: Manifest](name: String): Try[T] = {
      BodyParsing.getParamTypeFromRequest[T](request, name, objectMapper)
    }
  }
}
