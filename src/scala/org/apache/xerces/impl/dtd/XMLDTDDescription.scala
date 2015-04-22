package org.apache.xerces.impl.dtd

import org.apache.xerces.util.XMLResourceIdentifierImpl

// @ebruchez: stub to get the code to compile.
class XMLDTDDescription(
  publicId   : String, 
  literalId  : String, 
  baseId     : String, 
  expandedId : String, 
  rootName   : String
) extends XMLResourceIdentifierImpl
with org.apache.xerces.xni.grammars.XMLDTDDescription {

  def setRootName(rootName: String) = ()
  def getRootName: String = null
  def getGrammarType: String = null
}
