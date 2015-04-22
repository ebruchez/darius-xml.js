package org.orbeon.darius.util

import java.io.PrintWriter

import org.orbeon.darius.xni.parser.XMLErrorHandler
import org.orbeon.darius.xni.parser.XMLParseException
import org.orbeon.darius.xni.parser.XMLErrorHandler

/**
 * Default error handler.
 */
class DefaultErrorHandler(protected var fOut: PrintWriter) extends XMLErrorHandler {

  /**
   * Constructs an error handler that prints error messages to
   * `System.err`.
   */
  def this() {
    this(new PrintWriter(System.err))
  }

  /**
   Warning.
   */
  def warning(domain: String, key: String, ex: XMLParseException): Unit = {
    printError("Warning", ex)
  }

  /**
   Error.
   */
  def error(domain: String, key: String, ex: XMLParseException): Unit = {
    printError("Error", ex)
  }

  /**
   Fatal error.
   */
  def fatalError(domain: String, key: String, ex: XMLParseException): Unit = {
    printError("Fatal Error", ex)
    throw ex
  }

  /**
   Prints the error message.
   */
  private def printError(`type`: String, ex: XMLParseException): Unit = {
    fOut.print("[")
    fOut.print(`type`)
    fOut.print("] ")
    var systemId = ex.getExpandedSystemId
    if (systemId ne null) {
      val index = systemId.lastIndexOf('/')
      if (index != -1) systemId = systemId.substring(index + 1)
      fOut.print(systemId)
    }
    fOut.print(':')
    fOut.print(ex.getLineNumber)
    fOut.print(':')
    fOut.print(ex.getColumnNumber)
    fOut.print(": ")
    fOut.print(ex.getMessage)
    fOut.println()
    fOut.flush()
  }
}
