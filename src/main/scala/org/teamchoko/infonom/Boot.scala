package org.teamchoko.infonom

import org.http4s.server.blaze.BlazeServer

object Boot extends App with MyService {

  BlazeServer.newBuilder
    .mountService(service, "/")
    .run()
}

