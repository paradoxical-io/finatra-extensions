package io.paradoxical.finatra

import com.google.inject.Module
import com.twitter.finatra.http.HttpServer
import io.paradoxical.finatra.serialization.JsonModule
import io.paradoxical.finatra.swagger.SwaggerDocs

/**
 * Base class that wires in tiny types and swagger support
 */
abstract class HttpServiceBase extends HttpServer with SwaggerDocs {
  override protected def jacksonModule: Module = new JsonModule()
}
