Finatra Extensions
===

[![Build Status](https://travis-ci.org/paradoxical-io/finatra-extensions.svg?branch=master)](https://travis-ci.org/paradoxical-io/finatra-extensions)
[![Maven Central](https://img.shields.io/maven-central/v/io.paradoxical/finatra-test_2.12.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22finatra-test_2.12t%22)


This repo is a collection of extensions and utilities to make working with Finatra easier.

Included are 

- Swagger support
- Cached asset support (for optimized local asset serving)
- X-ForwardedFor logging filter (for use with resolving from ELBs)
- Execution contexts auto wired with twitter local context tracing
- Json Module with `camelCase` support instead of `snake_case`
- Regex based cors policy

### Examples:

Create a server:

```scala
object SampleServerMain extends SampleServer

class SampleServer extends HttpServiceBase {
  override def defaultFinatraHttpPort = ":9999"

  override def documentation = new ApiDocumentationConfig {
    override val description: String = "Sample"
    override val title: String = "API"
    override val version: String = "1.0"
  }

  override def configureHttp(router: HttpRouter): Unit = {
    router
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .filter[CommonFilters]
    
    configureDocumentation(router)

    router.add[PingController]
  }
}
```

This wires up a server with swagger support at the local endpoint of:

```
/api-docs/ui
```

Controllers can add swagger information like in the following example:

```scala
class PingController extends Framework.RestApi {
  getWithDoc("/ping/:data") {
    _.description("Ping API").
      request[PingRequest].
      responseWith[PingResponse](status = 200)
  } { request: PingRequest =>
    info("ping")
    PingResponse(request.data)
  }
}

case class PingRequest(@RouteParam data: TinyTypeData)

case class PingResponse(data: TinyTypeData)

case class TinyTypeData(value: String) extends StringValue
```

The swagger block includes simplified methods to auto create request/response swagger information just from the finatra data classes.

For example `.request[T]` will automatically map route param, query param, headers, etc into the proper swagger based on the finatra annotations,
and any fields that are _not_ annotated will be created into a "body" data classes for swagger to generate from.

### NewTypes (tiny types)

Finatra includes the concept of [wrapped values](https://twitter.github.io/finatra/scaladocs/com/twitter/inject/domain/WrappedValue.html) 
however if you ever want to distribute your model you have to leak the wrapped value definition.  

This is problematic in SOA since twitter's ecosystem is not well versioned for backwards compatability.  Pulling in finagle/twitter/etc across service boundaries should be avoided.  

To that end, paradoxical has provided our own version of wrapped values which are bound in a lightweight [dependencyless API library](https://github.com/paradoxical-io/scala-global/tree/master/global/src/main/scala/io/paradoxical/global/tiny) and this finatra extensions lib automatically supports serializing and deserializing these data classes.  Notice in the above example the `TinyTypeData` class that extends `StringValue`. There is also `UUIDValue`, `LongValue`, `IntValue`, `DoubleValue` and others.


### Testing

The finatra-test library is used as a bundling mechanism for all the dependencies that finatra needs for testing. This makes it easy
to pull all the testing dependencies necessary in one fell swoop:

```
"io.paradoxical" %% "finatra-test" % <version> % "test"
```

