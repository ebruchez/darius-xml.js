package org.apache.xerces.xni.grammars

import org.apache.xerces.xni.XMLResourceIdentifier

object XMLGrammarDescription {

  /**
   * The grammar type constant for XML Schema grammars. When getGrammarType()
   * method returns this constant, the object should be an instance of
   * the XMLSchemaDescription interface.
   */
  val XML_SCHEMA = "http://www.w3.org/2001/XMLSchema"

  /**
   * The grammar type constant for DTD grammars. When getGrammarType()
   * method returns this constant, the object should be an instance of
   * the XMLDTDDescription interface.
   */
  val XML_DTD = "http://www.w3.org/TR/REC-xml"
}

/**
 *  This interface describes basic attributes of XML grammars--their
 * physical location and their type. 
 */
trait XMLGrammarDescription extends XMLResourceIdentifier {

  /**
   * Return the type of this grammar.
   *
   * @return  the type of this grammar
   */
  def getGrammarType: String
}
