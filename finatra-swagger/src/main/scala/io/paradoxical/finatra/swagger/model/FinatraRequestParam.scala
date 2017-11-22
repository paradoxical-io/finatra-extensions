package io.paradoxical.finatra.swagger.model

import scala.reflect.runtime.universe._

import java.lang.annotation.Annotation

sealed trait ModelParam {
  val name: String
  val description: String
  val required: Boolean
  val typ: java.lang.reflect.Type
}

sealed trait FinatraRequestParam

case class RouteRequestParam(
  name: String,
  typ: java.lang.reflect.Type,
  symbol: Symbol,
  description: String = "",
  required: Boolean = true,
  parameterizedType: Option[java.lang.reflect.Type] = None,
  annotations: List[Annotation] = Nil
) extends FinatraRequestParam with ModelParam

case class QueryRequestParam(
  name: String,
  typ: java.lang.reflect.Type,
  symbol: Symbol,
  description: String = "",
  required: Boolean = true,
  parameterizedType: Option[java.lang.reflect.Type] = None,
  annotations: List[Annotation] = Nil,
  defaultValue: Option[Any] = None
) extends FinatraRequestParam with ModelParam

case class BodyRequestParam(
  name: String,
  symbol: Symbol,
  typ: java.lang.reflect.Type,
  description: String = "",
  parameterizedType: Option[java.lang.reflect.Type] = None,
  annotations: List[Annotation] = Nil
) extends FinatraRequestParam

case class RequestInjectRequestParam(name: String) extends FinatraRequestParam

case class HeaderRequestParam(
  name: String,
  symbol: Symbol,
  typ: java.lang.reflect.Type,
  required: Boolean = true,
  description: String = "",
  parameterizedType: Option[java.lang.reflect.Type] = None
) extends FinatraRequestParam with ModelParam

case class FormRequestParam(
  name: String,
  symbol: Symbol,
  description: String = "",
  required: Boolean = true,
  typ: java.lang.reflect.Type,
  parameterizedType: Option[java.lang.reflect.Type] = None
) extends FinatraRequestParam with ModelParam

