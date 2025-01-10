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
package org.orbeon.apache.xerces.util

import org.orbeon.apache.xerces.xni.{XMLResourceIdentifier, XNIException}
import org.orbeon.apache.xerces.xni.parser.{XMLEntityResolver, XMLInputSource}
import org.xml.sax.{EntityResolver, SAXException}

import java.io.IOException


/**
 * This class wraps a SAX entity resolver in an XNI entity resolver.
 *
 * @see EntityResolver
 * @author Andy Clark, IBM
 * @version $Id: EntityResolverWrapper.java 699892 2008-09-28 21:08:27Z mrglavas $
 */
class EntityResolverWrapper extends XMLEntityResolver {

  /** The SAX entity resolver. */
  protected var fEntityResolver: EntityResolver = null

  /** Wraps the specified SAX entity resolver. */
  def this(entityResolver: EntityResolver) = {
    this()
    setEntityResolver(entityResolver)
  }

  /** Sets the SAX entity resolver. */
  def setEntityResolver(entityResolver: EntityResolver): Unit = fEntityResolver = entityResolver

  /** Returns the SAX entity resolver. */
  def getEntityResolver: EntityResolver = fEntityResolver

  /**
   * Resolves an external parsed entity. If the entity cannot be
   * resolved, this method should return null.
   *
   * @param resourceIdentifier contains the physical co-ordinates of the resource to be resolved
   * @throws XNIException Thrown on general error.
   * @throws IOException  Thrown if resolved entity stream cannot be
   *                      opened or some other i/o error occurs.
   */
  def resolveEntity(resourceIdentifier: XMLResourceIdentifier): XMLInputSource = {
    // When both pubId and sysId are null, the user's entity resolver
    // can do nothing about it. We'd better not bother calling it.
    // This happens when the resourceIdentifier is a GrammarDescription,
    // which describes a schema grammar of some namespace, but without
    // any schema location hint. -Sg
    val pubId = resourceIdentifier.getPublicId
    val sysId = resourceIdentifier.getExpandedSystemId
    if (pubId == null && sysId == null)
      return null
    // resolve entity using SAX entity resolver
    if (fEntityResolver != null && resourceIdentifier != null) try {
      val inputSource = fEntityResolver.resolveEntity(pubId, sysId)
      if (inputSource != null) {
        val publicId       = inputSource.getPublicId
        val systemId       = inputSource.getSystemId
        val baseSystemId   = resourceIdentifier.getBaseSystemId
        val byteStream     = inputSource.getByteStream
        val charStream     = inputSource.getCharacterStream
        val encoding       = inputSource.getEncoding
        val xmlInputSource = new XMLInputSource(publicId, systemId, baseSystemId)
        xmlInputSource.setByteStream(byteStream)
        xmlInputSource.setCharacterStream(charStream)
        xmlInputSource.setEncoding(encoding)
        return xmlInputSource
      }
    } catch {
      // error resolving entity
      case e: SAXException =>
        var ex = e.getException
        if (ex == null)
          ex = e
        throw new XNIException(ex)
    }
    // unable to resolve entity
    null
  }
}