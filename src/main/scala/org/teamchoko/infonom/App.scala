package org.teamchoko.infonom

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http

/**
 * Hello world!
 *
 */
object Main extends App with MySslConfiguration {
  implicit val system = ActorSystem()

  val handler = system.actorOf(Props[DemoService], name = "handler")

  IO(Http) ! Http.Bind(handler, interface = "localhost", port = 8080)
}
