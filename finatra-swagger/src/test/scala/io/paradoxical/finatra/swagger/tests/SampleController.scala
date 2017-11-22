package io.paradoxical.finatra.swagger.tests

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.http.Controller
import com.twitter.finatra.request.{QueryParam, RouteParam}
import com.twitter.util.Future
import io.paradoxical.finatra.swagger.SwaggerSupport
import io.paradoxical.finatra.swagger.annotations.ApiData
import java.util.Date
import org.joda.time.{DateTime, LocalDate}

class SampleFilter extends SimpleFilter[Request, Response] {
  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    service(request)
  }
}

case class Param[T, Y](data: T, foo: Y)

case class TestList(
  @RouteParam r: String,

  @ApiData(description = "foo")
  @QueryParam q: String = "some default val",
  data: List[Address],
  oData: Option[Address],
  aData: Address,
  @QueryParam
  boolean: Boolean = false
)

class SampleController extends Controller with SwaggerSupport {
  override implicit protected val swagger = SampleSwagger

  case class HelloResponse(text: String, time: Date)

  val student = Student("Tom", Some("Wang"), Gender.Male, new LocalDate(), 4, Some(Address("California Street", "94111")))

  postWithDoc("/list/") { o =>
    o.bodyParam[List[Param[TestList, TestList]]]("body")
  } { request: ListTasksRequest =>
  }

  postWithDoc("/simpleTestList/") { o =>
    o.request[TestList]
  } { request: ListTasksRequest =>

  }

  postWithDoc("/param/") { o =>
    o.request[Param[TestList, Param[String, String]]]
  } { request: Request =>

  }

  postWithDoc("/students2/:id") { o =>
    o.summary("Sample request with route")
      .description("Read the detail information about the student.")
      .tag("Student")
      .request[ListTasksRequest]
  } { request: ListTasksRequest =>
    val id = request.id

    response.ok.json(student).toFuture
  }

  postWithDoc("/students3/") { o =>
    o.summary("Sample request with route")
      .description("Read the detail information about the student.")
      .tag("Student")
      .responseWith[Unit](204, "No content")
  } { _: Request =>

  }

  postWithDoc("/students/:id") { o =>
    o.summary("Sample request with route")
      .description("Read the detail information about the student.")
      .tag("Student")
      .request[StudentWithRoute]
  } { request: StudentWithRoute =>
    val id = request.id

    response.ok.json(student).toFuture
  }

  postWithDoc("/students/bulk") { o =>
    o.summary("Create a list of students")
      .tag("Student")
      .bodyParam[List[Student]]("students", "the list of students")
      .responseWith[Unit](200, "the students are created")
      .responseWith[Unit](500, "internal error")
  } { students: List[Student] =>
    response.ok.json(students).toFuture
  }

  getWithDoc("/students/:id") { o =>
    o.summary("Read student information")
      .description("Read the detail information about the student.")
      .tag("Student")
      .routeParam[String]("id", "the student id")
      .produces("application/json")
      .responseWith[Student](200, "the student object", example = Some(student))
      .responseWith[Unit](404, "the student is not found")
  } { request: Request =>
    val id = request.getParam("id")

    response.ok.json(student).toFuture
  }

  postWithDoc("/students/test/:id") { o =>
    o.summary("Sample request with route2")
      .description("Read the detail information about the student.")
      .tag("Student")
      .request[StudentWithRoute]
  } { request: StudentWithRoute =>
    val id = request.id

    response.ok.json(student).toFuture
  }

  postWithDoc("/students/firstName") {
    _.request[StringWithRequest]
      .tag("Student")
  } { request: StringWithRequest =>
    request.firstName
  }

  postWithDoc("/students") { o =>
    o.summary("Create a new student")
      .tag("Student")
      .bodyParam[Student]("student", "the student details")
      .responseWith[Unit](200, "the student is created")
      .responseWith[Unit](500, "internal error")
  } { student: Student =>
    //val student = request.contentString
    response.ok.json(student).toFuture
  }

  putWithDoc("/students/:id") { o =>
    o.summary("Update the student")
      .tag("Student")
      .formParam[String]("name", "the student name")
      .formParam[Int]("grade", "the student grade")
      .routeParam[String]("id", "student ID")
      .cookieParam[String]("who", "who make the update")
      .headerParam[String]("token", "the token")
      .responseWith[Unit](200, "the student is updated")
      .responseWith[Unit](404, "the student is not found")
  } { request: Request =>
    val id = request.getParam("id")
    val name = request.getParam("name")
    val grade = request.getIntParam("grade")
    val who = request.cookies.getOrElse("who", "Sam")
    val token = request.headerMap("token")

    response.ok.toFuture
  }

  getWithDoc("/students") { o =>
    o.summary("Get a list of students")
      .tag("Student")
      .responseWith[Array[String]](200, "the student ids")
      .responseWith[Unit](500, "internal error")
      .addSecurity("sampleBasic", List())
  } { request: Request =>
    response.ok.json(Array("student1", "student2")).toFuture
  }

  getWithDoc("/courses") { o =>
    o.summary("Get a list of courses")
      .tag("Course")
      .responseWith[Array[String]](200, "the courses ids")
      .responseWith[Unit](500, "internal error")
  } { request: Request =>
    response.ok.json(Array("course1", "course2")).toFuture
  }

  getWithDoc("/courses/:id") { o =>
    o.summary("Get the detail of a course")
      .tag("Course")
      .routeParam[String]("id", "the course id")
      .responseWith[Course](200, "the courses detail")
      .responseWith[Unit](500, "internal error")
  } { request: Request =>
    response.ok.json(Course(new DateTime(), "calculation", Seq("math"), CourseType.LAB, 20, BigDecimal(300.54))).toFuture
  }

  filter[SampleFilter].getWithDoc("/courses/:courseId/student/:studentId") { o =>
    o.summary("Is the student in this course")
      .tags(List("Course", "Student"))
      .routeParam[Gender]("gender", "the gender")
      .routeParam[String]("studentId", "the student id")
      .responseWith[Boolean](200, "true / false")
      .responseWith[Unit](500, "internal error")
      .deprecated(true)
  } { request: Request =>
    response.ok.json(true).toFuture
  }


}
