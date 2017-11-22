package io.paradoxical.finatra.swagger

import io.swagger.converter.{ModelConverter, ModelConverterContext}
import io.swagger.models.Model
import io.swagger.models.properties.Property
import java.lang.annotation.Annotation
import java.lang.reflect.{ParameterizedType, Type}
import java.util

class SimpleResolver extends ModelConverter {
  override def resolveProperty(`type`: Type, context: ModelConverterContext, annotations: Array[Annotation], chain: util.Iterator[ModelConverter]): Property = {
    if (chain.hasNext) {
      chain.next().resolveProperty(`type`, context, annotations, chain)
    }
    else {
      null
    }
  }

  override def resolve(`type`: Type, context: ModelConverterContext, chain: util.Iterator[ModelConverter]): Model = {
    chain.next().resolve(`type`,context, chain)
  }
}

object Resolvers {
  class ParameterzedTypeOption extends SimpleResolver {
    override def resolveProperty(`type`: Type, context: ModelConverterContext, annotations: Array[Annotation], chain: util.Iterator[ModelConverter]): Property = {
      `type` match {
        case parameterizedType: ParameterizedType if parameterizedType.getRawType == classOf[Option[_]] =>
          val prop = chain.next().resolveProperty(parameterizedType.getActualTypeArguments.head, context, annotations, chain)

          prop.setRequired(false)

          return prop
        case _ =>
      }

      super.resolveProperty(`type`, context, annotations, chain)
    }
  }
}
