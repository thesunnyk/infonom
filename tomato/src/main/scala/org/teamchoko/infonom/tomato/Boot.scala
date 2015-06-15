package org.teamchoko.infonom.tomato

import org.http4s.server.blaze.BlazeBuilder
import scala.concurrent.ExecutionContext.Implicits.global

import doobie.imports.DriverManagerTransactor
import doobie.imports.toMoreConnectionIOOps
import scalaz.concurrent.Task

object Boot extends App with MyService {

  DbConn.initialiseDb.transact(DbConn.xa).run

  BlazeBuilder.mountService(service, "/").run
}

