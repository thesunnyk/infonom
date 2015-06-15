package org.teamchoko.infonom.tomato

import org.http4s.server.blaze.BlazeBuilder
import scala.concurrent.ExecutionContext.Implicits.global

import doobie.imports.DriverManagerTransactor
import doobie.imports.toMoreConnectionIOOps
import scalaz.concurrent.Task

object Boot extends App with MyService {

  log.info(toError(DbConn.initialiseDb.transact(DbConn.xa).attemptRun.leftMap(x => x.getMessage)))

  BlazeBuilder.mountService(service, "/").run
}

