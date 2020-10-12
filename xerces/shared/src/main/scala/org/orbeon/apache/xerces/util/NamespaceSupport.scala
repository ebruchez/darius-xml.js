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

import org.orbeon.apache.xerces.xni.NamespaceContext

import scala.util.control.Breaks

/**
 * Namespace support for XML document handlers. This class doesn't
 * perform any error checking and assumes that all strings passed
 * as arguments to methods are unique symbols. The SymbolTable class
 * can be used for this purpose.
 */
class NamespaceSupport extends NamespaceContext {

  /**
   * Namespace binding information. This array is composed of a
   * series of tuples containing the namespace binding information:
   * <prefix, uri>. The default size can be set to anything
   * as long as it is a power of 2 greater than 1.
   */
  protected var fNamespace: Array[String] = new Array[String](16 * 2)

  /**
   The top of the namespace information array.
   */
  protected var fNamespaceSize: Int = _

  /**
   * Context indexes. This array contains indexes into the namespace
   * information array. The index at the current context is the start
   * index of declared namespace bindings and runs to the size of the
   * namespace information array.
   */
  protected var fContext: Array[Int] = new Array[Int](8)

  /**
   The current context.
   */
  protected var fCurrentContext: Int = _

  protected var fPrefixes: Array[String] = new Array[String](16)

  /**
   * Constructs a namespace context object and initializes it with
   * the prefixes declared in the specified context.
   */
  def this(context: NamespaceContext) = {
    this()
    pushContext()
    for {
      prefix <- context.getAllPrefixes
      uri = context.getURI(prefix)
    } locally {
      declarePrefix(prefix, uri)
    }
  }

  def reset(): Unit = {
    fNamespaceSize = 0
    fCurrentContext = 0
    fContext(fCurrentContext) = fNamespaceSize
    fNamespace(fNamespaceSize) = XMLSymbols.PREFIX_XML
    fNamespaceSize += 1
    fNamespace(fNamespaceSize) = NamespaceContext.XML_URI
    fNamespaceSize += 1
    fNamespace(fNamespaceSize) = XMLSymbols.PREFIX_XMLNS
    fNamespaceSize += 1
    fNamespace(fNamespaceSize) = NamespaceContext.XMLNS_URI
    fNamespaceSize += 1
    fCurrentContext += 1
  }

  def pushContext(): Unit = {
    if (fCurrentContext + 1 == fContext.length) {
      val contextarray = new Array[Int](fContext.length * 2)
      System.arraycopy(fContext, 0, contextarray, 0, fContext.length)
      fContext = contextarray
    }
    fCurrentContext += 1
    fContext(fCurrentContext) = fNamespaceSize
  }

  def popContext(): Unit = {
    fNamespaceSize = fContext(fCurrentContext)
    fCurrentContext -= 1
  }

  def declarePrefix(prefix: String, uri: String): Boolean = {
    if (prefix == XMLSymbols.PREFIX_XML || prefix == XMLSymbols.PREFIX_XMLNS) {
      return false
    }
    var i = fNamespaceSize
    while (i > fContext(fCurrentContext)) {
      if (fNamespace(i - 2) == prefix) {
        fNamespace(i - 1) = uri
        return true
      }
      i -= 2
    }
    if (fNamespaceSize == fNamespace.length) {
      val namespacearray = new Array[String](fNamespaceSize * 2)
      System.arraycopy(fNamespace, 0, namespacearray, 0, fNamespaceSize)
      fNamespace = namespacearray
    }
    fNamespace(fNamespaceSize) = prefix
    fNamespaceSize += 1
    fNamespace(fNamespaceSize) = uri
    fNamespaceSize += 1
    true
  }

  def getURI(prefix: String): String = {
    var i = fNamespaceSize
    while (i > 0) {
      if (fNamespace(i - 2) == prefix) {
        return fNamespace(i - 1)
      }
      i -= 2
    }
    null
  }

  def getPrefix(uri: String): String = {
    var i = fNamespaceSize
    while (i > 0) {
      if (fNamespace(i - 1) == uri) {
        if (getURI(fNamespace(i - 2)) == uri) return fNamespace(i - 2)
      }
      i -= 2
    }
    null
  }

  def getDeclaredPrefixCount: Int = {
    (fNamespaceSize - fContext(fCurrentContext)) / 2
  }

  def getDeclaredPrefixAt(index: Int): String = {
    fNamespace(fContext(fCurrentContext) + index * 2)
  }

  def getAllPrefixes: Iterator[String] = {
    var count = 0
    if (fPrefixes.length < (fNamespace.length / 2)) {
      val prefixes = new Array[String](fNamespaceSize)
      fPrefixes = prefixes
    }
    var prefix: String = null
    var unique = true
    var i = 2
    val whileBreaks = new Breaks
    whileBreaks.breakable {
      while (i < (fNamespaceSize - 2)) {
        prefix = fNamespace(i + 2)
        for (k <- 0 until count if fPrefixes(k) == prefix) {
          unique = false
          whileBreaks.break()
        }
        if (unique) {
          fPrefixes(count) = prefix
          count += 1
        }
        unique = true
        i += 2
      }
    }
    fPrefixes.iterator.take(count)
  }

  /**
   * Checks whether a binding or unbinding for
   * the given prefix exists in the context.
   *
   * @param prefix The prefix to look up.
   *
   * @return true if the given prefix exists in the context
   */
  def containsPrefix(prefix: String): Boolean = {
    var i = fNamespaceSize
    while (i > 0) {
      if (fNamespace(i - 2) == prefix) {
        return true
      }
      i -= 2
    }
    false
  }
}
