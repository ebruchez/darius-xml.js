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

package org.orbeon.darius.xml.parsers

import java.io.IOException

import java.{util ⇒ ju}

import org.orbeon.darius.xml.impl.Constants
import org.orbeon.darius.xml.impl.Constants._
import org.orbeon.darius.xml.util.ParserConfigurationSettings
import org.orbeon.darius.xml.util.SymbolTable
import org.orbeon.darius.xml.xni.XMLDTDContentModelHandler
import org.orbeon.darius.xml.xni.XMLDTDHandler
import org.orbeon.darius.xml.xni.XMLDocumentHandler
import org.orbeon.darius.xml.xni.XNIException
import org.orbeon.darius.xml.xni.parser.XMLComponent
import org.orbeon.darius.xml.xni.parser.XMLComponentManager
import org.orbeon.darius.xml.xni.parser.XMLConfigurationException
import org.orbeon.darius.xml.xni.parser.XMLDocumentSource
import org.orbeon.darius.xml.xni.parser.XMLEntityResolver
import org.orbeon.darius.xml.xni.parser.XMLErrorHandler
import org.orbeon.darius.xml.xni.parser.XMLInputSource
import org.orbeon.darius.xml.xni.parser.XMLParserConfiguration

protected[parsers] object BasicParserConfiguration {

  /**
   Feature identifier: validation.
   */
  val VALIDATION = Constants.SAX_FEATURE_PREFIX + Constants.VALIDATION_FEATURE

  /**
   Feature identifier: namespaces.
   */
  val NAMESPACES = Constants.SAX_FEATURE_PREFIX + Constants.NAMESPACES_FEATURE

  /**
   Feature identifier: external general entities.
   */
  val EXTERNAL_GENERAL_ENTITIES = Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_GENERAL_ENTITIES_FEATURE

  /**
   Feature identifier: external parameter entities.
   */
  val EXTERNAL_PARAMETER_ENTITIES = Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_PARAMETER_ENTITIES_FEATURE

  /**
   Property identifier: xml string.
   */
  val XML_STRING = Constants.SAX_PROPERTY_PREFIX + Constants.XML_STRING_PROPERTY

  /**
   Property identifier: symbol table.
   */
  val SYMBOL_TABLE = Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY

  /**
   Property identifier: error handler.
   */
  val ERROR_HANDLER = Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_HANDLER_PROPERTY

  /**
   Property identifier: entity resolver.
   */
  val ENTITY_RESOLVER = Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_RESOLVER_PROPERTY
}

/**
 * A very basic parser configuration. This configuration class can
 * be used as a base class for custom parser configurations. The
 * basic parser configuration creates the symbol table (if not
 * specified at construction time) and manages all of the recognized
 * features and properties.
 *
 * The basic parser configuration does *not* mandate
 * any particular pipeline configuration or the use of specific
 * components except for the symbol table. If even this is too much
 * for a basic parser configuration, the programmer can create a new
 * configuration class that implements the
 * `XMLParserConfiguration` interface.
 *
 * Subclasses of the basic parser configuration can add their own
 * recognized features and properties by calling the
 * `addRecognizedFeature` and
 * `addRecognizedProperty` methods, respectively.
 *
 * The basic parser configuration assumes that the configuration
 * will be made up of various parser components that implement the
 * `XMLComponent` interface. If subclasses of this
 * configuration create their own components for use in the
 * parser configuration, then each component should be added to
 * the list of components by calling the `addComponent`
 * method. The basic parser configuration will make sure to call
 * the `reset` method of each registered component
 * before parsing an instance document.
 *
 * This class recognizes the following features and properties:
 *
 * - Features
 *
 *   - http://xml.org/sax/features/validation
 *   - http://xml.org/sax/features/namespaces
 *   - http://xml.org/sax/features/external-general-entities
 *   - http://xml.org/sax/features/external-parameter-entities
 *
 * - Properties
 *
 *   - http://xml.org/sax/properties/xml-string
 *   - http://apache.org/xml/properties/internal/symbol-table
 *   - http://apache.org/xml/properties/internal/error-handler
 *   - http://apache.org/xml/properties/internal/entity-resolver
 *
 *
 */
