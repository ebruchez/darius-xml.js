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

package org.orbeon.darius.xni

/**
 *  This represents the basic physical description of the location of any
 * XML resource (a Schema grammar, a DTD, a general entity etc.) 
 */
trait XMLResourceIdentifier {

  /**
   Sets the public identifier.
   */
  def setPublicId(publicId: String): Unit

  /**
   Returns the public identifier.
   */
  def getPublicId: String

  /**
   Sets the expanded system identifier.
   */
  def setExpandedSystemId(systemId: String): Unit

  /**
   Returns the expanded system identifier.
   */
  def getExpandedSystemId: String

  /**
   Sets the literal system identifier.
   */
  def setLiteralSystemId(systemId: String): Unit

  /**
   Returns the literal system identifier.
   */
  def getLiteralSystemId: String

  /**
   Sets the base URI against which the literal SystemId is to be
   resolved.
   */
  def setBaseSystemId(systemId: String): Unit

  /**
    Returns the base URI against which the literal SystemId is to be
   resolved. 
   */
  def getBaseSystemId: String

  /**
   Sets the namespace of the resource.
   */
  def setNamespace(namespace: String): Unit

  /**
   Returns the namespace of the resource.
   */
  def getNamespace: String
}
