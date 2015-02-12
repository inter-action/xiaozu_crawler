package app

import java.io.IOException
import java.util.concurrent.TimeUnit

import com.squareup.okhttp.{Callback, Response, OkHttpClient}
import com.squareup.okhttp.Request.Builder

/**
 * Created by Administrator on 15-2-10.
 */
object AsyncHttpParser {
  val client: OkHttpClient = new OkHttpClient()
  client.setConnectTimeout(5, TimeUnit.SECONDS)
  client.setReadTimeout(10, TimeUnit.SECONDS)

  @throws(classOf[IOException])
  def syncGet(url: String): String = {
    val request = new Builder().url(url).build()
    val response: Response = client.newCall(request).execute()
    if (!response.isSuccessful) throw new IOException(s"Unexpected code: ${response}")
    response.body().string()
  }

  def asyncGet(url: String, callback: Callback): Unit = {
    val request = new Builder().url(url).build()
    client.newCall(request).enqueue(callback)
  }

}
