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

import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle

object DatatypeMessageFormatter {

  private val BASE_NAME = "org.orbeon.darius.impl.msg.DatatypeMessages"

  /**
   * Formats a message with the specified arguments using the given
   * locale information.
   *
   * @param _locale   The locale of the message.
   * @param key       The message key.
   * @param arguments The message replacement text arguments. The order
   *                  of the arguments must match that of the placeholders
   *                  in the actual message.
   *
   * @return          the formatted message.
   *
   * @throws MissingResourceException Thrown if the message with the
   *                                  specified key cannot be found.
   */
  def formatMessage(_locale: Locale, key: String, arguments: Array[Any]): String = {
    var locale = _locale
    if (locale eq null) {
      locale = Locale.getDefault
    }
    val resourceBundle = ResourceBundle.getBundle(BASE_NAME, locale)
    var msg: String = null
    try {
      msg = resourceBundle.getString(key)
      if (arguments ne null) {
        try {
          msg = java.text.MessageFormat.format(msg, arguments)
        } catch {
          case e: Exception ⇒
            msg = resourceBundle.getString("FormatFailed")
            msg += " " + resourceBundle.getString(key)
        }
      }
    } catch {
      case e: MissingResourceException ⇒
        msg = resourceBundle.getString("BadMessageKey")
        throw new MissingResourceException(key, msg, key)
    }
    if (msg eq null) {
      msg = key
      if (arguments.length > 0) {
        val str = new StringBuffer(msg)
        str.append('?')
        for (i ← arguments.indices) {
          if (i > 0) {
            str.append('&')
          }
          str.append(String.valueOf(arguments(i)))
        }
      }
    }
    msg
  }
}
