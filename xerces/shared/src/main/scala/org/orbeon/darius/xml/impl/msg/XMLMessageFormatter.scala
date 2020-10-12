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

package org.orbeon.darius.xml.impl.msg

import org.orbeon.darius.xml.util.MessageFormatter

object XMLMessageFormatter {
  val XML_DOMAIN   = "http://www.w3.org/TR/1998/REC-xml-19980210"
  val XMLNS_DOMAIN = "http://www.w3.org/TR/1999/REC-xml-names-19990114"
}

class XMLMessageFormatter extends MessageFormatter {

  def formatMessage(key: String, argsOrNull: Array[Any]): String = {
    
    XMLMessages.Messages.get(key) match {
      case Some(message) ⇒
        
        var res = message
        
        for ((value, index) ← Option(argsOrNull).to[List] flatMap { _ map String.valueOf } zipWithIndex)
          res = res.replaceAllLiterally(s"{$index}", value)
        
        res
        
      case None ⇒
        val str = new StringBuffer(key)
        Option(argsOrNull) filter (_.nonEmpty) foreach { args ⇒
          str.append('?')
          str.append(args map String.valueOf mkString "&")
        } 
        str.toString
    }
  }
}
