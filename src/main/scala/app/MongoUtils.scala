package app

import com.mongodb.{BulkWriteResult, DBCollection, DBObject, MongoClient}

/**
 * Created by miaojing on 15-1-13.
 */
object MongoUtils {
  val _client = new MongoClient("localhost", 27017)
  val DB_NAME = "xiaozu_crawler"


  def dropDB(): Unit ={
    _client.dropDatabase(DB_NAME)
  }

  def dropCollection(collection: String): Unit = {
    _getCollection(collection).drop()
  }

  def insert(collection: String, dBObject: DBObject): Unit ={
    _getCollection(collection).insert(dBObject)
  }

  def _getCollection(collection: String): DBCollection = {
    _client.getDB(DB_NAME).getCollection(collection)
  }

  def batchInsert(collection: String, docs: List[DBObject]): BulkWriteResult = {
    val builder = _getCollection(collection).initializeOrderedBulkOperation()
    for (doc <- docs){
      builder.insert(doc)
    }

    builder.execute()
  }
}
