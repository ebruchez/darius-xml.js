package org.apache.xerces.xni.grammars

/**
 * A generic grammar for use in validating XML documents. The Grammar
 * object stores the validation information in a compiled form. Specific
 * subclasses extend this class and "populate" the grammar by compiling
 * the specific syntax (DTD, Schema, etc) into the data structures used
 * by this object.
 * 
 * *Note:* The Grammar object is not useful as a generic
 * grammar access or query object. In other words, you cannot round-trip
 * specific grammar syntaxes with the compiled grammar information in
 * the Grammar object. You *can* create equivalent validation
 * rules in your choice of grammar syntax but there is no guarantee that
 * the input and output will be the same.
 *
 *  Right now, this class is largely a shell; eventually,
 * it will be enriched by having more expressive methods added. 
 * will be moved from dtd.Grammar here.
 */
trait Grammar {

  /**
   * get the `XMLGrammarDescription` associated with this
   * object
   */
  def getGrammarDescription: XMLGrammarDescription
}
