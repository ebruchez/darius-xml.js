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

import org.orbeon.apache.xerces.xni.XMLLocator
import org.orbeon.apache.xerces.xni.XNIException
import org.orbeon.apache.xerces.xni.XMLLocator

/**
 * A parsing exception. This exception is different from the standard
 * XNI exception in that it stores the location in the document (or
 * its entities) where the exception occurred.
 */
class XMLParseException(locator: XMLLocator, message: String, exception: Exception)
  extends XNIException(message, exception) {

  /**
   Public identifier.
   */
  protected var fPublicId: String = _

  /**
   literal System identifier.
   */
  protected var fLiteralSystemId: String = _

  /**
   expanded System identifier.
   */
  protected var fExpandedSystemId: String = _

  /**
   Base system identifier.
   */
  protected var fBaseSystemId: String = _

  /**
   Line number.
   */
  protected var fLineNumber: Int = -1

  /**
   Column number.
   */
  protected var fColumnNumber: Int = -1

  /**
   Character offset.
   */
  protected var fCharacterOffset: Int = -1

  if (locator ne null) {
    fPublicId = locator.getPublicId
    fLiteralSystemId = locator.getLiteralSystemId
    fExpandedSystemId = locator.getExpandedSystemId
    fBaseSystemId = locator.getBaseSystemId
    fLineNumber = locator.getLineNumber
    fColumnNumber = locator.getColumnNumber
    fCharacterOffset = locator.getCharacterOffset
  }

  /**
   Constructs a parse exception.
   */
  def this(locator: XMLLocator, message: String) =
    this(locator, message, null)

  /**
   Returns the public identifier.
   */
  def getPublicId: String = fPublicId

  /**
   Returns the expanded system identifier.
   */
  def getExpandedSystemId: String = fExpandedSystemId

  /**
   Returns the literal system identifier.
   */
  def getLiteralSystemId: String = fLiteralSystemId

  /**
   Returns the base system identifier.
   */
  def getBaseSystemId: String = fBaseSystemId

  /**
   Returns the line number.
   */
  def getLineNumber: Int = fLineNumber

  /**
   Returns the row number.
   */
  def getColumnNumber: Int = fColumnNumber

  /**
   Returns the character offset.
   */
  def getCharacterOffset: Int = fCharacterOffset

  /**
   Returns a string representation of this object.
   */
  override def toString: String = {
    val str = new StringBuffer()
    if (fPublicId ne null) {
      str.append(fPublicId)
    }
    str.append(':')
    if (fLiteralSystemId ne null) {
      str.append(fLiteralSystemId)
    }
    str.append(':')
    if (fExpandedSystemId ne null) {
      str.append(fExpandedSystemId)
    }
    str.append(':')
    if (fBaseSystemId ne null) {
      str.append(fBaseSystemId)
    }
    str.append(':')
    str.append(fLineNumber)
    str.append(':')
    str.append(fColumnNumber)
    str.append(':')
    str.append(fCharacterOffset)
    str.append(':')
    var message = getMessage
    if (message eq null) {
      val exception = getException
      if (exception ne null) {
        message = exception.getMessage
      }
    }
    if (message ne null) {
      str.append(message)
    }
    str.toString
  }
}
