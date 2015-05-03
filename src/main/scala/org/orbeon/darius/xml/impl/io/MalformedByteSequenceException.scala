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

package org.orbeon.darius.xml.impl.io

import org.orbeon.darius.xml.util.MessageFormatter

/**
 * Signals that a malformed byte sequence was detected
 * by a `java.io.Reader` that decodes bytes
 * of a given encoding into characters.
 */
class MalformedByteSequenceException(
  var fFormatter : MessageFormatter, 
  var fDomain    : String, 
  var fKey       : String, 
  var fArguments : Array[Any]
) extends Exception {

  /**
   message text for this message, initially null *
   */
  private var fMessage: String = _

  /**
   * Returns the error domain of the error message.
   *
   * @return the error domain
   */
  def getDomain: String = fDomain

  /**
   * Returns the key of the error message.
   *
   * @return the error key of the error message
   */
  def getKey: String = fKey

  /**
   * Returns the replacement arguments for the error
   * message or `null` if none exist.
   *
   * @return the replacement arguments for the error message
   * or `null` if none exist
   */
  def getArguments: Array[Any] = fArguments

  /**
   * Returns the localized message for this exception.
   *
   * @return the localized message for this exception.
   */
  override def getMessage: String = {
    synchronized {
      if (fMessage eq null) {
        fMessage = fFormatter.formatMessage(fKey, fArguments)
        fFormatter = null
      }
      fMessage
    }
  }
}
