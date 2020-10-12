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

package org.orbeon.darius.xml.xni

/**
 * A structure that holds the components of an XML Namespaces qualified
 * name.
 *
 * To be used correctly, the strings must be identical references for
 * equal strings. Within the parser, these values are considered symbols
 * and should always be retrieved from the `SymbolTable`.
 */
class QName extends Cloneable {

  /**
   * The qname prefix. For example, the prefix for the qname "a:foo"
   * is "a".
   */
  var prefix: String = _

  /**
   * The qname localpart. For example, the localpart for the qname "a:foo"
   * is "foo".
   */
  var localpart: String = _

  /**
   * The qname rawname. For example, the rawname for the qname "a:foo"
   * is "a:foo".
   */
  var rawname: String = _

  /**
   * The URI to which the qname prefix is bound. This binding must be
   * performed by a XML Namespaces aware processor.
   */
  var uri: String = _

  clear()

  /**
   Constructs a QName with the specified values.
   */
  def this(
    prefix    : String,
    localpart : String,
    rawname   : String,
    uri       : String) = {
    this()
    setValues(prefix, localpart, rawname, uri)
  }

  /**
   Constructs a copy of the specified QName.
   */
  def this(qname: QName) = {
    this()
    setValues(qname)
  }

  /**
   * Convenience method to set the values of the qname components.
   *
   * @param qname The qualified name to be copied.
   */
  def setValues(qname: QName): Unit = {
    prefix    = qname.prefix
    localpart = qname.localpart
    rawname   = qname.rawname
    uri       = qname.uri
  }

  /**
   * Convenience method to set the values of the qname components.
   *
   * @param prefix    The qname prefix. (e.g. "a")
   * @param localpart The qname localpart. (e.g. "foo")
   * @param rawname   The qname rawname. (e.g. "a:foo")
   * @param uri       The URI binding. (e.g. "http://foo.com/mybinding")
   */
  def setValues(
    prefix    : String,
    localpart : String,
    rawname   : String,
    uri       : String
  ): Unit = {
    this.prefix    = prefix
    this.localpart = localpart
    this.rawname   = rawname
    this.uri       = uri
  }

  /**
   Clears the values of the qname components.
   */
  def clear(): Unit = {
    prefix    = null
    localpart = null
    rawname   = null
    uri       = null
  }

  /**
   Returns a clone of this object.
   */
  override def clone(): AnyRef = new QName(this)

  /**
   Returns the hashcode for this object.
   */
  override def hashCode(): Int =
    if (uri ne null)
      uri.hashCode + (if (localpart ne null) localpart.hashCode else 0)
    else
      if (rawname ne null) rawname.hashCode else 0

  /**
   Returns true if the two objects are equal.
   */
  override def equals(other: Any): Boolean =
    other match {
      case qname: QName if qname.uri ne null =>
        uri == qname.uri && localpart == qname.localpart
      case qname: QName =>
        rawname == qname.rawname
      case _ =>
        false
    }

  /**
   Returns a string representation of this object.
   */
  override def toString: String = {
    val str = new StringBuffer()
    var comma = false
    if (prefix ne null) {
      str.append("prefix=\"").append(prefix).append('"')
      comma = true
    }
    if (localpart ne null) {
      if (comma) {
        str.append(',')
      }
      str.append("localpart=\"").append(localpart).append('"')
      comma = true
    }
    if (rawname ne null) {
      if (comma) {
        str.append(',')
      }
      str.append("rawname=\"").append(rawname).append('"')
      comma = true
    }
    if (uri ne null) {
      if (comma) {
        str.append(',')
      }
      str.append("uri=\"").append(uri).append('"')
    }
    str.toString
  }
}
