package org.teamchoko.infonom.tomato

import org.http4s.server.blaze.BlazeBuilder
import org.teamchoko.infonom.tomato.db.DbConn
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz.concurrent.Task

object Boot extends App with MyService {

  log.info(toError(DbConn.initDb))

  BlazeBuilder.mountService(service, "/").run
}

