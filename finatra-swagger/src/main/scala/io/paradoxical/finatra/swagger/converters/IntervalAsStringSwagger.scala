package io.paradoxical.finatra.swagger.converters

import io.swagger.models.properties.StringProperty
import org.joda.time.Interval

/**
 * A mapper that converts intervals to strings
 */
class IntervalAsStringSwagger extends SimpleSwaggerMapper[Interval] {
  override protected def as = new StringProperty
}
