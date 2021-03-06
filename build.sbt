name := "XIAOZU_CRAWLER"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
    "org.apache.httpcomponents" % "httpclient" % "4.3.6",
    "com.squareup.okhttp" % "okhttp" % "2.2.0",
    "net.sourceforge.nekohtml" % "nekohtml" % "1.9.21",
    "org.mongodb" % "mongo-java-driver" % "2.12.4"
)

mainClass in assembly := Some("app.App")