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

/**
 * All internalized xml symbols. They can be compared using "==".
 */
object XMLSymbols {

  /**
   * The empty string.
   */
  val EMPTY_STRING = "".intern()

  /**
   * The internalized "xml" prefix.
   */
  val PREFIX_XML = "xml".intern()

  /**
   * The internalized "xmlns" prefix.
   */
  val PREFIX_XMLNS = "xmlns".intern()

  /**
   Symbol: "ANY".
   */
  val fANYSymbol = "ANY".intern()

  /**
   Symbol: "CDATA".
   */
  val fCDATASymbol = "CDATA".intern()

  /**
   Symbol: "ID".
   */
  val fIDSymbol = "ID".intern()

  /**
   Symbol: "IDREF".
   */
  val fIDREFSymbol = "IDREF".intern()

  /**
   Symbol: "IDREFS".
   */
  val fIDREFSSymbol = "IDREFS".intern()

  /**
   Symbol: "ENTITY".
   */
  val fENTITYSymbol = "ENTITY".intern()

  /**
   Symbol: "ENTITIES".
   */
  val fENTITIESSymbol = "ENTITIES".intern()

  /**
   Symbol: "NMTOKEN".
   */
  val fNMTOKENSymbol = "NMTOKEN".intern()

  /**
   Symbol: "NMTOKENS".
   */
  val fNMTOKENSSymbol = "NMTOKENS".intern()

  /**
   Symbol: "NOTATION".
   */
  val fNOTATIONSymbol = "NOTATION".intern()

  /**
   Symbol: "ENUMERATION".
   */
  val fENUMERATIONSymbol = "ENUMERATION".intern()

  /**
   Symbol: "#IMPLIED.
   */
  val fIMPLIEDSymbol = "#IMPLIED".intern()

  /**
   Symbol: "#REQUIRED".
   */
  val fREQUIREDSymbol = "#REQUIRED".intern()

  /**
   Symbol: "#FIXED".
   */
  val fFIXEDSymbol = "#FIXED".intern()
}
