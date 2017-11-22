package io.paradoxical.finatra.swagger

import io.swagger.models.{ArrayModel, Model, RefModel}
import io.swagger.models.properties.{ArrayProperty, Property, RefProperty}

object PropertyUtil {
  def toModel(property: Property): Model = {
    property match {
      case null => null
      case p: RefProperty => new RefModel(p.getSimpleRef)
      case p: ArrayProperty => {
        val arrayModel = new ArrayModel()
        arrayModel.setItems(p.getItems)
        arrayModel
      }
      case _ => null
    }
  }
}