abstract class BasicParserConfiguration protected (protected var fSymbolTable: SymbolTable, parentSettings: XMLComponentManager)
    extends ParserConfigurationSettings(parentSettings) with XMLParserConfiguration {

  import BasicParserConfiguration._

  /**
   Components.
   */
  protected var fComponents = new ju.ArrayList[XMLComponent]()

  /**
   The document handler.
   */
  protected var fDocumentHandler: XMLDocumentHandler = _

  /**
   The DTD handler.
   */
  protected var fDTDHandler: XMLDTDHandler = _

  /**
   The DTD content model handler.
   */
  protected var fDTDContentModelHandler: XMLDTDContentModelHandler = _

  /**
   Last component in the document pipeline
   */
  protected var fLastComponent: XMLDocumentSource = _

  fRecognizedFeatures = new ju.ArrayList[String]()

  fRecognizedProperties = new ju.ArrayList[String]()

  fFeatures = new ju.HashMap[String, Any]()

  fProperties = new ju.HashMap[String, Any]()

  val recognizedFeatures = Array(PARSER_SETTINGS, VALIDATION, NAMESPACES, EXTERNAL_GENERAL_ENTITIES, EXTERNAL_PARAMETER_ENTITIES)

  addRecognizedFeatures(recognizedFeatures)

  fFeatures.put(PARSER_SETTINGS, true)
  fFeatures.put(VALIDATION, false)
  fFeatures.put(NAMESPACES, true)
  fFeatures.put(EXTERNAL_GENERAL_ENTITIES, true)
  fFeatures.put(EXTERNAL_PARAMETER_ENTITIES, true)

  val recognizedProperties = Array(XML_STRING, SYMBOL_TABLE, ERROR_HANDLER, ENTITY_RESOLVER)

  addRecognizedProperties(recognizedProperties)

  if (fSymbolTable eq null) {
    fSymbolTable = new SymbolTable()
  }

  fProperties.put(SYMBOL_TABLE, fSymbolTable)

  /**
   Default Constructor.
   */
  protected def this() {
    this(null, null)
  }

  /**
   * Constructs a parser configuration using the specified symbol table.
   *
   * @param symbolTable The symbol table to use.
   */
  protected def this(symbolTable: SymbolTable) {
    this(symbolTable, null)
  }

  /**
   * Adds a component to the parser configuration. This method will
   * also add all of the component's recognized features and properties
   * to the list of default recognized features and properties.
   *
   * @param component The component to add.
   */
  protected def addComponent(component: XMLComponent): Unit = {
    if (fComponents.contains(component)) {
      return
    }
    fComponents.add(component)
    val recognizedFeatures = component.getRecognizedFeatures
    addRecognizedFeatures(recognizedFeatures)
    val recognizedProperties = component.getRecognizedProperties
    addRecognizedProperties(recognizedProperties)
    if (recognizedFeatures ne null) {
      for (featureId ← recognizedFeatures) {
        val state = component.getFeatureDefault(featureId)
        if (state ne null) {
          super.setFeature(featureId, state.booleanValue())
        }
      }
    }
    if (recognizedProperties ne null) {
      for (propertyId ← recognizedProperties) {
        val value = component.getPropertyDefault(propertyId)
        if (value ne null) {
          super.setProperty(propertyId, value)
        }
      }
    }
  }

  /**
   * Parse an XML document.
   *
   * The parser can use this method to instruct this configuration
   * to begin parsing an XML document from any valid input source
   * (a character stream, a byte stream, or a URI).
   *
   * Parsers may not invoke this method while a parse is in progress.
   * Once a parse is complete, the parser may then parse another XML
   * document.
   *
   * This method is synchronous: it will not return until parsing
   * has ended.  If a client application wants to terminate
   * parsing early, it should throw an exception.
   *
   * @param inputSource The input source for the top-level of the
   *               XML document.
   *
   * @throws XNIException Any XNI exception, possibly wrapping
   *                         another exception.
   * @throws IOException  An IO exception from the parser, possibly
   *                         from a byte stream or character stream
   *                         supplied by the parser.
   */
  def parse(inputSource: XMLInputSource): Unit

  /**
   * Sets the document handler on the last component in the pipeline
   * to receive information about the document.
   *
   * @param documentHandler   The document handler.
   */
  def setDocumentHandler(documentHandler: XMLDocumentHandler): Unit = {
    fDocumentHandler = documentHandler
    if (fLastComponent ne null) {
      fLastComponent.setDocumentHandler(fDocumentHandler)
      if (fDocumentHandler ne null) {
        fDocumentHandler.setDocumentSource(fLastComponent)
      }
    }
  }

  /**
   Returns the registered document handler.
   */
  def getDocumentHandler: XMLDocumentHandler = fDocumentHandler

  /**
   * Sets the DTD handler.
   *
   * @param dtdHandler The DTD handler.
   */
  def setDTDHandler(dtdHandler: XMLDTDHandler): Unit = {
    fDTDHandler = dtdHandler
  }

  /**
   Returns the registered DTD handler.
   */
  def getDTDHandler: XMLDTDHandler = fDTDHandler

  /**
   * Sets the DTD content model handler.
   *
   * @param handler The DTD content model handler.
   */
  def setDTDContentModelHandler(handler: XMLDTDContentModelHandler): Unit = {
    fDTDContentModelHandler = handler
  }

  /**
   Returns the registered DTD content model handler.
   */
  def getDTDContentModelHandler: XMLDTDContentModelHandler = fDTDContentModelHandler

  /**
   * Sets the resolver used to resolve external entities. The EntityResolver
   * interface supports resolution of public and system identifiers.
   *
   * @param resolver The new entity resolver. Passing a null value will
   *                 uninstall the currently installed resolver.
   */
  def setEntityResolver(resolver: XMLEntityResolver): Unit = {
    fProperties.put(ENTITY_RESOLVER, resolver)
  }

  /**
   * Return the current entity resolver.
   *
   * @return The current entity resolver, or null if none
   *         has been registered.
   */
  def getEntityResolver: XMLEntityResolver = {
    fProperties.get(ENTITY_RESOLVER).asInstanceOf[XMLEntityResolver]
  }

  /**
   * Allow an application to register an error event handler.
   *
   * If the application does not register an error handler, all
   * error events reported by the SAX parser will be silently
   * ignored; however, normal processing may not continue.  It is
   * highly recommended that all SAX applications implement an
   * error handler to avoid unexpected bugs.
   *
   * Applications may register a new or different handler in the
   * middle of a parse, and the SAX parser must begin using the new
   * handler immediately.
   *
   * @param errorHandler The error handler.
   * @throws java.lang.NullPointerException If the handler
   *            argument is null.
   */
  def setErrorHandler(errorHandler: XMLErrorHandler): Unit = {
    fProperties.put(ERROR_HANDLER, errorHandler)
  }

  /**
   * Return the current error handler.
   *
   * @return The current error handler, or null if none
   *         has been registered.
   */
  def getErrorHandler: XMLErrorHandler = {
    fProperties.get(ERROR_HANDLER).asInstanceOf[XMLErrorHandler]
  }

  /**
   * Set the state of a feature.
   *
   * Set the state of any feature in a SAX2 parser.  The parser
   * might not recognize the feature, and if it does recognize
   * it, it might not be able to fulfill the request.
   *
   * @param featureId The unique identifier (URI) of the feature.
   * @param state The requested state of the feature (true or false).
   *
   * @throws org.orbeon.darius.xml.xni.parser.XMLConfigurationException If the
   *            requested feature is not known.
   */
  override def setFeature(featureId: String, state: Boolean): Unit = {
    for (c ← fComponents) {
      c.setFeature(featureId, state)
    }
    super.setFeature(featureId, state)
  }

  override def setProperty(propertyId: String, value: AnyRef): Unit = {
    for (c ← fComponents) {
      c.setProperty(propertyId, value)
    }
    super.setProperty(propertyId, value)
  }

  /**
   * reset all components before parsing and namespace context
   */
  protected def reset(): Unit = {
    for (c ← fComponents) {
      c.reset(this)
    }
  }

  /**
   * Check a property. If the property is known and supported, this method
   * simply returns. Otherwise, the appropriate exception is thrown.
   *
   * @param propertyId The unique identifier (URI) of the property
   *                   being set.
   * @throws org.orbeon.darius.xml.xni.parser.XMLConfigurationException If the
   *            requested feature is not known or supported.
   */
  override protected def checkProperty(propertyId: String): Unit = {
    if (propertyId.startsWith(Constants.SAX_PROPERTY_PREFIX)) {
      val suffixLength = propertyId.length - Constants.SAX_PROPERTY_PREFIX.length
      if (suffixLength == Constants.XML_STRING_PROPERTY.length &&
        propertyId.endsWith(Constants.XML_STRING_PROPERTY)) {
        val `type` = XMLConfigurationException.NOT_SUPPORTED
        throw new XMLConfigurationException(`type`, propertyId)
      }
    }
    super.checkProperty(propertyId)
  }

  /**
   * Check a feature. If feature is know and supported, this method simply
   * returns. Otherwise, the appropriate exception is thrown.
   *
   * @param featureId The unique identifier (URI) of the feature.
   *
   * @throws XMLConfigurationException Thrown for configuration error.
   *                                   In general, components should
   *                                   only throw this exception if
   *                                   it is *really*
   *                                   a critical error.
   */
  override protected def checkFeature(featureId: String): Unit = {
    if (featureId.startsWith(Constants.XERCES_FEATURE_PREFIX)) {
      val suffixLength = featureId.length - Constants.XERCES_FEATURE_PREFIX.length
      if (suffixLength == Constants.PARSER_SETTINGS.length && featureId.endsWith(Constants.PARSER_SETTINGS)) {
        val `type` = XMLConfigurationException.NOT_SUPPORTED
        throw new XMLConfigurationException(`type`, featureId)
      }
    }
    super.checkFeature(featureId)
  }
}
