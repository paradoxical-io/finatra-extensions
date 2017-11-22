package io.paradoxical.finatra.swagger

import io.paradoxical.finatra.swagger.model._
import io.paradoxical.finatra.swagger.annotations.ApiData
import com.google.inject.{Inject => GInject}
import com.twitter.finagle.http.Request
import com.twitter.finatra.request.{FormParam, QueryParam, RouteParam, Header => HeaderParam}
import io.swagger.converter.ModelConverters
import io.swagger.models._
import io.swagger.models.parameters._
import io.swagger.models.properties.Property
import java.lang.annotation.Annotation
import java.lang.reflect
import java.lang.reflect.{Field, ParameterizedType}
import javax.inject.{Inject => JInject}
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.`type`.TypeDescription
import net.bytebuddy.description.modifier.Visibility
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.reflect.internal.{Definitions, StdNames, SymbolTable}
import scala.reflect.runtime._
import scala.reflect.runtime.universe._
import scala.util.{Failure, Success}
import sun.reflect.generics.reflectiveObjects.TypeVariableImpl

object FinatraSwagger {
  private val finatraRouteParamter = ":(\\w+)".r

  /**
   * Cache of dynamically generated class bodies keyed by qualified names
   */
  private val dynamicClassBodies: mutable.HashMap[String, Class[_]] = new mutable.HashMap[String, Class[_]]()

  implicit def convertToFinatraSwagger(swagger: Swagger): FinatraSwagger = new FinatraSwagger(swagger)
}

class FinatraSwagger(swagger: Swagger) {

  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  import FinatraSwagger._

  /**
   * Register a request object that contains body information/route information/etc
   *
   * @tparam T
   * @return
   */
  def register[T: TypeTag]: List[Parameter] = {
    val properties = getFinatraProps[T]

    val className = currentMirror.runtimeClass(typeOf[T]).getName

    val swaggerProps =
      properties.collect {
        case x: ModelParam => x
      }.map {
        case param @ (_: RouteRequestParam) =>
          new PathParameter().
            name(param.name).
            description(param.description).
            required(param.required).
            property(registerModel(param.typ, param.parameterizedType.getOrElse(param.typ)))
        case param @ (_: QueryRequestParam) =>
          val q =
            new QueryParameter().
              name(param.name).
              description(param.description).
              required(param.required).
              property(registerModel(param.typ, param.parameterizedType.getOrElse(param.typ)))

          q.setDefault(param.defaultValue.orNull)

          q
        case param @ (_: HeaderRequestParam) =>
          new HeaderParameter().
            name(param.name).
            description(param.description).
            required(param.required).
            property(registerModel(param.typ, param.parameterizedType.getOrElse(param.typ)))
        case param @ (_: FormRequestParam) =>
          new FormParameter().
            name(param.name).
            description(param.description).
            required(param.required).
            property(registerModel(param.typ, param.parameterizedType.getOrElse(param.typ)))
      }

    val bodyElements = properties.collect { case b: BodyRequestParam => b }

    swaggerProps ++ List(registerDynamicBody(bodyElements, className)).flatten
  }

  /**
   * Checks if a field is required and unboxes the inner type if its option
   *
   * @param field
   * @return (Isrequired, Option[InnerValueType])
   */
  private def isFieldRequired(field: Field): (Boolean, Option[reflect.Type]) = {
    field.getGenericType match {
      case parameterizedType: ParameterizedType if parameterizedType.getRawType.asInstanceOf[Class[_]] == classOf[Option[_]] =>

        (false, Some(parameterizedType.getActualTypeArguments.apply(0)))
      case parameterizedType: ParameterizedType if parameterizedType.getRawType.asInstanceOf[Class[_]] != classOf[Option[_]] =>
        (true, Some(parameterizedType.getActualTypeArguments.apply(0)))
      case _ =>
        (true, None)
    }
  }

