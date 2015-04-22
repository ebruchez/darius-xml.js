package org.apache.xerces.util

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
