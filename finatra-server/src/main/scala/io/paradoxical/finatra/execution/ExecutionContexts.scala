package io.paradoxical.finatra.execution

import com.twitter.util.Local
import java.util
import java.util.concurrent._
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}

/**
 * Marker interfaces to provide contexts with custom logic. This
 * forces users to make sure to use the execution context providers that support request tracing
 * and maybe other tooling
 */
trait ProvidedExecutionContext extends ExecutionContext

trait ProvidedExecutorService extends ExecutorService {
  def asExecutionContext: ExecutionContextExecutorService = {
    ExecutionContext.fromExecutorService(this)
  }
}

trait ProvidedSchedulerService extends ScheduledExecutorService {
  def asExecutionContext: ExecutionContextExecutorService = {
    ExecutionContext.fromExecutorService(this)
  }
}

object TwitterExecutionContextProvider  {
  object Implicits {
    val global = of(scala.concurrent.ExecutionContext.Implicits.global)
  }

  /**
   * Safely wrap any execution context into one that properly passes context
   *
   * @param executionContext
   * @return
   */
   def of(executionContext: ExecutionContext) = new PropagatingExecutionContextWrapper(executionContext)

  /**
   * Wrap an executor service
   *
   * @param executorService
   * @return
   */
   def of(executorService: ExecutorService) = new PropagatedExecutorService(executorService)

  /**
   * Wrap a scheduled executor
   *
   * @param scheduledExecutorService
   * @return
   */
   def of(scheduledExecutorService: ScheduledExecutorService) = new PropagatedScheduledExecutor(scheduledExecutorService)
}

trait TwitterLocalExecutionContext extends ProvidedExecutionContext {
  self =>

  override def prepare(): ExecutionContext = new ExecutionContext {
    // Save the call-site state
    private val context = Local.save()

    def execute(r: Runnable): Unit = self.execute(new Runnable {
      def run(): Unit = {
        Local.let(context) {
          r.run()
        }
      }
    })

    def reportFailure(t: Throwable): Unit = self.reportFailure(t)
  }
}

trait TwitterLocalExecutionService extends ProvidedExecutorService {
  protected val wrapped: ExecutorService

  override def submit[T](task: Callable[T]): Future[T] = {
    wrapped.submit(newCallable(task))
  }

  override def submit[T](task: Runnable, result: T): Future[T] = {
    wrapped.submit(newRunnable(task), result)
  }

  override def submit(task: Runnable): Future[_] = {
    wrapped.submit(newRunnable(task))
  }

  protected def newCallable[T](callable: Callable[T]): Callable[T] = {
    new Callable[T] {
      private val context = Local.save()

      override def call(): T = {
        Local.let(context) {
          callable.call()
        }
      }
    }
  }

  protected def newRunnable(runnable: Runnable): Runnable = {
    new Runnable {
      // Save the call-site state
      private val context = Local.save()

      override def run() = Local.let(context) {
        runnable.run()
      }
    }
  }
}

/**
 * Wrap an executor service
 *
 * @param wrapped
 */
class PropagatedExecutorService(protected val wrapped: ExecutorService) extends TwitterLocalExecutionService {
  override def shutdown(): Unit = {
    wrapped.shutdown()
  }

  override def isTerminated: Boolean = {
    wrapped.isTerminated
  }

  override def awaitTermination(timeout: Long, unit: TimeUnit): Boolean = {
    wrapped.awaitTermination(timeout, unit)
  }

  override def shutdownNow(): util.List[Runnable] = {
    wrapped.shutdownNow()
  }

  override def invokeAll[T](tasks: util.Collection[_ <: Callable[T]]): util.List[Future[T]] = {
    wrapped.invokeAll(tasks.asScala.map(newCallable).asJavaCollection)
  }

  override def invokeAll[T](tasks: util.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit): util.List[Future[T]] = {
    wrapped.invokeAll(tasks.asScala.map(newCallable).asJavaCollection, timeout, unit)
  }

  override def invokeAny[T](tasks: util.Collection[_ <: Callable[T]]): T = {
    wrapped.invokeAny(tasks.asScala.map(newCallable).asJavaCollection)
  }

  override def invokeAny[T](tasks: util.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit): T = {
    wrapped.invokeAny(tasks.asScala.map(newCallable).asJavaCollection, timeout, unit)
  }

  override def isShutdown: Boolean = {
    wrapped.isShutdown
  }

  override def execute(command: Runnable): Unit = {
    wrapped.execute(newRunnable(command))
  }
}

/**
 * Wrapper around an existing ExecutionContext that makes it propagate MDC information.
 */
class PropagatingExecutionContextWrapper(wrapped: ExecutionContext)
  extends ExecutionContext with TwitterLocalExecutionContext {

  override def execute(r: Runnable): Unit = wrapped.execute(r)

  override def reportFailure(t: Throwable): Unit = wrapped.reportFailure(t)
}

/**
 * Wrapper around scheduled executor
 *
 * @param wrapped
 */
class PropagatedScheduledExecutor(wrapped: ScheduledExecutorService) extends PropagatedExecutorService(wrapped) with ProvidedSchedulerService {
  override def scheduleAtFixedRate(command: Runnable, initialDelay: Long, period: Long, unit: TimeUnit): ScheduledFuture[_] = {
    wrapped.scheduleAtFixedRate(newRunnable(command), initialDelay, period, unit)
  }

  override def asExecutionContext: ExecutionContextExecutorService = {
    super.asExecutionContext
  }

  override def schedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture[_] = {
    wrapped.schedule(newRunnable(command), delay, unit)
  }

  override def schedule[V](callable: Callable[V], delay: Long, unit: TimeUnit): ScheduledFuture[V] = {
    wrapped.schedule(newCallable(callable), delay, unit)
  }

  override def scheduleWithFixedDelay(command: Runnable, initialDelay: Long, delay: Long, unit: TimeUnit): ScheduledFuture[_] = {
    wrapped.scheduleWithFixedDelay(newRunnable(command), initialDelay, delay, unit)
  }
}
