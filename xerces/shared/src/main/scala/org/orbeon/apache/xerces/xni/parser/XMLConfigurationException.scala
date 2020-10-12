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

import org.orbeon.apache.xerces.xni.XNIException

object XMLConfigurationException {

  /**
   Exception type: identifier not recognized.
   */
  val NOT_RECOGNIZED: Short = 0

  /**
   Exception type: identifier not supported.
   */
  val NOT_SUPPORTED: Short = 1
}

/**
 * An XNI parser configuration exception. This exception class extends
 * `XNIException` in order to differentiate between general
 * parsing errors and configuration errors.
 */
class XMLConfigurationException(fType: Short, fIdentifier: String)
    extends XNIException(fIdentifier) {

  /**
   * Returns the exception type.
   */
  def getType: Short = fType

  /**
   Returns the feature or property identifier.
   */
  def getIdentifier: String = fIdentifier
}
