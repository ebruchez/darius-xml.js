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

package org.orbeon.darius.xml.xni.parser

import java.io.IOException

import org.orbeon.darius.xml.xni.XMLDTDContentModelHandler
import org.orbeon.darius.xml.xni.XMLDTDHandler
import org.orbeon.darius.xml.xni.XMLDocumentHandler
import org.orbeon.darius.xml.xni.XNIException

/**
 * Represents a parser configuration. The parser configuration maintains
 * a table of recognized features and properties, assembles components
 * for the parsing pipeline, and is responsible for initiating parsing
 * of an XML document.
 * 
 * By separating the configuration of a parser from the specific parser
 * instance, applications can create new configurations and re-use the
 * existing parser components and external API generators (e.g. the
 * DOMParser and SAXParser).
 * 
 * The internals of any specific parser configuration instance are hidden.
 * Therefore, each configuration may implement the parsing mechanism any
 * way necessary. However, the parser configuration should follow these
 * guidelines:
 * 
 *  - 
 *   Call the `reset` method on each component before parsing.
 *   This is only required if the configuration is re-using existing
 *   components that conform to the `XMLComponent` interface.
 *   If the configuration uses all custom parts, then it is free to
 *   implement everything as it sees fit as long as it follows the
 *   other guidelines.
 *  
 *  - 
 *   Call the `setFeature` and `setProperty` method
 *   on each component during parsing to propagate features and properties
 *   that have changed. This is only required if the configuration is
 *   re-using existing components that conform to the `XMLComponent`
 *   interface. If the configuration uses all custom parts, then it is free
 *   to implement everything as it sees fit as long as it follows the other
 *   guidelines.
 *  
 *  - 
 *   Pass the same unique String references for all symbols that are
 *   propagated to the registered handlers. Symbols include, but may not
 *   be limited to, the names of elements and attributes (including their
 *   uri, prefix, and localpart). This is suggested but not an absolute
 *   must. However, the standard parser components may require access to
 *   the same symbol table for creation of unique symbol references to be
 *   propagated in the XNI pipeline.
 *  
 * 
 */
trait XMLParserConfiguration extends XMLComponentManager {

  /**
   * Parse an XML document.
   * 
   * The parser can use this method to instruct this configuration
   * to begin parsing an XML document from any valid input source
   * (a character stream, a byte stream, or a URI).
   * 
   * Parsers may not invoke this method while a parse is in progress.
   * Once a parse is complete, the parser may then parse another XML
   * document.
   * 
   * This method is synchronous: it will not return until parsing
   * has ended.  If a client application wants to terminate
   * parsing early, it should throw an exception.
   * 
   * When this method returns, all characters streams and byte streams
   * opened by the parser are closed.
   *
   * @param inputSource The input source for the top-level of the
   *                    XML document.
   *
   * @throws XNIException Any XNI exception, possibly wrapping
   *                         another exception.
   * @throws IOException  An IO exception from the parser, possibly
   *                         from a byte stream or character stream
   *                         supplied by the parser.
   */
  def parse(inputSource: XMLInputSource): Unit

  /**
   * Allows a parser to add parser specific features to be recognized
   * and managed by the parser configuration.
   *
   * @param featureIds An array of the additional feature identifiers
   *                   to be recognized.
   */
  def addRecognizedFeatures(featureIds: Array[String]): Unit

  /**
   * Sets the state of a feature. This method is called by the parser
   * and gets propagated to components in this parser configuration.
   *
   * @param featureId The feature identifier.
   * @param state     The state of the feature.
   *
   * @throws XMLConfigurationException Thrown if there is a configuration
   *                                   error.
   */
  def setFeature(featureId: String, state: Boolean): Unit

  /**
   * Returns the state of a feature.
   *
   * @param featureId The feature identifier.
   *
   * @throws XMLConfigurationException Thrown if there is a configuration
   *                                   error.
   */
  def getFeature(featureId: String): Boolean

  /**
   * Allows a parser to add parser specific properties to be recognized
   * and managed by the parser configuration.
   *
   * @param propertyIds An array of the additional property identifiers
   *                    to be recognized.
   */
  def addRecognizedProperties(propertyIds: Array[String]): Unit

  /**
   * Sets the value of a property. This method is called by the parser
   * and gets propagated to components in this parser configuration.
   *
   * @param propertyId The property identifier.
   * @param value      The value of the property.
   *
   * @throws XMLConfigurationException Thrown if there is a configuration
   *                                   error.
   */
  def setProperty(propertyId: String, value: AnyRef): Unit

  /**
   * Returns the value of a property.
   *
   * @param propertyId The property identifier.
   *
   * @throws XMLConfigurationException Thrown if there is a configuration
   *                                   error.
   */
  def getProperty(propertyId: String): Any

  /**
   * Sets the error handler.
   *
   * @param errorHandler The error resolver.
   */
  def setErrorHandler(errorHandler: XMLErrorHandler): Unit

  /**
   Returns the registered error handler.
   */
  def getErrorHandler: XMLErrorHandler

  /**
   * Sets the document handler to receive information about the document.
   *
   * @param documentHandler The document handler.
   */
  def setDocumentHandler(documentHandler: XMLDocumentHandler): Unit

  /**
   Returns the registered document handler.
   */
  def getDocumentHandler: XMLDocumentHandler

  /**
   * Sets the DTD handler.
   *
   * @param dtdHandler The DTD handler.
   */
  def setDTDHandler(dtdHandler: XMLDTDHandler): Unit

  /**
   Returns the registered DTD handler.
   */
  def getDTDHandler: XMLDTDHandler

  /**
   * Sets the DTD content model handler.
   *
   * @param dtdContentModelHandler The DTD content model handler.
   */
  def setDTDContentModelHandler(dtdContentModelHandler: XMLDTDContentModelHandler): Unit

  /**
   Returns the registered DTD content model handler.
   */
  def getDTDContentModelHandler: XMLDTDContentModelHandler

  /**
   * Sets the entity resolver.
   *
   * @param entityResolver The new entity resolver.
   */
  def setEntityResolver(entityResolver: XMLEntityResolver): Unit

  /**
   Returns the registered entity resolver.
   */
  def getEntityResolver: XMLEntityResolver
}
