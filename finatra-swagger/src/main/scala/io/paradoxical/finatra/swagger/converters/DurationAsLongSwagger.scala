package io.paradoxical.finatra.swagger.converters

import io.swagger.models.properties.LongProperty
import scala.concurrent.duration.Duration

/**
 * A mapper that converts durations to longs
 */
class DurationAsLongSwagger extends SimpleSwaggerMapper[Duration] {
  override protected def as = new LongProperty
}
