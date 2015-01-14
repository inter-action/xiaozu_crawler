package app

import com.mongodb.{DBObject, BasicDBObjectBuilder}
import org.apache.http.client.methods.CloseableHttpResponse

import scala.collection.mutable.ListBuffer

/**
 * Created by miaojing on 15-1-12.
 */


object App {
  //imports rename
  val httptrans = HttpTransporter
  val parser = HttpParser
  val mongo = MongoUtils

  val COLLECTION_NAME = "menu"

  def main(args: Array[String]) {
    val catResp: CloseableHttpResponse = httptrans.get("http://fuhua.xiaozufan.com/Index/index/category/a?zh=main", true)
    val catEntity = catResp.getEntity
    val catIns = catEntity.getContent

    val catLinks = parser.getCatLinks(catIns)

    for (arr <- catLinks){
      println(s"linkname: ${arr._1}, link href: ${arr._2}")
    }

    mongo.dropDB()

    val docs: ListBuffer[DBObject] = ListBuffer()

    for (link <- catLinks){
      //      val linkname = link(0)
      //      val linkhref = link(1)
      val (linkname: String, linkhref: String) = link
      if (linkhref.contains("/category/a")){
        println(s"cate a link: $linkhref")

        val catALinkResponse = httptrans.get(linkhref)
        val catAContents = parser.parseCateA(catALinkResponse.getEntity.getContent)
        catALinkResponse.close()
        val catADBObject = BasicDBObjectBuilder.start()
        catADBObject.add( "name", BasicDBObjectBuilder.start().add("name", linkname).add("link", linkhref).get() )
        catADBObject.add( "contents", catAContents)

        docs += catADBObject.get()
      }else{
        println(s"cate b link: $linkhref")

        val catBLinkResponse = httptrans.get(linkhref)
        val catBContents = parser.parseCateB(catBLinkResponse.getEntity.getContent)
        catBLinkResponse.close()
        val catBDBObject = BasicDBObjectBuilder.start()
        catBDBObject.add( "name", BasicDBObjectBuilder.start().add("name", linkname).add("link", linkhref).get() )
        catBDBObject.add( "contents", catBContents)

        docs += catBDBObject.get()
      }
    }

    mongo.batchInsert(COLLECTION_NAME, docs.toList)
    mongo._client.close()
  }

  /**
   * 将文档插入 collection
   *
   * curry 的用法
   *
   * val insertToMenu = insertTo(COLLECTION_NAME)(_) //scala curry
   *
   * @param collectionName
   * @param dbObject
   * @return
   */
  def insertTo(collectionName: String)(dbObject: DBObject) = mongo.insert(collectionName, dbObject)

}
