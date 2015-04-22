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

package org.orbeon.darius.impl

import java.util.HashMap
import java.util.Locale

import org.orbeon.darius.impl.XMLErrorReporter._
import org.orbeon.darius.util.DefaultErrorHandler
import org.orbeon.darius.util.MessageFormatter
import org.orbeon.darius.xni.XMLLocator
import org.orbeon.darius.xni.XNIException
import org.orbeon.darius.xni.parser.XMLComponent
import org.orbeon.darius.xni.parser.XMLComponentManager
import org.orbeon.darius.xni.parser.XMLErrorHandler
import org.orbeon.darius.xni.parser.XMLParseException

object XMLErrorReporter {

  /**
   * Severity: warning. Warnings represent informational messages only
   * that should not be considered serious enough to stop parsing or
   * indicate an error in the document's validity.
   */
  val SEVERITY_WARNING: Short = 0

  /**
   * Severity: error. Common causes of errors are document structure and/or
   * content that that does not conform to the grammar rules specified for
   * the document. These are typically validation errors.
   */
  val SEVERITY_ERROR: Short = 1

  /**
   * Severity: fatal error. Fatal errors are errors in the syntax of the
   * XML document or invalid byte sequences for a given encoding. The
   * XML 1.0 Specification mandates that errors of this type are not
   * recoverable.
   * 
   * *Note:* The parser does have a "continue after fatal
   * error" feature but it should be used with extreme caution and care.
   */
  val SEVERITY_FATAL_ERROR: Short = 2

  /**
   Feature identifier: continue after fatal error.
   */
  protected val CONTINUE_AFTER_FATAL_ERROR = Constants.XERCES_FEATURE_PREFIX + Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE

  /**
   Property identifier: error handler.
   */
  protected val ERROR_HANDLER = Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_HANDLER_PROPERTY

  /**
   Recognized features.
   */
  private val RECOGNIZED_FEATURES = Array(CONTINUE_AFTER_FATAL_ERROR)

  /**
   Feature defaults.
   */
  private val FEATURE_DEFAULTS = Array[java.lang.Boolean](null)

  /**
   Recognized properties.
   */
  private val RECOGNIZED_PROPERTIES = Array(ERROR_HANDLER)

  /**
   Property defaults.
   */
  private val PROPERTY_DEFAULTS = Array(null)
}

/**
 * This class is a common element of all parser configurations and is
 * used to report errors that occur. This component can be queried by
 * parser components from the component manager using the following
 * property ID:
 * 
 *   http://apache.org/xml/properties/internal/error-reporter
 * 
 * Errors are separated into domains that categorize a class of errors.
 * In a parser configuration, the parser would register a
 * `MessageFormatter` for each domain that is capable of
 * localizing error messages and formatting them based on information
 * about the error. Any parser component can invent new error domains
 * and register additional message formatters to localize messages in
 * those domains.
 * 
 * This component requires the following features and properties from the
 * component manager that uses it:
 * 
 *  - http://apache.org/xml/properties/internal/error-handler
 * 
 * 
 * This component can use the following features and properties but they
 * are not required:
 * 
 *  - http://apache.org/xml/features/continue-after-fatal-error
 * 
 *
 * @see MessageFormatter
 */
class XMLErrorReporter extends XMLComponent {

  /**
   The locale to be used to format error messages.
   */
  protected var fLocale: Locale = _

  /**
   Mapping of Message formatters for domains.
   */
  protected var fMessageFormatters = new HashMap[String, MessageFormatter]()

  /**
   Error handler.
   */
  protected var fErrorHandler: XMLErrorHandler = _

  /**
   Document locator.
   */
  protected var fLocator: XMLLocator = _

  /**
   Continue after fatal error feature.
   */
  protected var fContinueAfterFatalError: Boolean = _

  /**
   * Default error handler. This error handler is only used in the
   * absence of a registered error handler so that errors are not
   * "swallowed" silently. This is one of the most common "problems"
   * reported by users of the parser.
   */
  protected var fDefaultErrorHandler: XMLErrorHandler = _

  /**
   * Sets the current locale.
   *
   * @param locale The new locale.
   */
  def setLocale(locale: Locale): Unit = {
    fLocale = locale
  }

  /**
   * Gets the current locale.
   *
   * @return the current Locale
   */
  def getLocale: Locale = fLocale

  /**
   * Sets the document locator.
   *
   * @param locator The locator.
   */
  def setDocumentLocator(locator: XMLLocator): Unit = {
    fLocator = locator
  }

