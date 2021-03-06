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

package org.orbeon.apache.xerces.xni.grammars

import org.orbeon.apache.xerces.xni.QName
import org.orbeon.apache.xerces.xni.XMLAttributes

object XMLSchemaDescription {

  /**
   * Indicate that the current schema document is <include>d by another
   * schema document.
   */
  val CONTEXT_INCLUDE: Short = 0

  /**
   * Indicate that the current schema document is <redefine>d by another
   * schema document.
   */
  val CONTEXT_REDEFINE: Short = 1

  /**
   * Indicate that the current schema document is <import>ed by another
   * schema document.
   */
  val CONTEXT_IMPORT: Short = 2

  /**
   * Indicate that the current schema document is being preparsed.
   */
  val CONTEXT_PREPARSE: Short = 3

  /**
   * Indicate that the parse of the current schema document is triggered
   * by xsi:schemaLocation/noNamespaceSchemaLocation attribute(s) in the
   * instance document. This value is only used if we don't defer the loading
   * of schema documents.
   */
  val CONTEXT_INSTANCE: Short = 4

  /**
   * Indicate that the parse of the current schema document is triggered by
   * the occurrence of an element whose namespace is the target namespace
   * of this schema document. This value is only used if we do defer the
   * loading of schema documents until a component from that namespace is
   * referenced from the instance.
   */
  val CONTEXT_ELEMENT: Short = 5

  /**
   * Indicate that the parse of the current schema document is triggered by
   * the occurrence of an attribute whose namespace is the target namespace
   * of this schema document. This value is only used if we do defer the
   * loading of schema documents until a component from that namespace is
   * referenced from the instance.
   */
  val CONTEXT_ATTRIBUTE: Short = 6

  /**
   * Indicate that the parse of the current schema document is triggered by
   * the occurrence of an "xsi:type" attribute, whose value (a QName) has
   * the target namespace of this schema document as its namespace.
   * This value is only used if we do defer the loading of schema documents
   * until a component from that namespace is referenced from the instance.
   */
  val CONTEXT_XSITYPE: Short = 7
}

/**
 * All information specific to XML Schema grammars.
 */
trait XMLSchemaDescription extends XMLGrammarDescription {

  /**
   * Get the context. The returned value is one of the pre-defined
   * CONTEXT_xxx constants.
   *
   * @return  the value indicating the context
   */
  def getContextType: Short

  /**
   * If the context is "include" or "redefine", then return the target
   * namespace of the enclosing schema document; otherwise, the expected
   * target namespace of this document.
   *
   * @return  the expected/enclosing target namespace
   */
  def getTargetNamespace: String

  /**
   * For import and references from the instance document, it's possible to
   * have multiple hints for one namespace. So this method returns an array,
   * which contains all location hints.
   *
   * @return  an array of all location hints associated to the expected
   *          target namespace
   */
  def getLocationHints: Array[String]

  /**
   * If a call is triggered by an element/attribute/xsi:type in the instance,
   * this call returns the name of such triggering component: the name of
   * the element/attribute, or the value of the xsi:type.
   *
   * @return  the name of the triggering component
   */
  def getTriggeringComponent: QName

  /**
   * If a call is triggered by an attribute or xsi:type, then this method
   * returns the enclosing element of such element.
   *
   * @return  the name of the enclosing element
   */
  def getEnclosingElementName: QName

  /**
   * If a call is triggered by an element/attribute/xsi:type in the instance,
   * this call returns all attributes of such an element (or enclosing element).
   *
   * @return  all attributes of the triggering/enclosing element
   */
  def getAttributes: XMLAttributes
}
