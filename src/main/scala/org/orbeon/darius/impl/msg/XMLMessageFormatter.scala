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

import org.orbeon.darius.util.MessageFormatter

object XMLMessageFormatter {
  val XML_DOMAIN   = "http://www.w3.org/TR/1998/REC-xml-19980210"
  val XMLNS_DOMAIN = "http://www.w3.org/TR/1999/REC-xml-names-19990114"
}

class XMLMessageFormatter extends MessageFormatter {

  // @ebruchez: plain message for now, until we hook up English error messages
  def formatMessage(key: String, arguments: Array[Any]): String = {
    var msg: String = null
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
