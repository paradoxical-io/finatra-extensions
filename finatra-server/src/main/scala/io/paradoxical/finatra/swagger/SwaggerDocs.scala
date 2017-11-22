package io.paradoxical.finatra.swagger

import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.server.TwitterServer
import io.swagger.converter.{ModelConverter, ModelConverters}
import io.swagger.models.Info
import io.swagger.models.auth.ApiKeyAuthDefinition
import io.swagger.scala.converter.SwaggerScalaModelConverter

trait ApiDocumentationConfig {
  val description: String
  val version: String
  val title: String
}

/** Trait to add swagger controller **/
trait SwaggerDocs {
  self: TwitterServer =>
  def documentation: ApiDocumentationConfig

  protected lazy val swaggerInfo = {
    val info =
      SwaggerDefinition.info(
        new Info()
          .description(documentation.description)
          .version(documentation.version)
          .title(documentation.title)
      )

    swaggerSecurityDefinitions.foreach {
      case (name, definition) => info.securityDefinition(name, definition)
    }

    info
  }

  /**
   * Security definitions to apply to swagger
   *
   * @return
   */
  protected def swaggerSecurityDefinitions: Seq[(String, ApiKeyAuthDefinition)] = {
    Nil
  }

  def configureDocumentation(router: HttpRouter): Unit = {
    router.add(new SwaggerController(swagger = SwaggerDefinition))
    router.add[WebjarsController]


    // add the converters in order to swagger
    swaggerConverters.reverse.foreach(ModelConverters.getInstance().addConverter)
  }

  /**
   * Ordered stack of converters.  First element has highest priority
   */
  protected def swaggerConverters: List[ModelConverter] = List(
    new Resolvers.ParameterzedTypeOption,
    new SwaggerScalaModelConverter,
    new ParadoxicalWrappedValueModelResolver,
    new WrappedValueModelResolver
  ) ++ swaggerTypeOverrides

  /**
   * Custom type overrides to hook in for swager
   */
  protected lazy val swaggerTypeOverrides: List[ModelConverter] = List()
}