  /**
   * Registers a message formatter for the specified domain.
   * 
   * *Note:* Registering a message formatter for a domain
   * when there is already a formatter registered will cause the previous
   * formatter to be lost. This method replaces any previously registered
   * message formatter for the specified domain.
   */
  def putMessageFormatter(domain: String, messageFormatter: MessageFormatter): Unit = {
    fMessageFormatters.put(domain, messageFormatter)
  }

  /**
   * Returns the message formatter associated with the specified domain,
   * or null if no message formatter is registered for that domain.
   *
   * @param domain The domain of the message formatter.
   */
  def getMessageFormatter(domain: String): MessageFormatter = {
    fMessageFormatters.get(domain)
  }

  /**
   * Removes the message formatter for the specified domain and
   * returns the removed message formatter.
   *
   * @param domain The domain of the message formatter.
   */
  def removeMessageFormatter(domain: String): MessageFormatter = {
    fMessageFormatters.remove(domain)
  }

  /**
   * Reports an error. The error message passed to the error handler
   * is formatted for the locale by the message formatter installed
   * for the specified error domain.
   *
   * @param domain    The error domain.
   * @param key       The key of the error message.
   * @param arguments The replacement arguments for the error message,
   *                  if needed.
   * @param severity  The severity of the error.
   * @return          The formatted error message.
   *
   * @see #SEVERITY_WARNING
   * @see #SEVERITY_ERROR
   * @see #SEVERITY_FATAL_ERROR
   */
  def reportError(domain: String, 
      key: String, 
      arguments: Array[Any], 
      severity: Short): String = {
    reportError(fLocator, domain, key, arguments, severity)
  }

  /**
   * Reports an error. The error message passed to the error handler
   * is formatted for the locale by the message formatter installed
   * for the specified error domain.
   *
   * @param domain    The error domain.
   * @param key       The key of the error message.
   * @param arguments The replacement arguments for the error message,
   *                  if needed.
   * @param severity  The severity of the error.
   * @param exception The exception to wrap.
   * @return          The formatted error message.
   *
   * @see #SEVERITY_WARNING
   * @see #SEVERITY_ERROR
   * @see #SEVERITY_FATAL_ERROR
   */
  def reportError(domain: String, 
      key: String, 
      arguments: Array[Any], 
      severity: Short, 
      exception: Exception): String = {
    reportError(fLocator, domain, key, arguments, severity, exception)
  }

  /**
   * Reports an error at a specific location.
   *
   * @param location  The error location.
   * @param domain    The error domain.
   * @param key       The key of the error message.
   * @param arguments The replacement arguments for the error message,
   *                  if needed.
   * @param severity  The severity of the error.
   * @return          The formatted error message.
   *
   * @see #SEVERITY_WARNING
   * @see #SEVERITY_ERROR
   * @see #SEVERITY_FATAL_ERROR
   */
  def reportError(location: XMLLocator, 
      domain: String, 
      key: String, 
      arguments: Array[Any], 
      severity: Short): String = {
    reportError(location, domain, key, arguments, severity, null)
  }

  /**
   * Reports an error at a specific location.
   *
   * @param location  The error location.
   * @param domain    The error domain.
   * @param key       The key of the error message.
   * @param arguments The replacement arguments for the error message,
   *                  if needed.
   * @param severity  The severity of the error.
   * @param exception The exception to wrap.
   * @return          The formatted error message.
   *
   * @see #SEVERITY_WARNING
   * @see #SEVERITY_ERROR
   * @see #SEVERITY_FATAL_ERROR
   */
  def reportError(location: XMLLocator, 
      domain: String, 
      key: String, 
      arguments: Array[Any], 
      severity: Short, 
      exception: Exception): String = {
    val messageFormatter = getMessageFormatter(domain)
    var message: String = null
    if (messageFormatter ne null) {
      message = messageFormatter.formatMessage(fLocale, key, arguments)
    } else {
      val str = new StringBuffer()
      str.append(domain)
      str.append('#')
      str.append(key)
      val argCount = if (arguments ne null) arguments.length else 0
      if (argCount > 0) {
        str.append('?')
        for (i ← 0 until argCount) {
          str.append(arguments(i))
          if (i < argCount - 1) {
            str.append('&')
          }
        }
      }
      message = str.toString
    }
    val parseException = if (exception ne null) new XMLParseException(location, message, exception) else new XMLParseException(location, 
      message)
    var errorHandler = fErrorHandler
    if (errorHandler eq null) {
      if (fDefaultErrorHandler eq null) {
        fDefaultErrorHandler = new DefaultErrorHandler()
      }
      errorHandler = fDefaultErrorHandler
    }
    severity match {
      case SEVERITY_WARNING ⇒
        errorHandler.warning(domain, key, parseException)
      case SEVERITY_ERROR ⇒
        errorHandler.error(domain, key, parseException)
      case SEVERITY_FATAL_ERROR ⇒
        errorHandler.fatalError(domain, key, parseException)
        if (!fContinueAfterFatalError) {
          throw parseException
        }
    }
    message
  }