  /**
   * Given the request object format its finatra parameters via reflection
   *
   * @tparam T
   * @return
   */
  private def getFinatraProps[T: TypeTag]: List[FinatraRequestParam] = {
    // get runtime mirror
    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

    val ds = universe.asInstanceOf[Definitions with SymbolTable with StdNames]

    val t = typeOf[T]

    val clazz = currentMirror.runtimeClass(typeOf[T])

    val fields = clazz.getDeclaredFields

    val constructorArgWithField =
      clazz.
        getConstructors.
        head.getParameters.
        map(m => (clazz: Class[_ <: Annotation]) => {
          val annotation = m.getAnnotationsByType(clazz)

          if (annotation.isEmpty) {
            None
          } else {
            Some(annotation)
          }
        }).
        zip(fields)

    val ast: List[Option[FinatraRequestParam]] =
      constructorArgWithField.map { case (annotationExtractor, field) =>
        val routeParam = annotationExtractor(classOf[RouteParam])
        val queryParam = annotationExtractor(classOf[QueryParam])
        val injectJavax = annotationExtractor(classOf[JInject])
        val injectGuice = annotationExtractor(classOf[GInject])
        val header = annotationExtractor(classOf[HeaderParam])
        val form = annotationExtractor(classOf[FormParam])

        val paramList = t.typeSymbol.asClass.primaryConstructor.typeSignature.paramLists.head

        val symbol = paramList.find(_.name.decodedName.toString == field.getName).get

        val description = annotationExtractor(classOf[ApiData]).flatMap(_.headOption).map(_.asInstanceOf[ApiData].description()).getOrElse("")

        // if the type is a generic erased T from java, then use the scala symbol
        // api to find out what is the actual type of the resolved generic
        val fieldType = if (field.getGenericType.isInstanceOf[TypeVariableImpl[_]]) {
          val fieldTypeSignature = t.decls.filter(_.name == symbol.name).head.typeSignatureIn(t).finalResultType

          currentMirror.runtimeClass(fieldTypeSignature)
        } else {
          // otherwise return the regular type
          field.getGenericType
        }

        // special checking if the parameterized type is an option field.
        // body parameters already properly marked optional based on their type
        // but query/route/header/etc paramteres need to be explicitly told
        val (isRequired, parameterizedType) = isFieldRequired(field)

        if (routeParam.isDefined) {
          Some(RouteRequestParam(
            field.getName,
            typ = fieldType,
            symbol = symbol,
            description = description,
            parameterizedType = parameterizedType,
            annotations = field.getAnnotations.toList
          ))
        }
        else if (queryParam.isDefined) {
          // if the current symbol has a default param find
          // its default param knowing what the synthetic method is on
          // the companion type and invoking the synthetic method's
          // getter method
          val defaultValue = scala.util.Try {
            if (symbol.asTerm.isParamWithDefault) {

              // for some reason default symbols are 1 indexed...?
              val symbolPosition = paramList.zipWithIndex.find(_._1 == symbol).get._2 + 1

              val companionMethod = t.companion.members.find(_.name.decodedName.toString == "apply$default$" + symbolPosition)

              val module = t.typeSymbol.companion.asModule

              val companion = runtimeMirror.reflectModule(module).instance

              val result = runtimeMirror.reflect(companion).reflectMethod(companionMethod.get.asMethod)()

              Some(result)
            } else {
              None
            }
          } match {
            case Success(x) => x
            case Failure(_) => None
          }

          Some(QueryRequestParam(
            field.getName,
            typ = fieldType,
            symbol = symbol,
            required = isRequired,
            description = description,
            parameterizedType = parameterizedType,
            annotations = field.getAnnotations.toList,
            defaultValue = defaultValue
          ))
        }
        else if ((injectJavax.isDefined || injectGuice.isDefined) && field.getType.isAssignableFrom(classOf[Request])) {
          Some(RequestInjectRequestParam(field.getName))
        }
        else if (header.isDefined) {
          Some(HeaderRequestParam(
            field.getName,
            typ = fieldType,
            required = isRequired,
            symbol = symbol,
            parameterizedType = parameterizedType
          ))
        }
        else if (form.isDefined) {
          Some(FormRequestParam(
            symbol = symbol,
            name = field.getName,
            typ = fieldType,
            required = isRequired,
            parameterizedType = parameterizedType
          ))
        }
        else {
          Some(BodyRequestParam(
            symbol = symbol,
            name = field.getName,
            typ = fieldType,
            parameterizedType = parameterizedType,
            annotations = field.getAnnotations.toList
          ))
        }
      }.toList

    ast.flatten
  }

