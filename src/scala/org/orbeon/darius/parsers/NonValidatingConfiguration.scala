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

package org.orbeon.darius.parsers

import java.io.IOException
import java.util.Locale

import org.orbeon.darius.impl.Constants
import org.orbeon.darius.impl.XMLDTDScannerImpl
import org.orbeon.darius.impl.XMLDocumentScannerImpl
import org.orbeon.darius.impl.XMLEntityManager
import org.orbeon.darius.impl.XMLErrorReporter
import org.orbeon.darius.impl.XMLNSDocumentScannerImpl
import org.orbeon.darius.impl.msg.XMLMessageFormatter
import org.orbeon.darius.impl.validation.ValidationManager
import org.orbeon.darius.util.ParserConfigurationSettings
import org.orbeon.darius.util.SymbolTable
import org.orbeon.darius.xni.XMLLocator
import org.orbeon.darius.xni.XNIException
import org.orbeon.darius.xni.grammars.XMLGrammarPool
import org.orbeon.darius.xni.parser.XMLComponent
import org.orbeon.darius.xni.parser.XMLComponentManager
import org.orbeon.darius.xni.parser.XMLConfigurationException
import org.orbeon.darius.xni.parser.XMLDTDScanner
import org.orbeon.darius.xni.parser.XMLDocumentScanner
import org.orbeon.darius.xni.parser.XMLInputSource
import org.orbeon.darius.xni.parser.XMLPullParserConfiguration

protected[parsers] object NonValidatingConfiguration {

  /**
   Feature identifier: warn on duplicate attribute definition.
   */
  val WARN_ON_DUPLICATE_ATTDEF = Constants.XERCES_FEATURE_PREFIX + Constants.WARN_ON_DUPLICATE_ATTDEF_FEATURE

  /**
   Feature identifier: warn on duplicate entity definition.
   */
  val WARN_ON_DUPLICATE_ENTITYDEF = Constants.XERCES_FEATURE_PREFIX + Constants.WARN_ON_DUPLICATE_ENTITYDEF_FEATURE

  /**
   Feature identifier: warn on undeclared element definition.
   */
  val WARN_ON_UNDECLARED_ELEMDEF = Constants.XERCES_FEATURE_PREFIX + Constants.WARN_ON_UNDECLARED_ELEMDEF_FEATURE

  /**
   Feature identifier: allow Java encodings.
   */
  val ALLOW_JAVA_ENCODINGS = Constants.XERCES_FEATURE_PREFIX + Constants.ALLOW_JAVA_ENCODINGS_FEATURE

  /**
   Feature identifier: continue after fatal error.
   */
  val CONTINUE_AFTER_FATAL_ERROR = Constants.XERCES_FEATURE_PREFIX + Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE

  /**
   Feature identifier: load external DTD.
   */
  val LOAD_EXTERNAL_DTD = Constants.XERCES_FEATURE_PREFIX + Constants.LOAD_EXTERNAL_DTD_FEATURE

  /**
   Feature identifier: notify built-in refereces.
   */
  val NOTIFY_BUILTIN_REFS = Constants.XERCES_FEATURE_PREFIX + Constants.NOTIFY_BUILTIN_REFS_FEATURE

  /**
   Feature identifier: notify character refereces.
   */
  val NOTIFY_CHAR_REFS = Constants.XERCES_FEATURE_PREFIX + Constants.NOTIFY_CHAR_REFS_FEATURE

  /**
   Feature identifier: expose schema normalized value
   */
  val NORMALIZE_DATA = Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_NORMALIZED_VALUE

  /**
   Feature identifier: send element default value via characters()
   */
  val SCHEMA_ELEMENT_DEFAULT = Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_ELEMENT_DEFAULT

  /**
   Property identifier: error reporter.
   */
  val ERROR_REPORTER = Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY

  /**
   Property identifier: entity manager.
   */
  val ENTITY_MANAGER = Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_MANAGER_PROPERTY

  /**
   Property identifier document scanner:
   */
  val DOCUMENT_SCANNER = Constants.XERCES_PROPERTY_PREFIX + Constants.DOCUMENT_SCANNER_PROPERTY

  /**
   Property identifier: DTD scanner.
   */
  val DTD_SCANNER = Constants.XERCES_PROPERTY_PREFIX + Constants.DTD_SCANNER_PROPERTY

  /**
   Property identifier: grammar pool.
   */
  val XMLGRAMMAR_POOL = Constants.XERCES_PROPERTY_PREFIX + Constants.XMLGRAMMAR_POOL_PROPERTY

  /**
   Property identifier: DTD validator.
   */
  val DTD_VALIDATOR = Constants.XERCES_PROPERTY_PREFIX + Constants.DTD_VALIDATOR_PROPERTY

  /**
   Property identifier: namespace binder.
   */
  val NAMESPACE_BINDER = Constants.XERCES_PROPERTY_PREFIX + Constants.NAMESPACE_BINDER_PROPERTY

  /**
   Property identifier: datatype validator factory.
   */
  val DATATYPE_VALIDATOR_FACTORY = Constants.XERCES_PROPERTY_PREFIX + Constants.DATATYPE_VALIDATOR_FACTORY_PROPERTY

  val VALIDATION_MANAGER = Constants.XERCES_PROPERTY_PREFIX + Constants.VALIDATION_MANAGER_PROPERTY

  /**
   Property identifier: XML Schema validator.
   */
  val SCHEMA_VALIDATOR = Constants.XERCES_PROPERTY_PREFIX + Constants.SCHEMA_VALIDATOR_PROPERTY

  /**
   Property identifier: locale.
   */
  val LOCALE = Constants.XERCES_PROPERTY_PREFIX + Constants.LOCALE_PROPERTY

  /**
   Set to true and recompile to print exception stack trace.
   */
  private val PRINT_EXCEPTION_STACK_TRACE = false
}