  /**
   * Resets the component. The component can query the component manager
   * about any features and properties that affect the operation of the
   * component.
   */
  def reset(componentManager: XMLComponentManager): Unit = {
    try {
      fContinueAfterFatalError = componentManager.getFeature(CONTINUE_AFTER_FATAL_ERROR)
    } catch {
      case e: XNIException ⇒ fContinueAfterFatalError = false
    }
    fErrorHandler = componentManager.getProperty(ERROR_HANDLER).asInstanceOf[XMLErrorHandler]
  }

  /**
   * Returns a list of feature identifiers that are recognized by
   * this component. This method may return null if no features
   * are recognized by this component.
   */
  def getRecognizedFeatures: Array[String] = {
    RECOGNIZED_FEATURES.clone()
  }

  /**
   * Sets the state of a feature. This method is called by the component
   * manager any time after reset when a feature changes state.
   * 
   * *Note:* Components should silently ignore features
   * that do not affect the operation of the component.
   */
  def setFeature(featureId: String, state: Boolean): Unit = {
    if (featureId.startsWith(Constants.XERCES_FEATURE_PREFIX)) {
      val suffixLength = featureId.length - Constants.XERCES_FEATURE_PREFIX.length
      if (suffixLength == 
        Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE.length && 
        featureId.endsWith(Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE)) {
        fContinueAfterFatalError = state
      }
    }
  }

  def getFeature(featureId: String): Boolean = {
    if (featureId.startsWith(Constants.XERCES_FEATURE_PREFIX)) {
      val suffixLength = featureId.length - Constants.XERCES_FEATURE_PREFIX.length
      if (suffixLength == 
        Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE.length && 
        featureId.endsWith(Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE)) {
        return fContinueAfterFatalError
      }
    }
    false
  }

  /**
   * Returns a list of property identifiers that are recognized by
   * this component. This method may return null if no properties
   * are recognized by this component.
   */
  def getRecognizedProperties: Array[String] = {
    RECOGNIZED_PROPERTIES.clone()
  }

  /**
   * Sets the value of a property. This method is called by the component
   * manager any time after reset when a property changes value.
   * 
   * *Note:* Components should silently ignore properties
   * that do not affect the operation of the component.
   */
  def setProperty(propertyId: String, value: AnyRef): Unit = {
    if (propertyId.startsWith(Constants.XERCES_PROPERTY_PREFIX)) {
      val suffixLength = propertyId.length - Constants.XERCES_PROPERTY_PREFIX.length
      if (suffixLength == Constants.ERROR_HANDLER_PROPERTY.length && 
        propertyId.endsWith(Constants.ERROR_HANDLER_PROPERTY)) {
        fErrorHandler = value.asInstanceOf[XMLErrorHandler]
      }
    }
  }

  /**
   * Returns the default state for a feature, or null if this
   * component does not want to report a default value for this
   * feature.
   *
   * @param featureId The feature identifier.
   *
   * @since Xerces 2.2.0
   */
  def getFeatureDefault(featureId: String): java.lang.Boolean = {
    RECOGNIZED_FEATURES.indices.find(RECOGNIZED_FEATURES(_) == featureId)
      .map(FEATURE_DEFAULTS(_)).orNull
  }

  /**
   * Returns the default state for a property, or null if this
   * component does not want to report a default value for this
   * property.
   *
   * @param propertyId The property identifier.
   *
   * @since Xerces 2.2.0
   */
  def getPropertyDefault(propertyId: String): AnyRef = {
    RECOGNIZED_PROPERTIES.indices.find(RECOGNIZED_PROPERTIES(_) == propertyId)
      .map(PROPERTY_DEFAULTS(_)).orNull
  }

  /**
   * Get the internal XMLErrrorHandler.
   */
  def getErrorHandler: XMLErrorHandler = fErrorHandler
}
