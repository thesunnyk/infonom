package org.teamchoko.infonom

import akka.actor.{ActorSystem, Actor, Props}
import akka.io.IO
import spray.can.Http

class DemoService extends Actor {
  def receive = {
    case _: Http.Connected => sender ! Http.Register(self)
  }
}

/**
 * Hello world!
 *
 */
object App {
  def message = "Hello World"

  implicit val system = ActorSystem()

  val myListener = system.actorOf(Props[DemoService], name = "handler")

  def main(args: Array[String]) : Unit = {
    println(message)
    IO(Http) ! Http.Bind(myListener, interface = "localhost", port = 8080)
  }

}
