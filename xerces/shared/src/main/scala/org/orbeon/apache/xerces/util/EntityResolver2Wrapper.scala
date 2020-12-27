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

import org.orbeon.apache.xerces.impl.{ExternalSubsetResolver, XMLEntityDescription}
import org.orbeon.apache.xerces.xni.{XMLResourceIdentifier, XNIException}
import org.orbeon.apache.xerces.xni.grammars.XMLDTDDescription
import org.orbeon.apache.xerces.xni.parser.XMLInputSource
import org.xml.sax.{InputSource, SAXException}
import org.xml.sax.ext.EntityResolver2

import java.io.IOException


/**
 * <p>This class wraps a SAX entity resolver (EntityResolver2) in an XNI entity resolver.</p>
 *
 * @author Michael Glavassevich, IBM
 * @version $Id: EntityResolver2Wrapper.java 699892 2008-09-28 21:08:27Z mrglavas $
 */
class EntityResolver2Wrapper extends ExternalSubsetResolver {

  /** An instance of SAX2 Extensions 1.1's EntityResolver2. */
  protected var fEntityResolver: EntityResolver2 = null

  /**
   * <p>Creates a new instance wrapping the given SAX entity resolver.</p>
   *
   * @param entityResolver the SAX entity resolver to wrap
   */
  def this(entityResolver: EntityResolver2) {
    this()
    setEntityResolver(entityResolver)
  }

  /**
   * <p>Sets the SAX entity resolver wrapped by this object.</p>
   *
   * @param entityResolver the SAX entity resolver to wrap
   */
  def setEntityResolver(entityResolver: EntityResolver2): Unit = fEntityResolver = entityResolver

  /**
   * <p>Returns the SAX entity resolver wrapped by this object.</p>
   *
   * @return the SAX entity resolver wrapped by this object
   */
  def getEntityResolver: EntityResolver2 = fEntityResolver

  /**
   * <p>Locates an external subset for documents which do not explicitly
   * provide one. If no external subset is provided, this method should
   * return <code>null</code>.</p>
   *
   * @param grammarDescription a description of the DTD
   * @throws XNIException Thrown on general error.
   * @throws IOException  Thrown if resolved entity stream cannot be
   *                      opened or some other i/o error occurs.
   */
  def getExternalSubset(grammarDescription: XMLDTDDescription): XMLInputSource =
    if (fEntityResolver != null) {
      val name    = grammarDescription.getRootName
      val baseURI = grammarDescription.getBaseSystemId
      // Resolve using EntityResolver2
      try {
        val inputSource = fEntityResolver.getExternalSubset(name, baseURI)
        if (inputSource != null)
          createXMLInputSource(inputSource, baseURI)
        else
          null
      } catch {
            // error resolving external subset
        case e: SAXException =>
          var ex = e.getException
          if (ex == null) ex = e
          throw new XNIException(ex)
      }
    } else {
      // unable to resolve external subset
      null
    }

  /**
   * Resolves an external parsed entity. If the entity cannot be
   * resolved, this method should return null.
   *
   * @param resourceIdentifier contains the physical co-ordinates of the resource to be resolved
   * @throws XNIException Thrown on general error.
   * @throws IOException  Thrown if resolved entity stream cannot be
   *                      opened or some other i/o error occurs.
   */
  def resolveEntity(resourceIdentifier: XMLResourceIdentifier): XMLInputSource =
    if (fEntityResolver != null) {
      val pubId   = resourceIdentifier.getPublicId
      val sysId   = resourceIdentifier.getLiteralSystemId
      val baseURI = resourceIdentifier.getBaseSystemId
      var name: String = null
      resourceIdentifier match {
        case _: XMLDTDDescription              => name = "[dtd]"
        case description: XMLEntityDescription => name = description.getEntityName
        case _                                 =>
      }
      // When both pubId and sysId are null, the user's entity resolver
      // can do nothing about it. We'd better not bother calling it.
      // This happens when the resourceIdentifier is a GrammarDescription,
      // which describes a schema grammar of some namespace, but without
      // any schema location hint. -Sg
      if (pubId == null && sysId == null) return null
      try {
        val inputSource = fEntityResolver.resolveEntity(name, pubId, baseURI, sysId)
        if (inputSource != null)
          createXMLInputSource(inputSource, baseURI)
        else
          null
      } catch {
        case e: SAXException =>
          var ex = e.getException
          if (ex == null)
            ex = e
          throw new XNIException(ex)
      } // error resolving entity
    } else {
      // unable to resolve entity
      null
    }

  /**
   * Creates an XMLInputSource from a SAX InputSource.
   */
  private def createXMLInputSource(source: InputSource, baseURI: String) = {
    val publicId       = source.getPublicId
    val systemId       = source.getSystemId
    val baseSystemId   = baseURI
    val byteStream     = source.getByteStream
    val charStream     = source.getCharacterStream
    val encoding       = source.getEncoding
    val xmlInputSource = new XMLInputSource(publicId, systemId, baseSystemId)
    xmlInputSource.setByteStream(byteStream)
    xmlInputSource.setCharacterStream(charStream)
    xmlInputSource.setEncoding(encoding)
    xmlInputSource
  }
}