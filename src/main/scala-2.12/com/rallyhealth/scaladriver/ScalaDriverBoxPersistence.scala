package com.rallyhealth.scaladriver

import com.mongodb.client.result.DeleteResult
import com.rallyhealth.scaladriver.Box
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistries._
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}
import org.joda.time.{DateTimeZone, DateTime}
import org.mongodb.scala._
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.Accumulators._
import org.mongodb.scala.model.Aggregates._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.Filters.eq
import org.mongodb.scala.model.Sorts._


import scala.concurrent.{Promise, Future}
import scala.util.Try

import scala.concurrent.ExecutionContext.Implicits.global



class JodaCodec extends Codec[DateTime] {

  override def decode(bsonReader: BsonReader, decoderContext: DecoderContext): DateTime = new DateTime(bsonReader.readDateTime())

  override def encode(bsonWriter: BsonWriter, t: DateTime, encoderContext: EncoderContext): Unit = bsonWriter.writeDateTime(t.getMillis)

  override def getEncoderClass: Class[DateTime] = classOf[DateTime]
}

class ScalaDriverBoxConnectionManager {
  val mongoClient: MongoClient = MongoClient()
  val database: MongoDatabase = mongoClient.getDatabase("scalaDriverDb")
}

class ScalaDriverBoxPersistence extends ScalaDriverBoxConnectionManager {

  val codecRegistry = fromRegistries( fromProviders(classOf[Box]), CodecRegistries.fromCodecs(new JodaCodec), DEFAULT_CODEC_REGISTRY )

  val collection: MongoCollection[Box] = database.getCollection[Box]("box").withCodecRegistry(codecRegistry)

  def save(box: Box): Future[Unit] = {

    val promise = Promise[Unit]()

    def observer  = new Observer[Completed] {

      override def onNext(result: Completed): Unit = promise.complete(Try(()))

      override def onError(e: Throwable): Unit = promise.failure(new Exception("Failed to insert :" + e.getMessage))

      override def onComplete(): Unit = promise.complete(Try(()))
    }

    collection.insertOne(box).subscribe(observer)

    promise.future
  }

  def findOneCorrugatedBox(): Future[Option[CorrugatedBox]] = {

    import org.mongodb.scala.model.Filters.{eq => eqTo}
    collection
      .find(eqTo("length", 1))
      .toFuture()
      .map(_.headOption.map(_.asInstanceOf[CorrugatedBox]))
  }

  def findCorrugatedBoxById(id: String): Future[Option[CorrugatedBox]] = {

    import org.mongodb.scala.model.Filters.{eq => eqTo}
    collection
      .find(eqTo("_id", id))
      .toFuture()
      .map(_.headOption.map(_.asInstanceOf[CorrugatedBox]))
  }

  def deleteAll(): Future[DeleteResult] = {
    val promise = Promise[DeleteResult]()

    collection.deleteMany(org.mongodb.scala.model.Filters.exists("length")).subscribe(new Observer[DeleteResult] {

      override def onError(e: Throwable): Unit = promise.failure(new Exception("Failed to get :" + e.getMessage))

      override def onComplete(): Unit = ()

      override def onNext(result: DeleteResult): Unit = {
        promise.complete(Try(result))
      }
    })
    promise.future
  }

  def findAllBoxesSortedByLength(): Future[Seq[Box]] = {
    collection
      .find().sort(ascending("length"))
      .toFuture()
  }

  def findAggregateLength() : Future[Int] = {
    val promise = Promise[Int]()
    collection.aggregate[Document](List(project(and(excludeId(), include("length"))),group(null, sum("total", "$length")))).subscribe(new Observer[Document] {
      override def onError(e: Throwable): Unit = promise.failure(new Exception("Failed to aggregate :" + e.getMessage))

      override def onComplete(): Unit = ()

      override def onNext(result: Document): Unit = promise.complete(Try(result.getInteger("total")))

    })
    promise.future
  }
}
