package io.paradoxical.finatra.sample

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import io.paradoxical.finatra.HttpServiceBase
import io.paradoxical.finatra.sample.controllers.PingController
import io.paradoxical.finatra.swagger.ApiDocumentationConfig

object SampleServerMain extends SampleServer

class SampleServer extends HttpServiceBase {
  override def defaultFinatraHttpPort = ":9999"

  override def documentation = new ApiDocumentationConfig {
    override val description: String = "Sample"
    override val title: String = "API"
    override val version: String = "1.0"
  }

  override def configureHttp(router: HttpRouter): Unit = {
    router
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .filter[CommonFilters]
      .add[PingController]

    configureDocumentation(router)
  }
}
