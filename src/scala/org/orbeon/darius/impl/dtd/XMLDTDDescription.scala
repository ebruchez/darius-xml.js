package org.orbeon.darius.impl.dtd

import org.orbeon.darius.util.XMLResourceIdentifierImpl

// @ebruchez: stub to get the code to compile.
class XMLDTDDescription(
  publicId   : String, 
  literalId  : String, 
  baseId     : String, 
  expandedId : String, 
  rootName   : String
) extends XMLResourceIdentifierImpl
with org.orbeon.darius.xni.grammars.XMLDTDDescription {

  def setRootName(rootName: String) = ()
  def getRootName: String = null
  def getGrammarType: String = null
}
