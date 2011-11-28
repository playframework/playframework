package library

import akka.actor.Actor
import Actor._
import play.api.mvc.Results._


case class Work(start: Int, nrOfElements: Int)

class Calculator extends Actor {

  def calculatePiFor(start: Int, nrOfElements: Int): Double = {
    var acc = 0.0
    for (i <- start until (start + nrOfElements))
      acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1)
    acc
  }

  def receive = {
       case Work(start,nrOfElements) => self.reply(Ok("<h1>Pi</h1>starting from "+start+" <br /> number of elements "+ nrOfElements+" <br /> result: "+calculatePiFor(start,nrOfElements).toString).as("text/html"))
  }
}
