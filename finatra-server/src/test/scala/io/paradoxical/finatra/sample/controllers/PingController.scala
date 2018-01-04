package io.paradoxical.finatra.sample.controllers

import com.twitter.finatra.request.RouteParam
import io.paradoxical.finatra.Framework
import io.paradoxical.global.tiny.StringValue

class PingController extends Framework.RestApi {
  getWithDoc("/ping/:data") {
    _.description("Ping API")
  } { request: PingRequest =>
    info("ping")
    PingResponse(request.data)
  }
}

case class PingRequest(@RouteParam data: TinyTypeData)

case class PingResponse(data: TinyTypeData)

case class TinyTypeData(value: String) extends StringValue