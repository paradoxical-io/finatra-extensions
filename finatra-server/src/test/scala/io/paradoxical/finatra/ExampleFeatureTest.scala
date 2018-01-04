package io.paradoxical.finatra

import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.EmbeddedHttpServer
import io.paradoxical.finatra.sample.SampleServer
import org.scalatest.{FlatSpec, Matchers}

class ExampleFeatureTest extends FlatSpec with Matchers {

  val server = new EmbeddedHttpServer(new SampleServer)

  "server" should "ping" in {
    server.httpGet(
      path = "/ping/test",
      andExpect = Ok,
    ).contentString shouldEqual """{"data":"test"}"""
  }
}
