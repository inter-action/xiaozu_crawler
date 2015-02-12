package app

import java.io.{ByteArrayInputStream, IOException}
import java.util.concurrent.CountDownLatch

import com.mongodb.{BasicDBObjectBuilder, DBObject}
import com.squareup.okhttp.{Request, Response, Callback}

import scala.collection.mutable.ListBuffer


/**
 * Created by miaojing on 15-2-10.
 *  OkHttp 的异步使用线程来实现的, 不像nodejs的事件通知, 所以使用时候要注意线程同步的问题
 */
object AsyncApp {

  val parser = HttpParser
  val mongo = MongoUtils

  val COLLECTION_NAME = "menu"
  val asyncHttpParser = AsyncHttpParser


  def main(args: Array[String]) {


    val catsRespStr = asyncHttpParser.syncGet("http://fuhua.xiaozufan.com/Index/index/category/a?zh=main")
    val catLinks = parser.getCatLinks(toInputStream(catsRespStr))

    for (arr <- catLinks){
      println(s"linkname: ${arr._1}, link href: ${arr._2}")
    }

    mongo.dropCollection(COLLECTION_NAME)

    val docs: ListBuffer[DBObject] = ListBuffer()
    val latch = new CountDownLatch(catLinks.length)

    for (link <- catLinks){

      val (linkname: String, linkhref: String) = link
      if (linkhref.contains("/category/a")){

        asyncHttpParser.asyncGet(linkhref, new Callback {
          override def onFailure(request: Request, e: IOException): Unit = {
            latch.countDown()
            e.printStackTrace()
          }

          override def onResponse(response: Response): Unit = {
            if (!response.isSuccessful){
              latch.countDown()
              throw new IOException(s"unexepected code: ${response}")
            }

            try{
              parser.synchronized {

                println(s"cate a link: $linkhref")

                val responseStr = response.body().string()
                val catAContents = parser.parseCateA(toInputStream(responseStr))
                val catADBObject = BasicDBObjectBuilder.start()
                catADBObject.add( "name", BasicDBObjectBuilder.start().add("name", linkname).add("link", linkhref).get() )
                catADBObject.add( "contents", catAContents)
                docs += catADBObject.get()
              }

            } finally {
              latch.countDown()
            }
          }
        })

      }else{

        asyncHttpParser.asyncGet(linkhref, new Callback {
          override def onFailure(request: Request, e: IOException): Unit = {
            latch.countDown()
            e.printStackTrace()
          }

          override def onResponse(response: Response): Unit = {
            if (!response.isSuccessful){
              latch.countDown()
              throw new IOException(s"unexepected code: ${response}")
            }

            try{
              parser.synchronized {

                println(s"\ncate b link: $linkhref")

                val responseStr = response.body().string()
                val catBContents = parser.parseCateB(toInputStream(responseStr))
                val catBDBObject = BasicDBObjectBuilder.start()
                catBDBObject.add( "name", BasicDBObjectBuilder.start().add("name", linkname).add("link", linkhref).get() )
                catBDBObject.add( "contents", catBContents)
                docs += catBDBObject.get()
              }
            } finally {
              latch.countDown()
            }
          }
        })
      }
    }

    latch.await()
    asyncHttpParser.client.getDispatcher.getExecutorService.shutdown()

    mongo.batchInsert(COLLECTION_NAME, docs.toList)
    mongo._client.close()

    println("process exit")
  }

  def toInputStream(str: String, charset: String = "utf-8") = new ByteArrayInputStream(str.getBytes(charset))
}
