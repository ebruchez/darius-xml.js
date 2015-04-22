package org.orbeon.darius.xni.grammars

import java.io.IOException
import java.util.Locale

import org.orbeon.darius.xni.XNIException
import org.orbeon.darius.xni.parser.XMLConfigurationException
import org.orbeon.darius.xni.parser.XMLEntityResolver
import org.orbeon.darius.xni.parser.XMLErrorHandler
import org.orbeon.darius.xni.parser.XMLInputSource

/**
 * The intention of this interface is to provide a generic means
 * by which Grammar objects may be created without parsing instance
 * documents.  Implementations of this interface will know how to load
 * specific types of grammars (e.g., DTD's or schemas); a wrapper
 * will be provided for user applications to interact with these implementations.
 */
trait XMLGrammarLoader {

  /**
   * Returns a list of feature identifiers that are recognized by
   * this XMLGrammarLoader.  This method may return null if no features
   * are recognized.
   */
  def getRecognizedFeatures: Array[String]

  /**
   * Returns the state of a feature.
   *
   * @param featureId The feature identifier.
   *
   * @throws XMLConfigurationException Thrown on configuration error.
   */
  def getFeature(featureId: String): Boolean

  /**
   * Sets the state of a feature.
   *
   * @param featureId The feature identifier.
   * @param state     The state of the feature.
   *
   * @throws XMLConfigurationException Thrown when a feature is not
   *                  recognized or cannot be set.
   */
  def setFeature(featureId: String, state: Boolean): Unit

  /**
   * Returns a list of property identifiers that are recognized by
   * this XMLGrammarLoader.  This method may return null if no properties
   * are recognized.
   */
  def getRecognizedProperties: Array[String]

  /**
   * Returns the state of a property.
   *
   * @param propertyId The property identifier.
   *
   * @throws XMLConfigurationException Thrown on configuration error.
   */
  def getProperty(propertyId: String): AnyRef

  /**
   * Sets the state of a property.
   *
   * @param propertyId The property identifier.
   * @param state     The state of the property.
   *
   * @throws XMLConfigurationException Thrown when a property is not
   *                  recognized or cannot be set.
   */
  def setProperty(propertyId: String, state: AnyRef): Unit

  /**
   * Set the locale to use for messages.
   *
   * @param locale The locale object to use for localization of messages.
   *
   * @throws XNIException Thrown if the parser does not support the
   *                         specified locale.
   */
  def setLocale(locale: Locale): Unit

  /**
   Return the Locale the XMLGrammarLoader is using.
   */
  def getLocale: Locale

  /**
   * Sets the error handler.
   *
   * @param errorHandler The error handler.
   */
  def setErrorHandler(errorHandler: XMLErrorHandler): Unit

  /**
   Returns the registered error handler.
   */
  def getErrorHandler: XMLErrorHandler

  /**
   * Sets the entity resolver.
   *
   * @param entityResolver The new entity resolver.
   */
  def setEntityResolver(entityResolver: XMLEntityResolver): Unit

  /**
   Returns the registered entity resolver.
   */
  def getEntityResolver: XMLEntityResolver

  /**
   * Returns a Grammar object by parsing the contents of the
   * entity pointed to by source.
   *
   * @param source        the location of the entity which forms
   *                          the starting point of the grammar to be constructed.
   * @throws IOException      When a problem is encountered reading the entity
   *          XNIException    When a condition arises (such as a FatalError) that requires parsing
   *                              of the entity be terminated.
   */
  def loadGrammar(source: XMLInputSource): Grammar
}
