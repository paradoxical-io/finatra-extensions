package io.paradoxical.finatra.modules

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import io.paradoxical.finatra.execution.{ProvidedExecutionContext, TwitterExecutionContextProvider}
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

class ExecutionContextModule(implicit executionContext: ExecutionContext) extends TwitterModule {
  @Provides
  @Singleton
  def providedContext(): ProvidedExecutionContext = {
    TwitterExecutionContextProvider.of(executionContext)
  }

  @Provides
  @Singleton
  def standardContext(provided: ProvidedExecutionContext): ExecutionContext = {
    provided
  }
}

object Defaults {
  def apply()(implicit executionContext: ExecutionContext) = {
    List(new ExecutionContextModule())
  }
}
