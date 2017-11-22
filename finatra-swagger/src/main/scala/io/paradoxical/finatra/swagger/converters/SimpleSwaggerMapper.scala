package io.paradoxical.finatra.swagger.converters

import io.paradoxical.finatra.swagger.SimpleResolver
import io.swagger.converter.{ModelConverter, ModelConverterContext}
import io.swagger.models.properties.Property
import io.swagger.util.Json
import java.lang.annotation.Annotation
import java.lang.reflect.Type
import java.util
import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.universe._

abstract class SimpleSwaggerMapper[Source: TypeTag] extends SimpleResolver {
  protected def as: Property

  override def resolveProperty(`type`: Type, context: ModelConverterContext, annotations: Array[Annotation], chain: util.Iterator[ModelConverter]): Property = {
    val source = currentMirror.runtimeClass(typeOf[Source])

    val propType = Json.mapper().constructType(`type`)

    if (propType.getRawClass.isAssignableFrom(source)) {
      val prop = as

      return prop
    }

    super.resolveProperty(`type`, context, annotations, chain)
  }
}
