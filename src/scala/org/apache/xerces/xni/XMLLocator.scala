package org.apache.xerces.xni

/**
 * Location information.
 */
trait XMLLocator {

  /**
   Returns the public identifier.
   */
  def getPublicId: String

  /**
   Returns the literal system identifier.
   */
  def getLiteralSystemId: String

  /**
   Returns the base system identifier.
   */
  def getBaseSystemId: String

  /**
   Returns the expanded system identifier.
   */
  def getExpandedSystemId: String

  /**
   Returns the line number, or `-1` if no line number is available.
   */
  def getLineNumber: Int

  /**
   Returns the column number, or `-1` if no column number is available.
   */
  def getColumnNumber: Int

  /**
   Returns the character offset, or `-1` if no character offset is available.
   */
  def getCharacterOffset: Int

  /**
   * Returns the encoding of the current entity.
   * Note that, for a given entity, this value can only be
   * considered final once the encoding declaration has been read (or once it
   * has been determined that there is no such declaration) since, no encoding
   * having been specified on the XMLInputSource, the parser
   * will make an initial "guess" which could be in error.
   */
  def getEncoding: String

  /**
   * Returns the XML version of the current entity. This will normally be the
   * value from the XML or text declaration or defaulted by the parser. Note that
   * that this value may be different than the version of the processing rules
   * applied to the current entity. For instance, an XML 1.1 document may refer to
   * XML 1.0 entities. In such a case the rules of XML 1.1 are applied to the entire
   * document. Also note that, for a given entity, this value can only be considered
   * final once the XML or text declaration has been read or once it has been
   * determined that there is no such declaration.
   */
  def getXMLVersion: String
}
