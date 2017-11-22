package io.paradoxical.finatra.internal

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.util.Duration
import io.paradoxical.finatra.serving.CachedAssetResolver
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.duration._

private[finatra] class AssetsApiInternal extends RestApiInternal {
  protected val DEFAULT_EXPIRE_TIME: FiniteDuration = 1 day

  @Inject
  var fileResolver: CachedAssetResolver = _

  /**
   * Server an asset with cache e-tags loaded
   *
   * @param request
   * @param path
   * @return
   */
  def staticFile(request: Request, path: String): ResponseBuilder#EnrichedResponse = {
    val fileWithSlash = if (path.startsWith("/")) path else "/" + path

    eTagHash(fileWithSlash) match {
      case Some(hash) =>
        serveFile(request, fileWithSlash, hash)
      case None =>
        response.notFound(fileWithSlash + " not found")
    }
  }

  private def serveFile(request: Request, fileWithSlash: String, hash: String) = {
    if (notExpired(request, hash)) {
      response.notModified
    } else {

      fileResolver.getInputStream(fileWithSlash) map { inputStream =>
        val r = response.ok
        r.contentType(fileResolver.getContentType(fileWithSlash))
        r.body(inputStream)

        setCacheHeaders(r, hash)
        r
      } getOrElse {
        response.notFound(fileWithSlash + " not found")
      }
    }
  }

  /**
   * Server a static file or if not exists the index page
   *
   * @param request
   * @param path
   * @param index
   * @return
   */
  def staticFileOrIndex(request: Request, path: String, index: String = "index.html"): ResponseBuilder#EnrichedResponse = {
    val sanitizedPath = if (path.startsWith("/")) path else "/" + path
    val sanitizedIndex = if (index.startsWith("/")) index else "/" + index

    if (fileResolver.exists(sanitizedPath)) {
      staticFile(request, sanitizedPath)
    }
    else {
      staticFile(request, sanitizedIndex)
    }
  }

  private def notExpired(request: Request, hash: String): Boolean = {
    eTagMatches(request, hash) && notModifiedSince(request)
  }

  private def eTagHash(uri: String): Option[String] = {
    fileResolver.hashFor(uri)
  }

  private def eTagMatches(request: Request, eTagName: String): Boolean = {
    request.headerMap.get("If-None-Match") match {
      case None => false
      case Some(token) => token == eTagName
    }
  }

  private def notModifiedSince(request: Request): Boolean = {
    request.headerMap.get("If-Modified-Since").map(_.toLong) match {
      case None => false
      case Some(last) => last - System.currentTimeMillis > 0L
    }
  }

  private def setCacheHeaders(response: ResponseBuilder#EnrichedResponse, eTag: String): ResponseBuilder#EnrichedResponse = {
    response.header("ETag", eTag)
    response.expires = new Date(System.currentTimeMillis() + DEFAULT_EXPIRE_TIME.toMillis).toInstant.toEpochMilli.toString
    response.lastModified = new Date(System.currentTimeMillis() + DEFAULT_EXPIRE_TIME.toMillis).toInstant.toEpochMilli.toString
    response.cacheControl = Duration(DEFAULT_EXPIRE_TIME.toMillis, TimeUnit.MILLISECONDS)
    response
  }
}
