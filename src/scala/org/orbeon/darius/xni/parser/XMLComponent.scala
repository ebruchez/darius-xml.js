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

package org.orbeon.darius.xni.parser

import org.orbeon.darius.xni.XNIException

/**
 * The component interface defines methods that must be implemented
 * by components in a parser configuration. The component methods allow
 * the component manager to initialize the component state and notify
 * the component when feature and property values change.
 *
 * @see XMLComponentManager
 */
trait XMLComponent {

  /**
   * Resets the component. The component can query the component manager
   * about any features and properties that affect the operation of the
   * component.
   *
   * @param componentManager The component manager.
   *
   * @throws XNIException Thrown by component on initialization error.
   */
  def reset(componentManager: XMLComponentManager): Unit

  /**
   * Returns a list of feature identifiers that are recognized by
   * this component. This method may return null if no features
   * are recognized by this component.
   */
  def getRecognizedFeatures: Array[String]

  /**
   * Sets the state of a feature. This method is called by the component
   * manager any time after reset when a feature changes state.
   * 
   * *Note:* Components should silently ignore features
   * that do not affect the operation of the component.
   *
   * @param featureId The feature identifier.
   * @param state     The state of the feature.
   *
   * @throws XMLConfigurationException Thrown for configuration error.
   *                                   In general, components should
   *                                   only throw this exception if
   *                                   it is *really*
   *                                   a critical error.
   */
  def setFeature(featureId: String, state: Boolean): Unit

  /**
   * Returns a list of property identifiers that are recognized by
   * this component. This method may return null if no properties
   * are recognized by this component.
   */
  def getRecognizedProperties: Array[String]

  /**
   * Sets the value of a property. This method is called by the component
   * manager any time after reset when a property changes value.
   * 
   * *Note:* Components should silently ignore properties
   * that do not affect the operation of the component.
   *
   * @param propertyId The property identifier.
   * @param value      The value of the property.
   *
   * @throws XMLConfigurationException Thrown for configuration error.
   *                                   In general, components should
   *                                   only throw this exception if
   *                                   it is *really*
   *                                   a critical error.
   */
  def setProperty(propertyId: String, value: AnyRef): Unit

  /**
   * Returns the default state for a feature, or null if this
   * component does not want to report a default value for this
   * feature.
   *
   * @param featureId The feature identifier.
   *
   * @since Xerces 2.2.0
   */
  def getFeatureDefault(featureId: String): java.lang.Boolean

  /**
   * Returns the default state for a property, or null if this
   * component does not want to report a default value for this
   * property.
   *
   * @param propertyId The property identifier.
   *
   * @since Xerces 2.2.0
   */
  def getPropertyDefault(propertyId: String): AnyRef
}
