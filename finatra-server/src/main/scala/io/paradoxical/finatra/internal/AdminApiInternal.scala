package io.paradoxical.finatra.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.twitter.finagle.http.{Request, RouteIndex}
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.internal.marshalling.MessageBodyManager
import io.paradoxical.finatra.Framework.{AdminRoute, ApiDescription}
import javax.inject.Inject
import scala.util.Try

private[finatra] class AdminApiInternal  extends Controller with ApiDescription {
  protected val defaultGroupName = "Tasks"

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

  /**
   * Admin routes must be on a specific route and can't just be anywhere
   *
   * @param s
   * @return
   */
  protected implicit def onAdmin(s: String): AdminRoute = {
    if (s.contains(":")) {
      // dynamic route
      AdminRoute("/admin/finatra/" + s.stripPrefix("/"))
    } else {
      // static route
      AdminRoute("/admin/" + s.stripPrefix("/"))
    }
  }

  def adminGet[RequestType: Manifest, ResponseType: Manifest](
    route: AdminRoute,
    name: String = "")(callback: RequestType => ResponseType): Unit = {
    get(route.route, name, admin = true, None)(callback)
  }

  def adminPost[RequestType: Manifest, ResponseType: Manifest](
    route: AdminRoute,
    name: String = "")(callback: RequestType => ResponseType): Unit = {
    post(route.route, name, admin = true, None)(callback)
  }

  def adminPut[RequestType: Manifest, ResponseType: Manifest](
    route: AdminRoute,
    name: String = "")(callback: RequestType => ResponseType): Unit = {
    put(route.route, name, admin = true, None)(callback)
  }

  def adminDelete[RequestType: Manifest, ResponseType: Manifest](
    route: AdminRoute,
    name: String = "")(callback: RequestType => ResponseType): Unit = {
    delete(route.route, name, admin = true, None)(callback)
  }

  def adminOptions[RequestType: Manifest, ResponseType: Manifest](
    route: AdminRoute,
    name: String = "")(callback: RequestType => ResponseType): Unit = {
    options(route.route, name, admin = true, None)(callback)
  }

  def adminPatch[RequestType: Manifest, ResponseType: Manifest](
    route: AdminRoute,
    name: String = "")(callback: RequestType => ResponseType): Unit = {
    patch(route.route, name, admin = true, None)(callback)
  }

  def adminHead[RequestType: Manifest, ResponseType: Manifest](
    route: AdminRoute,
    name: String = "")(callback: RequestType => ResponseType): Unit = {
    head(route.route, name, admin = true, None)(callback)
  }

  def adminTrace[RequestType: Manifest, ResponseType: Manifest](
    route: AdminRoute,
    name: String = "")(callback: RequestType => ResponseType): Unit = {
    trace(route.route, name, admin = true, None)(callback)
  }

  /**
   * Register an admin task that shows up in the interface under the group name "Tasks"
   *
   * @param route
   * @param callback
   * @tparam RequestType
   * @tparam ResponseType
   */
  def uiTask[RequestType: Manifest, ResponseType: Manifest](
    route: AdminRoute,
    groupName: String = defaultGroupName)(callback: Request => RequestType) = {
    get(
      route = route.route,
      admin = true,
      index = Some(RouteIndex(group = groupName, alias = route.route))
    )(callback)
  }
}
