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

package org.orbeon.apache.xerces.xni

/**
 * This class is used as a structure to pass text contained in the underlying
 * character buffer of the scanner. The offset and length fields allow the
 * buffer to be re-used without creating new character arrays.
 *
 * *Note:* Methods that are passed an XMLString structure
 * should consider the contents read-only and not make any modifications
 * to the contents of the buffer. The method receiving this structure
 * should also not modify the offset and length if this structure (or
 * the values of this structure) are passed to another method.
 *
 * *Note:* Methods that are passed an XMLString structure
 * are required to copy the information out of the buffer if it is to be
 * saved for use beyond the scope of the method. The contents of the
 * structure are volatile and the contents of the character buffer cannot
 * be assured once the method that is passed this structure returns.
 * Therefore, methods passed this structure should not save any reference
 * to the structure or the character array contained in the structure.
 */
class XMLString {

  /**
   The character array.
   */
  var ch: Array[Char] = _

  /**
   The offset into the character array.
   */
  var offset: Int = _

  /**
   The length of characters from the offset.
   */
  var length: Int = _

  /**
   * Constructs an XMLString structure preset with the specified
   * values.
   *
   * @param ch     The character array.
   * @param offset The offset into the character array.
   * @param length The length of characters from the offset.
   */
  def this(ch: Array[Char], offset: Int, length: Int) = {
    this()
    setValues(ch, offset, length)
  }

  /**
   * Constructs an XMLString structure with copies of the values in
   * the given structure.
   *
   * *Note:* This does not copy the character array;
   * only the reference to the array is copied.
   *
   * @param string The XMLString to copy.
   */
  def this(string: XMLString) = {
    this()
    setValues(string)
  }

  /**
   * Initializes the contents of the XMLString structure with the
   * specified values.
   *
   * @param ch     The character array.
   * @param offset The offset into the character array.
   * @param length The length of characters from the offset.
   */
  def setValues(ch: Array[Char], offset: Int, length: Int): Unit = {
    this.ch = ch
    this.offset = offset
    this.length = length
  }

  /**
   * Initializes the contents of the XMLString structure with copies
   * of the given string structure.
   *
   * *Note:* This does not copy the character array;
   * only the reference to the array is copied.
   */
  def setValues(s: XMLString): Unit = {
    setValues(s.ch, s.offset, s.length)
  }

  /**
   Resets all of the values to their defaults.
   */
  def clear(): Unit = {
    this.ch = null
    this.offset = 0
    this.length = -1
  }

  /**
   * Returns true if the contents of this XMLString structure and
   * the specified array are equal.
   *
   * @param ch     The character array.
   * @param offset The offset into the character array.
   * @param length The length of characters from the offset.
   */
  def equals(ch: Array[Char], offset: Int, length: Int): Boolean = {
    if (ch eq null) {
      return false
    }
    if (this.length != length) {
      return false
    }
    for (i <- 0 until length if this.ch(this.offset + i) != ch(offset + i)) {
      return false
    }
    true
  }

  /**
   * Returns true if the contents of this XMLString structure and
   * the specified string are equal.
   */
  def equals(s: String): Boolean = {
    if (s eq null) {
      return false
    }
    if (length != s.length) {
      return false
    }
    for (i <- 0 until length if ch(offset + i) != s.charAt(i)) {
      return false
    }
    true
  }

  override def toString: String = {
    if (length > 0) new String(ch, offset, length) else ""
  }
}
