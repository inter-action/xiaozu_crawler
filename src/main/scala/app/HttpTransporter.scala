package app

import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet}
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager

/**
 * Created by Administrator on 15-1-12.
 */
object HttpTransporter {
  // initalization block
  private val client: CloseableHttpClient = {
    val connectionManager = new PoolingHttpClientConnectionManager()
    HttpClients.custom().setConnectionManager(connectionManager).build()
  }

  /**
   * 请求资源 是否要关闭资源由调用者控制
   * @param url
   * @param throwErrorOnBadRequest 是否在请求返回非200状态码的时候抛出异常
   * @return CloseableHttpResponse
   */
  //todo: 方法扔出运行时异常 这个方法签名该如何优化
  def get(url: String, throwErrorOnBadRequest: Boolean = false): CloseableHttpResponse = {
    val response: CloseableHttpResponse = client.execute(new HttpGet(url))

    if (throwErrorOnBadRequest){
      val code = response.getStatusLine.getStatusCode
      if (code != 200){
        throw new RuntimeException(s"get url: $url failed with code: $code")
      }
    }

    response
  }


  //http://stackoverflow.com/questions/2207425/what-automatic-resource-management-alternatives-exists-for-scala
  def using[T <: { def close() }] (resource: T) (block: T => Unit) ={
    try{
      block(resource)
    }finally{
      if (resource != null) resource.close()
    }
  }
}
