package actors

import akka.actor.{ActorLogging, Actor}
import play.api.Play.current

case class Greeting(who: String)

class HelloActor extends Actor with ActorLogging {
  def receive = {
    case Greeting(who) =>
      println("receive message -> " + who)
      sender ! ("hello " + who)
    case _ =>
      println("receive unknown message ")
  }
}
