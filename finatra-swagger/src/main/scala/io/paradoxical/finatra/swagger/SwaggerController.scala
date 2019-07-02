package io.paradoxical.finatra.swagger

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.response.Mustache
import io.swagger.models.Swagger
import io.swagger.util.Json

@Mustache("redoc")
case class Redoc(modelPath: String)

class SwaggerController(docPath: String = "/api-docs", swagger: Swagger) extends Controller {
  get(s"${docPath}/model") { request: Request =>
    response.ok.body(Json.mapper.writeValueAsString(swagger))
      .contentType("application/json").toFuture
  }

  get(s"${docPath}/ui") { request: Request =>
    response.temporaryRedirect
      .location(s"../../webjars/swagger-ui/3.4.4/index.html?url=/$docPath/model")
  }

  get(s"$docPath/redoc") { request: Request =>
    Redoc(modelPath = s"$docPath/model")
  }
}
