//
// Copyright (c) 2011-2017 by Curalate, Inc.
//

package io.paradoxical.finatra.security.cors

import com.twitter.finagle.http.filter.Cors
import scala.util.matching.Regex

object WildcardCorsPolicy {
  def apply(wildcards: String*): Cors.Policy = {
    RegexCorsPolicy(
      wildcards.map(
        _.replace(".", "\\.").
          replace("*", ".*").
          replace("?", ".")
      ).reduce(_ + "|" + _).r
    )
  }
}
object RegexCorsPolicy {
  /**
   * Applies a regex to domains to determine if they are allowable for cross origin requests
   *
   * @param regex
   * @return
   */
  def apply(regex: Regex): Cors.Policy = {
    def originAllowed(origin: String): Option[String] = {
      regex.findFirstIn(origin).map(_ => origin)
    }

    Cors.UnsafePermissivePolicy.copy(
      allowsOrigin = originAllowed
    )
  }
}