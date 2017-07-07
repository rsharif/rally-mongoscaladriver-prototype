package com.rallyhealth.scaladriver

import org.bson.codecs.ObjectIdGenerator
import org.joda.time.DateTime
import org.scalatest.FunSuite

import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Await}

import scala.concurrent.ExecutionContext.Implicits.global

class ScalaDriverBoxPersistenceSpec extends FunSuite {

  val atMost = Duration(2000, "millis")

  test("ScalaDriver CRUD ops") {

    val cBoxId = new ObjectIdGenerator().generate().toString
    val rBoxId = new ObjectIdGenerator().generate().toString
    val fBoxId = new ObjectIdGenerator().generate().toString
    val bBoxId = new ObjectIdGenerator().generate().toString


    val now = DateTime.now

    val cBox = CorrugatedBox(cBoxId, length = 1, width = 1, height = 1, manufactureDate =  now, lastShipped = None,layers = 4)
    val rBox = RigidBox(rBoxId, length = 4, width = 2, height = 2, manufactureDate =  now, lastShipped = Some(now), numberOfPiece = 5)
    val fBox = FoldingBox(fBoxId, length = 2, width = 1, height = 1, manufactureDate =  now, lastShipped = Some(now), style = "b")
    val boxOfBoxes = BoxOfBoxes(bBoxId, length = 3, width = 1, height = 1, manufactureDate =  now, lastShipped = None, boxes = Seq(cBox, rBox, fBox))

    val persister = new ScalaDriverBoxPersistence()
    //remove any previous boxes
    val deletionResult = Await.result(persister.deleteAll(), atMost)
    assert(deletionResult.wasAcknowledged())
    //save boxes

    Await.result(
      Future.sequence(
        Seq(
          persister.save(cBox),
          persister.save(rBox),
          persister.save(fBox),
          persister.save(boxOfBoxes)
        )), atMost)
    //find corrugatedBox
    val box = Await.result[Option[CorrugatedBox]](persister.findOneCorrugatedBox(), atMost)
    assert(box.isDefined)
    assert(box.get == cBox)
//    //find all boxes sorted by length
    val boxes = Await.result[Seq[Box]](persister.findAllBoxesSortedByLength(), atMost)
    assert(boxes(0).length == 1)
    assert(boxes(1).length == 2)
    assert(boxes(2).length == 3)
    assert(boxes(3).length == 4)
//    //verify nullable value , box(2) has lastShipped None
    assert(boxes(2).lastShipped.isEmpty)
    assert(boxes(3).lastShipped == Some(now))

    val totalLength = Await.result(persister.findAggregateLength(), atMost)
    assert(totalLength == 10)

    val deletionResultAfter = Await.result(persister.deleteAll(), atMost)
    assert(deletionResultAfter.wasAcknowledged())
  }

  test("stress") {
      var id = ""
      var timeBefore = System.currentTimeMillis()
      val persister = new ScalaDriverBoxPersistence()
      for ( i <- 1 to 100000) {
        id = new ObjectIdGenerator().generate().toString
        val cBox = CorrugatedBox(id, length = 1, width = 1, height = 1, manufactureDate =  DateTime.now, lastShipped = None,layers = 4)
        Await.result(persister.save(cBox), atMost )
        val box = Await.result[Option[CorrugatedBox]](persister.findCorrugatedBoxById(id), atMost)
        assert(box.get == cBox)

      }
      var timeAfter = System.currentTimeMillis()
      println((timeAfter - timeBefore)/(1000))
  }
}
