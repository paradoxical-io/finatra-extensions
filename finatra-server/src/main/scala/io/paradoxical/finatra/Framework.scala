package io.paradoxical.finatra

import com.twitter.finatra.http.Controller
import io.paradoxical.finatra.internal.{AdminApiInternal, AssetsApiInternal, RestApiInternal}
import io.paradoxical.finatra.swagger.{SwaggerDefinition, SwaggerSupport}
import io.swagger.models.Swagger

object Framework {
  case class AdminRoute(route: String)
  /** Tag to indicate a route is an admin route **/

  /**
   * Rest Api base class, All controllers should inherit this
   */
  abstract class RestApi extends RestApiInternal

  /**
   * Admin api base class. All admin tasks should implement this
   */
  abstract class AdminApi extends AdminApiInternal

  /**
   * Assets base class. Provides utilities for caching and setting assets
   *
   * NOT FOR PRODUCTION USE! ONLY FOR SMALL NON HEAVY LOADED ASSETS!
   */
  abstract class AssetsApi extends AssetsApiInternal

  /**
   * Defines swagger for a service
   */
  trait ApiDescription extends SwaggerSupport {
    self: Controller =>
    override protected implicit val swagger: Swagger = SwaggerDefinition
  }
}



