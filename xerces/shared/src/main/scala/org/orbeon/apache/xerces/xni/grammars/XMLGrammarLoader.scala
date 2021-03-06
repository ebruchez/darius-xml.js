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

package org.orbeon.apache.xerces.xni.grammars

import java.io.IOException

import org.orbeon.apache.xerces.xni.parser.XMLConfigurationException
import org.orbeon.apache.xerces.xni.parser.XMLEntityResolver
import org.orbeon.apache.xerces.xni.parser.XMLErrorHandler
import org.orbeon.apache.xerces.xni.parser.XMLInputSource

/**
 * The intention of this interface is to provide a generic means
 * by which Grammar objects may be created without parsing instance
 * documents.  Implementations of this interface will know how to load
 * specific types of grammars (e.g., DTD's or schemas); a wrapper
 * will be provided for user applications to interact with these implementations.
 */
trait XMLGrammarLoader {

  /**
   * Returns a list of feature identifiers that are recognized by
   * this XMLGrammarLoader.  This method may return null if no features
   * are recognized.
   */
  def getRecognizedFeatures: Array[String]

  /**
   * Returns the state of a feature.
   *
   * @param featureId The feature identifier.
   *
   * @throws XMLConfigurationException Thrown on configuration error.
   */
  def getFeature(featureId: String): Boolean

  /**
   * Sets the state of a feature.
   *
   * @param featureId The feature identifier.
   * @param state     The state of the feature.
   *
   * @throws XMLConfigurationException Thrown when a feature is not
   *                  recognized or cannot be set.
   */
  def setFeature(featureId: String, state: Boolean): Unit

  /**
   * Returns a list of property identifiers that are recognized by
   * this XMLGrammarLoader.  This method may return null if no properties
   * are recognized.
   */
  def getRecognizedProperties: Array[String]

  /**
   * Returns the state of a property.
   *
   * @param propertyId The property identifier.
   *
   * @throws XMLConfigurationException Thrown on configuration error.
   */
  def getProperty(propertyId: String): AnyRef

  /**
   * Sets the state of a property.
   *
   * @param propertyId The property identifier.
   * @param state     The state of the property.
   *
   * @throws XMLConfigurationException Thrown when a property is not
   *                  recognized or cannot be set.
   */
  def setProperty(propertyId: String, state: AnyRef): Unit

  /**
   * Sets the error handler.
   *
   * @param errorHandler The error handler.
   */
  def setErrorHandler(errorHandler: XMLErrorHandler): Unit

  /**
   Returns the registered error handler.
   */
  def getErrorHandler: XMLErrorHandler

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

  /**
   * Returns a Grammar object by parsing the contents of the
   * entity pointed to by source.
   *
   * @param source        the location of the entity which forms
   *                          the starting point of the grammar to be constructed.
   * @throws IOException      When a problem is encountered reading the entity
   *          XNIException    When a condition arises (such as a FatalError) that requires parsing
   *                              of the entity be terminated.
   */
  def loadGrammar(source: XMLInputSource): Grammar
}
