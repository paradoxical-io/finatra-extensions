package com.twitter.finatra.http

import io.paradoxical.finatra.swagger.FinatraSwagger
import com.twitter.finagle.http.RouteIndex
import com.twitter.finatra.http.internal.routing.Route
import io.swagger.models.{Operation, Swagger}

/**
 * To work around the accessibility of RouteDSL, this class is in "com.twitter.finatra.http" package
 */
object SwaggerRouteDSL {
  implicit def convertToSwaggerRouteDSL(dsl: RouteDSL)(implicit swagger: Swagger): SwaggerRouteDSL = new SwaggerRouteDSLWapper(dsl)(swagger)
}

trait SwaggerRouteDSL extends RouteDSL {
  self =>
  implicit protected val swagger: Swagger

  protected val dsl: RouteDSL = this

  private def prefixRoute(route: String): String = {
    contextVar().prefix match {
      case prefix if prefix.nonEmpty && prefix.startsWith("/") => s"$prefix$route"
      case prefix if prefix.nonEmpty && !prefix.startsWith("/") => s"/$prefix$route"
      case _ => route
    }
  }

  def postWithDoc[RequestType: Manifest, ResponseType: Manifest](
    route: String, name: String = "", admin: Boolean = false, routeIndex: Option[RouteIndex] = None)
    (doc: Operation => Unit)
    (callback: RequestType => ResponseType): Unit = {
    registerOperation(prefixRoute(route), "post")(doc)
    dsl.post(route, name, admin, routeIndex)(callback)
  }

  def getWithDoc[RequestType: Manifest, ResponseType: Manifest](
    route: String, name: String = "", admin: Boolean = false, routeIndex: Option[RouteIndex] = None)
    (doc: Operation => Unit)
    (callback: RequestType => ResponseType): Unit = {
    registerOperation(prefixRoute(route), "get")(doc)
    dsl.get(route, name, admin, routeIndex)(callback)
  }

  def putWithDoc[RequestType: Manifest, ResponseType: Manifest](
    route: String, name: String = "", admin: Boolean = false, routeIndex: Option[RouteIndex] = None)
    (doc: Operation => Unit)
    (callback: RequestType => ResponseType): Unit = {
    registerOperation(prefixRoute(route), "put")(doc)
    dsl.put(route, name, admin, routeIndex)(callback)
  }

  def patchWithDoc[RequestType: Manifest, ResponseType: Manifest](
    route: String, name: String = "", admin: Boolean = false, routeIndex: Option[RouteIndex] = None)
    (doc: Operation => Unit)
    (callback: RequestType => ResponseType): Unit = {
    registerOperation(prefixRoute(route), "patch")(doc)
    dsl.patch(route, name, admin, routeIndex)(callback)
  }

  def headWithDoc[RequestType: Manifest, ResponseType: Manifest](
    route: String, name: String = "", admin: Boolean = false, routeIndex: Option[RouteIndex] = None)
    (doc: Operation => Unit)
    (callback: RequestType => ResponseType): Unit = {
    registerOperation(prefixRoute(route), "head")(doc)
    dsl.head(route, name, admin, routeIndex)(callback)
  }

  def deleteWithDoc[RequestType: Manifest, ResponseType: Manifest](
    route: String, name: String = "", admin: Boolean = false, routeIndex: Option[RouteIndex] = None)
    (doc: Operation => Unit)
    (callback: RequestType => ResponseType): Unit = {
    registerOperation(prefixRoute(route), "delete")(doc)
    dsl.delete(route, name, admin, routeIndex)(callback)
  }

  def optionsWithDoc[RequestType: Manifest, ResponseType: Manifest](
    route: String, name: String = "", admin: Boolean = false, routeIndex: Option[RouteIndex] = None)
    (doc: Operation => Unit)
    (callback: RequestType => ResponseType): Unit = {
    registerOperation(prefixRoute(route), "options")(doc)
    dsl.options(route, name, admin, routeIndex)(callback)
  }

  private def registerOperation(path: String, method: String)(doc: Operation => Unit): Unit = {
    val op = new Operation
    doc(op)

    val sanitizedPath = path.stripSuffix(Route.OptionalTrailingSlashIdentifier)
    FinatraSwagger.convertToFinatraSwagger(swagger).registerOperation(sanitizedPath, method, op)
  }
}

private class SwaggerRouteDSLWapper(protected override val dsl: RouteDSL)(implicit protected val swagger: Swagger) extends SwaggerRouteDSL
