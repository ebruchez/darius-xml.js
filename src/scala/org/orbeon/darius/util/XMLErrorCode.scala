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

/**
 * A structure that represents an error code, characterized by
 * a domain and a message key.
 */
class XMLErrorCode(var fDomain: String, var fKey: String) {

  /**
   * Convenience method to set the values of an XMLErrorCode.
   *
   * @param domain The error domain.
   * @param key The key of the error message.
   */
  def setValues(domain: String, key: String): Unit = {
    fDomain = domain
    fKey = key
  }

  /**
   * Indicates whether some other object is equal to this XMLErrorCode.
   *
   * @param obj the object with which to compare.
   */
  override def equals(obj: Any): Boolean = {
    if (! obj.isInstanceOf[XMLErrorCode]) // @ebruchez: is this correct?
      return false
    val err = obj.asInstanceOf[XMLErrorCode]
    fDomain == err.fDomain && fKey == err.fKey
  }

  override def hashCode(): Int = fDomain.hashCode + fKey.hashCode
}
