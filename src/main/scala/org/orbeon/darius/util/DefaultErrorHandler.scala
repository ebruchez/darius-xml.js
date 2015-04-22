/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
