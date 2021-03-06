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

package org.orbeon.apache.xerces.xni.parser

/**
 * The component manager manages a parser configuration and the components
 * that make up that configuration. The manager notifies each component
 * before parsing to allow the components to initialize their state; and
 * also any time that a parser feature or property changes.
 *
 * The methods of the component manager allow components to query features
 * and properties that affect the operation of the component.
 */
trait XMLComponentManager {

  /**
   * Returns the state of a feature.
   *
   * @param featureId The feature identifier.
   *
   * @throws XMLConfigurationException Thrown on configuration error.
   */
  def getFeature(featureId: String): Boolean

  /**
   * Returns the value of a property.
   *
   * @param propertyId The property identifier.
   *
   * @throws XMLConfigurationException Thrown on configuration error.
   */
  def getProperty(propertyId: String): Any
}
