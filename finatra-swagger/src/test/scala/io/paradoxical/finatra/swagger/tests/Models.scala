package io.paradoxical.finatra.swagger.tests

import com.twitter.finagle.http.Request
import com.twitter.finatra.request.{QueryParam, RouteParam}
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import javax.inject.Inject
import org.joda.time.{DateTime, LocalDate}

@ApiModel(value = "AddressModel", description = "Sample address model for documentation")
case class Address(street: String, zip: String)

case class Student(
  firstName: String,
  lastName: Option[String],
  gender: Gender,
  birthday: LocalDate,
  grade: Int,
  address: Option[Address]
)

case class ListTasksRequest(
  @RouteParam id: Long,
  @QueryParam start: Option[DateTime],
  @QueryParam end: Option[DateTime],
  @QueryParam argsRegex: Option[String]
)

case class StudentWithRoute(
  @RouteParam id: String,
  @Inject request: Request,
  firstName: String,
  lastName: String,

  @QueryParam
  dateTime: Option[DateTime],

  @QueryParam
  gender: Gender,

  @QueryParam
  genderOpt: Option[Gender],

  birthday: LocalDate,
  grade: Int,
  address: Option[Address],
  name: Option[String]
)

case class StringWithRequest(
  @Inject request: Request,
  firstName: String
)

object CourseType extends Enumeration {
  val LEC, LAB = Value
}

case class Course(
  time: DateTime,
  name: String,
  @ApiModelProperty(required = false, example = "[math,stem]")
  tags: Seq[String],
  @ApiModelProperty(dataType = "string", allowableValues = "LEC,LAB")
  typ: CourseType.Value,
  @ApiModelProperty(readOnly = true)
  capacity: Int,
  @ApiModelProperty(dataType = "double", required = true)
  cost: BigDecimal
)