/**
 * This is the non validating parser configuration. It extends the basic
 * configuration with the set of following parser components:
 * Document scanner, DTD scanner, namespace binder, document handler.
 * 
 * Parser that uses this configuration is *not* [conformant](http://www.w3.org/TR/REC-xml#sec-conformance)
 * non-validating XML processor, since conformant non-validating processor is required
 * to process "all the declarations they read in the internal DTD subset ... must use the information in those declarations to normalize attribute values,
 * include the replacement text of internal entities, and supply default attribute values".
 */
class NonValidatingConfiguration(symbolTable: SymbolTable, protected var fGrammarPool: XMLGrammarPool, parentSettings: XMLComponentManager)
    extends BasicParserConfiguration(symbolTable, parentSettings) with XMLPullParserConfiguration {
  
  import BasicParserConfiguration._
  import NonValidatingConfiguration._
  import ParserConfigurationSettings._

  /**
   Error reporter.
   */
  protected var fErrorReporter: XMLErrorReporter = createErrorReporter()

  /**
   Entity manager.
   */
  protected var fEntityManager: XMLEntityManager = createEntityManager()

  /**
   Document scanner.
   */
  protected var fScanner: XMLDocumentScanner = _

  /**
   Input Source
   */
  protected var fInputSource: XMLInputSource = _

  /**
   DTD scanner.
   */
  protected var fDTDScanner: XMLDTDScanner = createDTDScanner()

  protected var fValidationManager: ValidationManager = createValidationManager()

  /**
   Document scanner that does namespace binding.
   */
  private var fNamespaceScanner: XMLNSDocumentScannerImpl = _

  /**
   Default implementation of scanner
   */
  private var fNonNSScanner: XMLDocumentScannerImpl = _

  /**
   fConfigUpdated is set to true if there has been any change to the configuration settings,
   * i.e a feature or a property was changed.
   */
  protected var fConfigUpdated: Boolean = false

  protected var fLocator: XMLLocator = _

  /**
   * True if a parse is in progress. This state is needed because
   * some features/properties cannot be set while parsing (e.g.
   * validation and namespaces).
   */
  protected var fParseInProgress: Boolean = false

  override val recognizedFeatures = Array(PARSER_SETTINGS, NAMESPACES, CONTINUE_AFTER_FATAL_ERROR)

  override val recognizedProperties = Array(ERROR_REPORTER, ENTITY_MANAGER, DOCUMENT_SCANNER, DTD_SCANNER, DTD_VALIDATOR, NAMESPACE_BINDER, XMLGRAMMAR_POOL, DATATYPE_VALIDATOR_FACTORY, VALIDATION_MANAGER, LOCALE)
  
  locally {
    addRecognizedFeatures(recognizedFeatures)
  
    fFeatures.put(CONTINUE_AFTER_FATAL_ERROR, false)
    fFeatures.put(PARSER_SETTINGS, true)
    fFeatures.put(NAMESPACES, true)
    
    addRecognizedProperties(recognizedProperties)
  
    if (fGrammarPool ne null) {
      fProperties.put(XMLGRAMMAR_POOL, fGrammarPool)
    }
  
    fProperties.put(ENTITY_MANAGER, fEntityManager)
    addComponent(fEntityManager)
  
    fErrorReporter.setDocumentLocator(fEntityManager.getEntityScanner)
  
    fProperties.put(ERROR_REPORTER, fErrorReporter)
    addComponent(fErrorReporter)
  
    if (fDTDScanner ne null) {
      fProperties.put(DTD_SCANNER, fDTDScanner)
      fDTDScanner match {
        case component: XMLComponent ⇒ addComponent(component)
        case _ ⇒
      }
    }
  
    if (fValidationManager ne null) {
      fProperties.put(VALIDATION_MANAGER, fValidationManager)
    }
  
    if (fErrorReporter.getMessageFormatter(XMLMessageFormatter.XML_DOMAIN) eq null) {
      val xmft = new XMLMessageFormatter()
      fErrorReporter.putMessageFormatter(XMLMessageFormatter.XML_DOMAIN, xmft)
      fErrorReporter.putMessageFormatter(XMLMessageFormatter.XMLNS_DOMAIN, xmft)
    }
  
    try {
      setLocale(Locale.getDefault)
    } catch {
      case e: XNIException ⇒ 
    }
  }

  def this() {
    this(null, null, null)
  }

  def this(symbolTable: SymbolTable) {
    this(symbolTable, null, null)
  }

  def this(symbolTable: SymbolTable, grammarPool: XMLGrammarPool) {
    this(symbolTable, grammarPool, null)
  }

  override def setFeature(featureId: String, state: Boolean): Unit = {
    fConfigUpdated = true
    super.setFeature(featureId, state)
  }

  override def getProperty(propertyId: String): Any = {
    if (LOCALE == propertyId) {
      return getLocale
    }
    super.getProperty(propertyId)
  }

  override def setProperty(propertyId: String, value: AnyRef): Unit = {
    fConfigUpdated = true
    if (LOCALE == propertyId) {
      setLocale(value.asInstanceOf[Locale])
    }
    super.setProperty(propertyId, value)
  }

  override def setLocale(locale: Locale): Unit = {
    super.setLocale(locale)
    fErrorReporter.setLocale(locale)
  }

  override def getFeature(featureId: String): Boolean = {
    if (featureId == PARSER_SETTINGS) {
      // @ebruchez: unclear how this works: fConfigUpdated remains false if there is no call to setFeature or
      // setProperty, and some components check PARSER_SETTINGS to decide whether to initialize. For now, return true.
      //return fConfigUpdated
      return true
    }
    super.getFeature(featureId)
  }

  def setInputSource(inputSource: XMLInputSource): Unit = {
    fInputSource = inputSource
  }

  def parse(complete: Boolean): Boolean = {
    if (fInputSource ne null) {
      try {
        reset()
        fScanner.setInputSource(fInputSource)
        fInputSource = null
      } catch {
        case ex: XNIException ⇒
          if (PRINT_EXCEPTION_STACK_TRACE) ex.printStackTrace()
          throw ex
        case ex: IOException ⇒
          if (PRINT_EXCEPTION_STACK_TRACE) ex.printStackTrace()
          throw ex
        case ex: RuntimeException ⇒
          if (PRINT_EXCEPTION_STACK_TRACE) ex.printStackTrace()
          throw ex
        case ex: Exception ⇒
          if (PRINT_EXCEPTION_STACK_TRACE) ex.printStackTrace()
          throw new XNIException(ex)
      }
    }
    try {
      fScanner.scanDocument(complete)
    } catch {
      case ex: XNIException ⇒
        if (PRINT_EXCEPTION_STACK_TRACE) ex.printStackTrace()
        throw ex
      case ex: IOException ⇒
        if (PRINT_EXCEPTION_STACK_TRACE) ex.printStackTrace()
        throw ex
      case ex: RuntimeException ⇒
        if (PRINT_EXCEPTION_STACK_TRACE) ex.printStackTrace()
        throw ex
      case ex: Exception ⇒
        if (PRINT_EXCEPTION_STACK_TRACE) ex.printStackTrace()
        throw new XNIException(ex)
    }
  }

  def cleanup(): Unit = {
    fEntityManager.closeReaders()
  }

  def parse(source: XMLInputSource): Unit = {
    if (fParseInProgress) {
      throw new XNIException("FWK005 parse may not be called while parsing.")
    }
    fParseInProgress = true
    try {
      setInputSource(source)
      parse(complete = true)
    } catch {
      case ex: XNIException ⇒
        if (PRINT_EXCEPTION_STACK_TRACE) ex.printStackTrace()
        throw ex
      case ex: IOException ⇒
        if (PRINT_EXCEPTION_STACK_TRACE) ex.printStackTrace()
        throw ex
      case ex: RuntimeException ⇒
        if (PRINT_EXCEPTION_STACK_TRACE) ex.printStackTrace()
        throw ex
      case ex: Exception ⇒
        if (PRINT_EXCEPTION_STACK_TRACE) ex.printStackTrace()
        throw new XNIException(ex)
    } finally {
      fParseInProgress = false
      this.cleanup()
    }
  }

  override protected def reset(): Unit = {
    if (fValidationManager ne null)
      fValidationManager.reset()
    configurePipeline()
    super.reset()
  }

  protected def configurePipeline(): Unit = {
    if (fFeatures.get(NAMESPACES) == true) {
      if (fNamespaceScanner eq null) {
        fNamespaceScanner = new XMLNSDocumentScannerImpl()
        addComponent(fNamespaceScanner.asInstanceOf[XMLComponent])
      }
      fProperties.put(DOCUMENT_SCANNER, fNamespaceScanner)
      fNamespaceScanner.setDTDValidator(null)
      fScanner = fNamespaceScanner
    } else {
      if (fNonNSScanner eq null) {
        fNonNSScanner = new XMLDocumentScannerImpl()
        addComponent(fNonNSScanner.asInstanceOf[XMLComponent])
      }
      fProperties.put(DOCUMENT_SCANNER, fNonNSScanner)
      fScanner = fNonNSScanner
    }
    fScanner.setDocumentHandler(fDocumentHandler)
    fLastComponent = fScanner
    if (fDTDScanner ne null) {
      fDTDScanner.setDTDHandler(fDTDHandler)
      fDTDScanner.setDTDContentModelHandler(fDTDContentModelHandler)
    }
  }

  override protected def checkFeature(featureId: String): Unit = {
    if (featureId.startsWith(Constants.XERCES_FEATURE_PREFIX)) {
      val suffixLength = featureId.length - Constants.XERCES_FEATURE_PREFIX.length
      if (suffixLength == Constants.DYNAMIC_VALIDATION_FEATURE.length && 
        featureId.endsWith(Constants.DYNAMIC_VALIDATION_FEATURE)) {
        return
      }
      if (suffixLength == Constants.DEFAULT_ATTRIBUTE_VALUES_FEATURE.length && 
        featureId.endsWith(Constants.DEFAULT_ATTRIBUTE_VALUES_FEATURE)) {
        val `type` = XMLConfigurationException.NOT_SUPPORTED
        throw new XMLConfigurationException(`type`, featureId)
      }
      if (suffixLength == Constants.VALIDATE_CONTENT_MODELS_FEATURE.length && 
        featureId.endsWith(Constants.VALIDATE_CONTENT_MODELS_FEATURE)) {
        val `type` = XMLConfigurationException.NOT_SUPPORTED
        throw new XMLConfigurationException(`type`, featureId)
      }
      if (suffixLength == Constants.LOAD_DTD_GRAMMAR_FEATURE.length && 
        featureId.endsWith(Constants.LOAD_DTD_GRAMMAR_FEATURE)) {
        return
      }
      if (suffixLength == Constants.LOAD_EXTERNAL_DTD_FEATURE.length && 
        featureId.endsWith(Constants.LOAD_EXTERNAL_DTD_FEATURE)) {
        return
      }
      if (suffixLength == Constants.VALIDATE_DATATYPES_FEATURE.length && 
        featureId.endsWith(Constants.VALIDATE_DATATYPES_FEATURE)) {
        val `type` = XMLConfigurationException.NOT_SUPPORTED
        throw new XMLConfigurationException(`type`, featureId)
      }
    }
    super.checkFeature(featureId)
  }

  override protected def checkProperty(propertyId: String): Unit = {
    if (propertyId.startsWith(Constants.XERCES_PROPERTY_PREFIX)) {
      val suffixLength = propertyId.length - Constants.XERCES_PROPERTY_PREFIX.length
      if (suffixLength == Constants.DTD_SCANNER_PROPERTY.length && 
        propertyId.endsWith(Constants.DTD_SCANNER_PROPERTY)) {
        return
      }
    }
    if (propertyId.startsWith(Constants.JAXP_PROPERTY_PREFIX)) {
      val suffixLength = propertyId.length - Constants.JAXP_PROPERTY_PREFIX.length
      if (suffixLength == Constants.SCHEMA_SOURCE.length && propertyId.endsWith(Constants.SCHEMA_SOURCE)) {
        return
      }
    }
    super.checkProperty(propertyId)
  }

  protected def createEntityManager()    : XMLEntityManager  = new XMLEntityManager
  protected def createErrorReporter()    : XMLErrorReporter  = new XMLErrorReporter
  protected def createDTDScanner()       : XMLDTDScanner     = new XMLDTDScannerImpl
  protected def createValidationManager(): ValidationManager = new ValidationManager
}
