/**
 * Copyright 2015 Orbeon, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orbeon.darius.api

import java.io.ByteArrayInputStream

import org.orbeon.darius.parsers.AbstractXMLDocumentParser
import org.orbeon.darius.parsers.NonValidatingConfiguration
import org.orbeon.darius.util.SymbolTable
import org.orbeon.darius.xni._
import org.orbeon.darius.xni.parser.XMLErrorHandler
import org.orbeon.darius.xni.parser.XMLInputSource
import org.orbeon.darius.xni.parser.XMLParseException

import scala.scalajs.js.annotation.JSExport

@JSExport
object API {
  
  @JSExport
  def parseString(xml: String, handler: XMLDocumentHandler): Unit = {
    
    val baseDir  = null
    val fileName = "test.xml"
    val encoding = "UTF-8"
    
    val source  = new XMLInputSource(null, fileName, baseDir, new ByteArrayInputStream(xml.getBytes("UTF-8")), encoding)
    val symbols = new SymbolTable
    val config  = new NonValidatingConfiguration(symbols)
    val parser  = new AbstractXMLDocumentParser(config) {}
    
    config.setDocumentHandler(handler)
    config.setErrorHandler(new XMLErrorHandler {
      def warning(domain: String, key: String, exception: XMLParseException): Unit =
        println("Warning: " + exception.getMessage)
      def error(domain: String, key: String, exception: XMLParseException): Unit =
        println("Error: " + exception.getMessage)
      def fatalError(domain: String, key: String, exception: XMLParseException): Unit =
        println("Fatal: " + exception.getMessage)
    })
    parser.parse(source)
    symbols.dumpEntries()
  }
}
