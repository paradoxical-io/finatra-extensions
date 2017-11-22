//
// Copyright (c) 2011-2017 by Curalate, Inc.
//

package io.paradoxical.finatra.serving

import com.google.common.cache.CacheBuilder
import com.google.common.hash.Hashing
import com.twitter.finatra.http.routing.FileResolver
import com.twitter.inject.annotations.Flag
import java.io.{ByteArrayInputStream, InputStream}
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import org.apache.commons.io.IOUtils
import scala.concurrent.duration._

case class DataHash(hash: String, data: Array[Byte])

/**
 * A cached asset reader that stores contents in memory instead of re-reading from the jar
 *
 * @param localDocRoot
 * @param docRoot
 */
class CachedAssetResolver @Inject()(
  @Flag("local.doc.root") localDocRoot: String,
  @Flag("doc.root") docRoot: String
) extends FileResolver(localDocRoot, docRoot) {

  private val cache = {
    CacheBuilder.newBuilder().
      expireAfterAccess((1 hour).toMillis, TimeUnit.MILLISECONDS).
      build[String, DataHash]
  }

  private val isLocalDev = localDocRoot.nonEmpty

  def hashFor(path: String): Option[String] = {
    dataHash(path).map(_.hash)
  }

  override def getInputStream(path: String): Option[InputStream] = {
    dataHash(path).map(d => new ByteArrayInputStream(d.data))
  }

  private def dataHash(path: String): Option[DataHash] = {
    if (isLocalDev) {
      getResource(path)
    } else {
      Option(cache.getIfPresent(path)).orElse {
        val loaded = getResource(path)

        loaded.foreach(data => cache.put(path, data))

        loaded
      }
    }
  }

  private def getResource(path: String): Option[DataHash] = {
    val stream = super.getInputStream(path)

    stream.map(s => {
      val data = IOUtils.toByteArray(s)

      val hash = Hashing.murmur3_128().hashBytes(data).toString

      DataHash(hash, data)
    })
  }
}
