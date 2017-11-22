package io.paradoxical.finatra.swagger

import com.twitter.inject.domain.WrappedValue
import io.paradoxical.global.tiny._
import io.swagger.converter.{ModelConverter, ModelConverterContext}
import io.swagger.models.properties._
import io.swagger.util.Json
import java.lang.annotation.Annotation
import java.lang.reflect.Type
import java.util

/**
 * Swagger support finatra wrapped classes
 */
class WrappedValueModelResolver extends SimpleResolver {
  override def resolveProperty(
    typ: Type,
    context: ModelConverterContext,
    annotations: Array[Annotation],
    next: util.Iterator[ModelConverter]): Property = {
    val propType = Json.mapper().constructType(typ)

    val wrapped = propType.findSuperType(classOf[WrappedValue[_]])
    if (wrapped != null) {
      val p = next.next().resolveProperty(wrapped.containedType(0), context, annotations, next)
      p.setRequired(true)
      return p
    }

    next.next().resolveProperty(propType, context, annotations, next)
  }
}

/**
 * Swagger support tiny wrappers
 */
class ParadoxicalWrappedValueModelResolver extends SimpleResolver {
  override def resolveProperty(
    typ: Type,
    context: ModelConverterContext,
    annotations: Array[Annotation],
    next: util.Iterator[ModelConverter]): Property = {
    val propType = Json.mapper().constructType(typ)

    val wrapped = propType.findSuperType(classOf[ValueType[_]])
    if (wrapped != null) {
      val property = {
        if (propType.isTypeOrSubTypeOf(classOf[StringValue])) {
          new StringProperty()
        }
        else if (propType.isTypeOrSubTypeOf(classOf[IntValue])) {
          new IntegerProperty()
        }
        else if (propType.isTypeOrSubTypeOf(classOf[LongValue])) {
          new LongProperty()
        }
        else if (propType.isTypeOrSubTypeOf(classOf[UuidValue])) {
          new UUIDProperty()
        }
        else if (propType.isTypeOrSubTypeOf(classOf[FloatValue])) {
          new FloatProperty()
        }
        else if (propType.isTypeOrSubTypeOf(classOf[DoubleValue])) {
          new DoubleProperty()
        }
        else {
          next.next().resolveProperty(wrapped.containedType(0), context, annotations, next)
        }
      }

      property.setRequired(true)

      return property
    }

    next.next().resolveProperty(propType, context, annotations, next)
  }
}
