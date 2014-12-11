package org.teamchoko.infonom

import scala.collection.immutable.Stream
import java.net.URLEncoder
import java.util.UUID
import org.joda.time.DateTime
import scalaz.stream.Process
import scalaz.concurrent.Task
import java.net.URI
import org.http4s.{Response, Status, Method, Message, MediaType, Request, EntityBody, EntityDecoder, Uri, ParseException, ParseResult}
import doobie.util.transactor
import scalaz.effect.IO

object DbConn {
  val db = transactor.DriverManagerTransactor[IO]("org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")

}