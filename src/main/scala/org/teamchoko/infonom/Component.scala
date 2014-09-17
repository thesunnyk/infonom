package org.teamchoko.infonom

object Literals {
  val DOT = "."
  val HASH = "#"
  val SP = " "
  val BRACE_OPEN = "{"
  val BRACE_CLOSE = "}"

  val COLON = ":"
  val SEMI = ";"
}

trait Component {
  def classStyle : String

  def objectStyle : String

  def classScript : String

  def objectScript : String

  def html : String

  def className : String

  def objectId : String
}

object Atom {
  import Literals._

  def style(map: Map[String, String]) : String = map.foldLeft("") {
    case (p, (k, v)) => p + k + COLON + SP + v + SEMI
  }
}

trait Atom extends Component {
  import Literals._
  import Atom.style

  def objectStyle : String = HASH + objectId + SP + BRACE_OPEN + style(idStyleMap) + BRACE_CLOSE

  def classStyle : String = DOT + className + SP + BRACE_OPEN + style(classStyleMap) + BRACE_CLOSE

  def idStyleMap: Map[String, String]

  def classStyleMap: Map[String, String]
}
