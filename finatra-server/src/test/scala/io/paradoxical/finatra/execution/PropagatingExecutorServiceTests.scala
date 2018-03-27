//
// PropagatingExecutionContextSpec.scala
//
// Copyright (c) 2016 by Curalate, Inc.
//

package io.paradoxical.finatra.execution

import com.twitter.util.Local
import io.paradoxical.common.extensions.Extensions._
import java.util.concurrent.{Callable, Executors}
import org.scalatest.{FlatSpec, Matchers}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import scala.language.postfixOps

class PropagatingExecutorServiceTests extends FlatSpec with Matchers {
  val provider = TwitterExecutionContextProvider

  "PropagatingExecutionContext" should "propagate Twitter Locals with an executor" in {
    val pool = provider.of(Executors.newFixedThreadPool(1))

    val local = new Local[Int]
    local.update(1)

    val fut = pool.submit(new Callable[Boolean] {
      override def call(): Boolean = {
        println(s"PropagatingExecutionContext: local() == ${local()}")

        local().contains(1)
      }
    }).toScalaFuture()

    Await.result(fut, 10 seconds) shouldEqual true
  }

  "Scheduled service" should "propagate" in {
    val pool = provider.of(Executors.newSingleThreadScheduledExecutor())

    val local = new Local[Int]
    local.update(1)

    val fut = pool.submit(new Callable[Boolean] {
      override def call(): Boolean = {
        println(s"PropagatingExecutionContext: local() == ${local()}")

        local().contains(1)
      }
    }).toScalaFuture()

    Await.result(fut, 10 seconds) shouldEqual true
  }
}


