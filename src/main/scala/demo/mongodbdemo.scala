package demo

import com.mongodb.{BasicDBObject, MongoClient}

/**
 * Created by miaojing on 1/10/15.
 * email : 243127495@qq.com
 */
object mongodbdemo {
  def main(args: Array[String]) {
    //The MongoClient class is designed to be thread safe and shared among threads.
    //to dispose of an instance, make sure you call MongoClient.close() to clean up resources
    val mongoClient = new MongoClient("localhost", 27017)
    val db = mongoClient.getDB("test")


    //--------- START:  insert a doc: ---------------
    /*


    {
         "name" : "MongoDB",
         "type" : "database",
         "count" : 1,
         "info" : {
                     x : 203,
                     y : 102
                   }
      }
    */
    val doc = new BasicDBObject("name" , "MongoDB")
      .append("type", "database").append("count", 1)
      .append("info", new BasicDBObject("x", 103).append("y", "102") )

    db.getCollection("temp").insert(doc)


    //--------- END:  insert a doc: ---------------


    //--------- START:  find a doc: ---------------
    val query = new BasicDBObject("name", "MongoDB")
    val cursor = db.getCollection("temp").find(query)
    try{
      while (cursor.hasNext){
        println(cursor.next())
      }
    } finally {
      cursor.close()
    }

    //--------- START:  remove a collection: ---------------

    // drop collection
    db.getCollection("temp").drop()

    // drop database
    mongoClient.dropDatabase("test")

    mongoClient.close()
  }
}


/*

import static java.util.concurrent.TimeUnit.SECONDS;

// To directly connect to a single MongoDB server (note that this will not auto-discover the primary even
// if it's a member of a replica set:
MongoClient mongoClient = new MongoClient();
// or
MongoClient mongoClient = new MongoClient( "localhost" );
// or
MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
// or, to connect to a replica set, with auto-discovery of the primary, supply a seed list of members
MongoClient mongoClient = new MongoClient(Arrays.asList(new ServerAddress("localhost", 27017),
                                      new ServerAddress("localhost", 27018),
                                      new ServerAddress("localhost", 27019)));

DB db = mongoClient.getDB( "mydb" );
 */