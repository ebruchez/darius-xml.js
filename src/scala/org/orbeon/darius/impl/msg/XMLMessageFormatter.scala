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

package org.orbeon.darius.impl.msg

import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle

import org.orbeon.darius.util.MessageFormatter

object XMLMessageFormatter {

  /**
   * The domain of messages concerning the XML 1.0 specification.
   */
  val XML_DOMAIN = "http://www.w3.org/TR/1998/REC-xml-19980210"

  val XMLNS_DOMAIN = "http://www.w3.org/TR/1999/REC-xml-names-19990114"
}

/**
 * XMLMessageFormatter provides error messages for the XML 1.0 Recommendation and for
 * the Namespaces Recommendation
 */
class XMLMessageFormatter extends MessageFormatter {

  private var fLocale: Locale = null

  private var fResourceBundle: ResourceBundle = null

  /**
   * Formats a message with the specified arguments using the given
   * locale information.
   *
   * @param _locale    The locale of the message.
   * @param key       The message key.
   * @param arguments The message replacement text arguments. The order
   *                  of the arguments must match that of the placeholders
   *                  in the actual message.
   *
   * @return Returns the formatted message.
   *
   * @throws MissingResourceException Thrown if the message with the
   *                                  specified key cannot be found.
   */
  def formatMessage(_locale: Locale, key: String, arguments: Array[Any]): String = {
    var locale = _locale
    if (locale eq null) {
      locale = Locale.getDefault
    }
    if (locale != fLocale) {
      fResourceBundle = ResourceBundle.getBundle("org.orbeon.darius.impl.msg.XMLMessages", locale)
      fLocale = locale
    }
    var msg: String = null
    try {
      msg = fResourceBundle.getString(key)
      if (arguments ne null) {
        try {
          msg = java.text.MessageFormat.format(msg, arguments)
        } catch {
          case e: Exception ⇒
            msg = fResourceBundle.getString("FormatFailed")
            msg += " " + fResourceBundle.getString(key)
        }
      }
    } catch {
      case e: MissingResourceException ⇒
        msg = fResourceBundle.getString("BadMessageKey")
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
