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
import scala.util.control.NoStackTrace

class PropagatingExecutionServiceTests extends FlatSpec with Matchers {
  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  "PropagatingExecutionContext" should "propagate Twitter Locals with the PropagatingExecutionContext" in {
    val pool = TwitterExecutionContextProvider.of(Executors.newFixedThreadPool(1))

    val local = new Local[Int]
    local.update(1)

    val fut = pool.submit(new Callable[Unit] {
      override def call() = logger.debug(s"PropagatingExecutionContext: local() == ${local()}")

      local() should equal(Some(1))
    }).toScalaFuture()

    Await.result(fut, 10 seconds)
  }
}

class PropagatingExecutionContextSpec extends FlatSpec with Matchers {
  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  "PropagatingExecutionContext" should "propagate Twitter Locals with the PropagatingExecutionContext" in {
    val local = new Local[Int]
    local.update(1)

    val fut = Future {
      logger.debug(s"PropagatingExecutionContext: local() == ${local()}")
      local() should equal(Some(1))
    }

    Await.ready(fut, 10 seconds)
  }

  it should "not work with the default execution context" in {
    import scala.concurrent.ExecutionContext.Implicits.global

    val local = new Local[Int]
    local.update(1)

    val fut = Future {
      logger.debug(s"Scala ExecutionContext: local() == ${local()}")
      local() should not equal Some(1)
    }

    Await.ready(fut, 10 seconds)
  }

  ignore should "successfully run a ton of Futures" in {

    val futs = (0 to 1000).toList.map { i =>
      Future {
        blocking(Thread.sleep(100))
        i
      }
    }

    val allFuts = Await.result(Future.sequence(futs), 10 seconds)

    allFuts.sum should equal((0 to 1000).sum)
  }

  it should "handle throwables similar to Scala's default execution context" in {
    testThrowableHandlerSet("Scala", scala.concurrent.ExecutionContext.Implicits.global)
    testThrowableHandlerSet("Propagating", TwitterExecutionContextProvider.Implicits.global)
  }

  private def testThrowableHandlerSet(msgPostfix: String, executionContext: ExecutionContext): Unit = {
    val duration = 50 millis

    // The tests that indicate TimeoutException are because they are Fatal exceptions (e.g. not scala.util.control.NonFatal)
    //scalastyle:off
    a[RuntimeException] should be thrownBy {
      Await.result(createFutureResult(
        s"RuntimeException$msgPostfix",
        new RuntimeException("1") with NoStackTrace
      )(executionContext), duration)
    }

    a[Throwable] should be thrownBy {
      Await.result(createFutureResult(
        s"Throwable$msgPostfix",
        new Throwable("2") with NoStackTrace
      )(executionContext), duration)
    }

    a[TimeoutException] should be thrownBy {
      Await.result(createFutureResult(
        s"LinkageError$msgPostfix",
        new LinkageError("3") with NoStackTrace
      )(executionContext), duration)
    }

    a[TimeoutException] should be thrownBy {
      Await.result(createFutureResult(
        s"NoClassDefFoundError$msgPostfix",
        new NoClassDefFoundError("4") with NoStackTrace
      )(executionContext), duration)
    }
    //scalastyle:on

    // Now test the blocking versions
    //scalastyle:off
    a[RuntimeException] should be thrownBy {
      Await.result(createFutureResult(
        s"RuntimeExceptionBlocking$msgPostfix",
        new RuntimeException("5") with NoStackTrace,
        shouldBlock = true
      )(executionContext), duration)
    }

    a[Throwable] should be thrownBy {
      Await.result(createFutureResult(
        s"ThrowableBlocking$msgPostfix",
        new Throwable("6") with NoStackTrace,
        shouldBlock = true
      )(executionContext), duration)
    }

    a[TimeoutException] should be thrownBy {
      Await.result(createFutureResult(
        s"LinkageErrorBlocking$msgPostfix",
        new LinkageError("7") with NoStackTrace,
        shouldBlock = true
      )(executionContext), duration)
    }

    a[TimeoutException] should be thrownBy {
      Await.result(createFutureResult(
        s"NoClassDefFoundErrorBlocking$msgPostfix",
        new NoClassDefFoundError("8") with NoStackTrace,
        shouldBlock = true
      )(executionContext), duration)
    }
    //scalastyle:on
  }

  private def createFutureResult(msg: String, t: Throwable, shouldBlock: Boolean = false)(implicit execctx: ExecutionContext): Future[Boolean] = {
    def func = {
      logger.debug(s"Running $msg on thread ${Thread.currentThread().getName}")

      throw t
    }

    Future {if (shouldBlock) blocking(func) else func}(execctx)
  }
}
