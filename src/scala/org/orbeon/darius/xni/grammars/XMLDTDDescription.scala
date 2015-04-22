package org.orbeon.darius.xni.grammars

/**
 * All information specific to DTD grammars.
 */
trait XMLDTDDescription extends XMLGrammarDescription {

  /**
   * Return the root name of this DTD.
   *
   * @return  the root name. null if the name is unknown.
   */
  def getRootName: String
}
