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

package org.orbeon.darius.xml.util

import java.{util ⇒ ju}

import org.orbeon.darius.xml.impl.Constants
import org.orbeon.darius.xml.xni.parser.XMLComponentManager
import org.orbeon.darius.xml.xni.parser.XMLConfigurationException

object ParserConfigurationSettings {
  protected[darius] val PARSER_SETTINGS = Constants.XERCES_FEATURE_PREFIX + Constants.PARSER_SETTINGS
}

/**
 * This class implements the basic operations for managing parser
 * configuration features and properties. This utility class can
 * be used as a base class for parser configurations or separately
 * to encapsulate a number of parser settings as a component
 * manager.
 *
 * This class can be constructed with a "parent" settings object
 * (in the form of an `XMLComponentManager`) that allows
 * parser configuration settings to be "chained" together.
 */
class ParserConfigurationSettings(protected var fParentSettings: XMLComponentManager)
    extends XMLComponentManager {

  /**
   Recognized properties.
   */
  protected var fRecognizedProperties = new ju.ArrayList[String]()

  /**
   Properties.
   */
  protected var fProperties = new ju.HashMap[String, Any]()

  /**
   Recognized features.
   */
  protected var fRecognizedFeatures = new ju.ArrayList[String]()

  /**
   Features.
   */
  protected var fFeatures = new ju.HashMap[String, Any]()

  def this() {
    this(null)
  }

  /**
   * Allows a parser to add parser specific features to be recognized
   * and managed by the parser configuration.
   */
  def addRecognizedFeatures(featureIds: Array[String]): Unit = {
    val featureIdsCount = if (featureIds ne null) featureIds.length else 0
    for (i ← 0 until featureIdsCount) {
      val featureId = featureIds(i)
      if (!fRecognizedFeatures.contains(featureId)) {
        fRecognizedFeatures.add(featureId)
      }
    }
  }

  /**
   * Set the state of a feature.
   *
   * Set the state of any feature in a SAX2 parser.  The parser
   * might not recognize the feature, and if it does recognize
   * it, it might not be able to fulfill the request.
   *
   * @param featureId The unique identifier (URI) of the feature.
   * @param state The requested state of the feature (true or false).
   *
   * @throws org.orbeon.darius.xml.xni.parser.XMLConfigurationException If the
   *            requested feature is not known.
   */
  def setFeature(featureId: String, state: Boolean): Unit = {
    checkFeature(featureId)
    fFeatures.put(featureId, if (state) true else false)
  }

  /**
   * Allows a parser to add parser specific properties to be recognized
   * and managed by the parser configuration.
   *
   * @param propertyIds An array of the additional property identifiers
   *                    to be recognized.
   */
  def addRecognizedProperties(propertyIds: Array[String]): Unit = {
    val propertyIdsCount = if (propertyIds ne null) propertyIds.length else 0
    for (i ← 0 until propertyIdsCount) {
      val propertyId = propertyIds(i)
      if (!fRecognizedProperties.contains(propertyId)) {
        fRecognizedProperties.add(propertyId)
      }
    }
  }

  /**
   * setProperty
   *
   * @throws org.orbeon.darius.xml.xni.parser.XMLConfigurationException If the
   *            requested feature is not known.
   */
  def setProperty(propertyId: String, value: AnyRef): Unit = {
    checkProperty(propertyId)
    fProperties.put(propertyId, value)
  }

  /**
   * Returns the state of a feature.
   *
   * @param featureId The feature identifier.
   * @return true if the feature is supported
   *
   * @throws XMLConfigurationException Thrown for configuration error.
   *                                   In general, components should
   *                                   only throw this exception if
   *                                   it is *really*
   *                                   a critical error.
   */
  def getFeature(featureId: String): Boolean = {
    fFeatures.get(featureId) match {
      case state: java.lang.Boolean ⇒
        state
      case _ ⇒
        checkFeature(featureId)
        false
    }
  }

  /**
   * Returns the value of a property.
   *
   * @param propertyId The property identifier.
   * @return the value of the property
   *
   * @throws XMLConfigurationException Thrown for configuration error.
   *                                   In general, components should
   *                                   only throw this exception if
   *                                   it is *really*
   *                                   a critical error.
   */
  def getProperty(propertyId: String): Any = {
    fProperties.get(propertyId) match {
      case null ⇒
        checkProperty(propertyId)
        null
      case propertyValue ⇒
        propertyValue
    }
  }

  /**
   * Check a feature. If feature is known and supported, this method simply
   * returns. Otherwise, the appropriate exception is thrown.
   *
   * @param featureId The unique identifier (URI) of the feature.
   *
   * @throws org.orbeon.darius.xml.xni.parser.XMLConfigurationException If the
   *            requested feature is not known.
   */
  protected def checkFeature(featureId: String): Unit = {
    if (!fRecognizedFeatures.contains(featureId)) {
      if (fParentSettings ne null) {
        fParentSettings.getFeature(featureId)
      } else {
        val `type` = XMLConfigurationException.NOT_RECOGNIZED
        throw new XMLConfigurationException(`type`, featureId)
      }
    }
  }

  /**
   * Check a property. If the property is known and supported, this method
   * simply returns. Otherwise, the appropriate exception is thrown.
   *
   * @param propertyId The unique identifier (URI) of the property
   *                   being set.
   * @throws org.orbeon.darius.xml.xni.parser.XMLConfigurationException If the
   *            requested feature is not known.
   */
  protected def checkProperty(propertyId: String): Unit = {
    if (!fRecognizedProperties.contains(propertyId)) {
      if (fParentSettings ne null) {
        fParentSettings.getProperty(propertyId)
      } else {
        val `type` = XMLConfigurationException.NOT_RECOGNIZED
        throw new XMLConfigurationException(`type`, propertyId)
      }
    }
  }
}
