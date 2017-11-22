//
// Copyright (c) 2011-2017 by Curalate, Inc.
//

package io.paradoxical.finatra.logging

import com.twitter.finagle.http.filter.LogFormatter
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.{Duration, Time}
import java.util.TimeZone
import org.apache.commons.lang.time.FastDateFormat

// Lifted from com.twitter.finagle.http.filter.CommonLogFormatter
class XForwardedForLogFormatter extends LogFormatter {
  /* See http://httpd.apache.org/docs/2.0/logs.html
 *
 * Apache common log format is: "%h %l %u %t \"%r\" %>s %b"
 *   %h: remote host
 *   %l: remote logname
 *   %u: remote user
 *   %t: time request was received
 *   %r: request time
 *   %s: status
 *   %b: bytes
 *
 * We add:
 *   %D: response time in milliseconds
 *   "%{User-Agent}i": user agent
 */
  val DateFormat = FastDateFormat.getInstance("dd/MMM/yyyy:HH:mm:ss Z",
    TimeZone.getTimeZone("GMT"))

  def format(request: Request, response: Response, responseTime: Duration): String = {
    val remoteAddr = request.headerMap.get("X-Forwarded-For").getOrElse(request.remoteAddress.getHostAddress)

    val contentLength = response.length
    val contentLengthStr = if (contentLength > 0) contentLength.toString else "-"

    val uaStr = request.userAgent.getOrElse("-")

    val builder = new StringBuilder
    builder.append(remoteAddr)
    builder.append(" - - [")
    builder.append(formattedDate)
    builder.append("] \"")
    builder.append(escape(request.method.toString))
    builder.append(' ')
    builder.append(escape(request.uri))
    builder.append(' ')
    builder.append(escape(request.version.toString))
    builder.append("\" ")
    builder.append(response.statusCode.toString)
    builder.append(' ')
    builder.append(contentLengthStr)
    builder.append(' ')
    builder.append(responseTime.inMillis)
    builder.append(" \"")
    builder.append(escape(uaStr))
    builder.append('"')

    builder.toString
  }

  def formatException(request: Request, throwable: Throwable, responseTime: Duration): String = throw new UnsupportedOperationException("Log throwables as empty 500s instead")

  def formattedDate: String = DateFormat.format(Time.now.toDate)
}
