package io.paradoxical.finatra.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.internal.marshalling.MessageBodyManager
import io.paradoxical.global.tiny.StringValue
import scala.util.Try

object BodyParsing {
  /**
   * Try and read the body parameter off the request
   *
   * @tparam T
   * @return
   */
  private[finatra] def getBodyFromRequest[T: Manifest](request: Request, bodyParser: MessageBodyManager): Try[T] = {
    Try {
      bodyParser.read[T](request)
    }
  }

  /**
   * Try and read the parameter into a value
   *
   * Useful for tiny types
   *
   * @param name
   * @tparam T
   * @return
   */
  private[finatra] def getParamTypeFromRequest[T: Manifest](request: Request, name: String, objectMapper: ObjectMapper with ScalaObjectMapper): Try[T]= {
    val param = request.getParam(name)
    Try {
      // special handling if a param request is a string param value type
      // but is not wrapped with quotes to make it serializable as a string
      if (!param.startsWith("\"") && !param.endsWith("\"") && classOf[StringValue].isAssignableFrom(manifest[T].runtimeClass)) {
        objectMapper.readValue[T](s""""${param}"""")
      } else {
        objectMapper.readValue[T](param)
      }
    }
  }
}
