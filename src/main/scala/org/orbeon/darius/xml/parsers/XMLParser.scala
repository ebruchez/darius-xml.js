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

package org.orbeon.darius.xml.parsers

import org.orbeon.darius.xml.impl.Constants
import org.orbeon.darius.xml.parsers.XMLParser._
import org.orbeon.darius.xml.xni.XNIException
import org.orbeon.darius.xml.xni.parser.XMLInputSource
import org.orbeon.darius.xml.xni.parser.XMLParserConfiguration

object XMLParser {

  /**
   Property identifier: entity resolver.
   */
  protected val ENTITY_RESOLVER = Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_RESOLVER_PROPERTY

  /**
   Property identifier: error handler.
   */
  protected val ERROR_HANDLER = Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_HANDLER_PROPERTY

  /**
   Recognized properties.
   */
  private val RECOGNIZED_PROPERTIES = Array(ENTITY_RESOLVER, ERROR_HANDLER)
}

/**
 * Base class of all XML-related parsers.
 * 
 * In addition to the features and properties recognized by the parser
 * configuration, this parser recognizes these additional features and
 * properties:
 * 
 * - Properties
 *  
 *   - http://apache.org/xml/properties/internal/error-handler
 *   - http://apache.org/xml/properties/internal/entity-resolver
 *  
 * 
 */
abstract class XMLParser protected (protected val fConfiguration: XMLParserConfiguration) {

  fConfiguration.addRecognizedProperties(RECOGNIZED_PROPERTIES)

  def parse(inputSource: XMLInputSource): Unit = {
    reset()
    fConfiguration.parse(inputSource)
  }

  /**
   * reset all components before parsing
   */
  protected def reset(): Unit = ()
}
