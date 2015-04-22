package org.apache.xerces.parsers

import org.apache.xerces.impl.Constants
import org.apache.xerces.parsers.XMLParser._
import org.apache.xerces.xni.XNIException
import org.apache.xerces.xni.parser.XMLInputSource
import org.apache.xerces.xni.parser.XMLParserConfiguration

object XMLParser {

  /**
   Property identifier: entity resolver.
   */
  protected val ENTITY_RESOLVER = Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_RESOLVER_PROPERTY

  /**
   Property identifier: error handler.
   */
  protected val ERROR_HANDLER = Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_HANDLER_PROPERTY

  /**
   Recognized properties.
   */
  private val RECOGNIZED_PROPERTIES = Array(ENTITY_RESOLVER, ERROR_HANDLER)
}

/**
 * Base class of all XML-related parsers.
 * 
 * In addition to the features and properties recognized by the parser
 * configuration, this parser recognizes these additional features and
 * properties:
 * 
 * - Properties
 *  
 *   - http://apache.org/xml/properties/internal/error-handler
 *   - http://apache.org/xml/properties/internal/entity-resolver
 *  
 * 
 */
abstract class XMLParser protected (protected val fConfiguration: XMLParserConfiguration) {

  fConfiguration.addRecognizedProperties(RECOGNIZED_PROPERTIES)

  def parse(inputSource: XMLInputSource): Unit = {
    reset()
    fConfiguration.parse(inputSource)
  }

  /**
   * reset all components before parsing
   */
  protected def reset(): Unit = ()
}
