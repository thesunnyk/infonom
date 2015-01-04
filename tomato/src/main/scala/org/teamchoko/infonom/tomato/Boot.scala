package org.teamchoko.infonom.tomato

import org.http4s.server.blaze.BlazeBuilder
import scala.concurrent.ExecutionContext.Implicits.global

object Boot extends App with MyService {

  BlazeBuilder.mountService(service, "/").run
}

