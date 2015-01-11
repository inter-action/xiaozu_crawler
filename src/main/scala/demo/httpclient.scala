package demo

import org.apache.http.impl.client._
import org.apache.http.client.methods._
import org.apache.http.util._
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager

/**
 *http://www.yeetrack.com/?p=782
 */
object httpclient {
  def main(args: Array[String]) = {
    val connectionManager = new PoolingHttpClientConnectionManager()
    val client = HttpClients.custom().setConnectionManager(connectionManager).build()
    val httpget = new HttpGet("http://fuhua.xiaozufan.com/Index/main")
    val response = client.execute(httpget)
    try {
      println(response.getStatusLine())
      val entity = response.getEntity()
      //EntityUtils.consume(entity)//ensure the repsonse is fully cosumed
      val resStr = EntityUtils.toString(entity)
      //println(resStr)

    } finally {
      response.close()
    }
  }
}

