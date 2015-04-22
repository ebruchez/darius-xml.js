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

package org.orbeon.darius.xni.parser

import java.io.InputStream
import java.io.Reader

import org.orbeon.darius.xni.XMLResourceIdentifier

/**
 * This class represents an input source for an XML document. The
 * basic properties of an input source are the following:
 * 
 *  - public identifier
 *  - system identifier
 *  - byte stream or character stream
 *  - 
 * 
 */
class XMLInputSource(
  protected var fPublicId     : String, 
  protected var fSystemId     : String, 
  protected var fBaseSystemId : String
) {

  /**
   Byte stream.
   */
  protected var fByteStream: InputStream = _

  /**
   Character stream.
   */
  protected var fCharStream: Reader = _

  /**
   Encoding.
   */
  protected var fEncoding: String = _

  /**
   * Constructs an input source from a XMLResourceIdentifier
   * object, leaving resolution of the entity and opening of
   * the input stream up to the caller.
   *
   * @param resourceIdentifier    the XMLResourceIdentifier containing the information
   */
  def this(resourceIdentifier: XMLResourceIdentifier) =
    this(
      fPublicId     = resourceIdentifier.getPublicId,
      fSystemId     = resourceIdentifier.getLiteralSystemId,
      fBaseSystemId = resourceIdentifier.getBaseSystemId
    )

  /**
   * Constructs an input source from a byte stream.
   *
   * @param publicId     The public identifier, if known.
   * @param systemId     The system identifier. This value should
   *                     always be set, if possible, and can be
   *                     relative or absolute. If the system identifier
   *                     is relative, then the base system identifier
   *                     should be set.
   * @param baseSystemId The base system identifier. This value should
   *                     always be set to the fully expanded URI of the
   *                     base system identifier, if possible.
   * @param byteStream   The byte stream.
   * @param encoding     The encoding of the byte stream, if known.
   */
  def this(publicId: String, 
      systemId: String, 
      baseSystemId: String, 
      byteStream: InputStream, 
      encoding: String) {
    this(
      fPublicId     = publicId,
      fSystemId     = systemId,
      fBaseSystemId = baseSystemId
    )
    fByteStream = byteStream
    fEncoding = encoding
  }

  /**
   * Constructs an input source from a character stream.
   *
   * @param publicId     The public identifier, if known.
   * @param systemId     The system identifier. This value should
   *                     always be set, if possible, and can be
   *                     relative or absolute. If the system identifier
   *                     is relative, then the base system identifier
   *                     should be set.
   * @param baseSystemId The base system identifier. This value should
   *                     always be set to the fully expanded URI of the
   *                     base system identifier, if possible.
   * @param charStream   The character stream.
   * @param encoding     The original encoding of the byte stream
   *                     used by the reader, if known.
   */
  def this(publicId: String, 
      systemId: String, 
      baseSystemId: String, 
      charStream: Reader, 
      encoding: String) {
    this(
      fPublicId     = publicId,
      fSystemId     = systemId,
      fBaseSystemId = baseSystemId
    )
    fCharStream = charStream
    fEncoding = encoding
  }

  /**
   * Sets the public identifier.
   *
   * @param publicId The new public identifier.
   */
  def setPublicId(publicId: String): Unit = {
    fPublicId = publicId
  }

  /**
   Returns the public identifier.
   */
  def getPublicId: String = fPublicId

  /**
   * Sets the system identifier.
   *
   * @param systemId The new system identifier.
   */
  def setSystemId(systemId: String): Unit = {
    fSystemId = systemId
  }

  /**
   Returns the system identifier.
   */
  def getSystemId: String = fSystemId

  /**
   * Sets the base system identifier.
   *
   * @param baseSystemId The new base system identifier.
   */
  def setBaseSystemId(baseSystemId: String): Unit = {
    fBaseSystemId = baseSystemId
  }

  /**
   Returns the base system identifier.
   */
  def getBaseSystemId: String = fBaseSystemId

  /**
   * Sets the byte stream. If the byte stream is not already opened
   * when this object is instantiated, then the code that opens the
   * stream should also set the byte stream on this object. Also, if
   * the encoding is auto-detected, then the encoding should also be
   * set on this object.
   *
   * @param byteStream The new byte stream.
   */
  def setByteStream(byteStream: InputStream): Unit = {
    fByteStream = byteStream
  }

  /**
   Returns the byte stream.
   */
  def getByteStream: InputStream = fByteStream

  /**
   * Sets the character stream. If the character stream is not already
   * opened when this object is instantiated, then the code that opens
   * the stream should also set the character stream on this object.
   * Also, the encoding of the byte stream used by the reader should
   * also be set on this object, if known.
   */
  def setCharacterStream(charStream: Reader): Unit = {
    fCharStream = charStream
  }

  /**
   Returns the character stream.
   */
  def getCharacterStream: Reader = fCharStream

  /**
   * Sets the encoding of the stream.
   *
   * @param encoding The new encoding.
   */
  def setEncoding(encoding: String): Unit = {
    fEncoding = encoding
  }

  /**
   Returns the encoding of the stream, or null if not known.
   */
  def getEncoding: String = fEncoding
}