  private def emitBodyClassForElements(bodyElements: List[BodyRequestParam], className: String, suffix: Int = 0): Option[Class[_]] = {
    try {
      if (suffix > 500) {
        logger.debug("Unable to create instance of body, skipping!")

        return None
      }

      val byteBuddy = new ByteBuddy()

      // add "Body" to avoid name collisions
      val bodyEmittedClass = byteBuddy.subclass(classOf[Object]).name(className + (if (suffix == 0) "" else suffix.toString))

      val bodyFields = bodyElements.foldLeft(bodyEmittedClass) { (asm, body) =>
        val bodyType =
          body.typ match {
            case t: ParameterizedType if body.parameterizedType.isDefined =>
              TypeDescription.Generic.Builder.parameterizedType(t.getRawType.asInstanceOf[Class[_]], body.parameterizedType.get).build()
            case _ =>
              new TypeDescription.Generic.OfNonGenericType.ForLoadedType(body.typ.asInstanceOf[Class[_]])
          }

        val f =
          asm.defineField(
            body.name,
            bodyType, Visibility.PUBLIC
          )

        f.annotateField(body.annotations.asJava)
      }

      Some(bodyFields.make().load(getClass.getClassLoader).getLoaded)
    } catch {
      case e: IllegalStateException =>
        logger.debug("Unable to create instance of body!", e)

        emitBodyClassForElements(bodyElements, className, suffix + 1)
    }
  }

  /**
   * Creates a fake object for swagger to reflect upon
   *
   * @param bodyElements
   * @param name
   * @return
   */
  private def registerDynamicBody(bodyElements: List[BodyRequestParam], name: String): Option[Parameter] = {
    if (bodyElements.isEmpty) {
      return None
    }

    val className = name + "Body"

    val emitttedData = emitBodyClassForElements(bodyElements, className)

    if (emitttedData.isEmpty) {
      return None
    }

    val bodyClass = dynamicClassBodies.getOrElse(className, emitttedData.get)

    dynamicClassBodies.put(className, bodyClass)

    val schema = registerModel(bodyClass, bodyClass, Some(name))

    val model = PropertyUtil.toModel(schema)

    Some(
      {
        val bodyparam = new BodyParameter().name("body").schema(model)

        bodyparam.setRequired(true)

        bodyparam
      }
    )
  }

  /**
   * Construct a java type from a scala type
   *
   * @param paramType
   * @return
   */
  private def toJavaType(paramType: Type): java.lang.reflect.Type = {
    val typeConstructor = currentMirror.runtimeClass(paramType)

    val innerTypes = paramType.typeArgs.map(toJavaType).toArray

    if (innerTypes.isEmpty) {
      typeConstructor
    } else {
      new ParameterizedType {
        override def getRawType: reflect.Type = {
          typeConstructor
        }

        override def getActualTypeArguments: Array[reflect.Type] = {
          innerTypes
        }

        override def getOwnerType: reflect.Type = {
          null
        }
      }
    }
  }

  def registerModel[T: TypeTag](example: Option[T]): Property = {
    val paramType: Type = typeOf[T]
    if (paramType =:= TypeTag.Nothing.tpe) {
      null
    } else {
      if (paramType.typeArgs.nonEmpty) {
        try {
          val typ = toJavaType(paramType)

          registerModel(typ, typ, example)
        } catch {
          case ex: Exception =>
            // if this fails just register it best we can
            val typeClass = currentMirror.runtimeClass(paramType)

            registerModel(typeClass, typeClass, example)
        }
      } else {
        val typeClass = currentMirror.runtimeClass(paramType)

        registerModel(typeClass, typeClass, example)
      }
    }
  }

  private def registerModel(typeConstructor: java.lang.reflect.Type, typ: java.lang.reflect.Type, example: Option[_] = None, name: Option[String] = None) = {
    val modelConverters = ModelConverters.getInstance()
    val models = modelConverters.readAll(typ)
    for (entry <- models.entrySet().asScala) {
      val model = entry.getValue

      swagger.addDefinition(entry.getKey, model)
    }

    val schema = modelConverters.readAsProperty(typeConstructor)

    example.foreach(schema.setExample)

    schema
  }

  def convertPath(path: String): String = {
    FinatraSwagger.finatraRouteParamter.replaceAllIn(path, "{$1}")
  }

  def registerOperation(path: String, method: String, operation: Operation): Swagger = {
    val swaggerPath = convertPath(path)

    var spath = swagger.getPath(swaggerPath)
    if (spath == null) {
      spath = new Path()
      swagger.path(swaggerPath, spath)
    }

    spath.set(method, operation)

    swagger
  }
}
