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

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.io.StringReader
import java.net.URI

import org.orbeon.darius.impl.XMLEntityManager._
import org.orbeon.darius.impl.io.ASCIIReader
import org.orbeon.darius.impl.io.Latin1Reader
import org.orbeon.darius.impl.io.UCSReader
import org.orbeon.darius.impl.io.UTF16Reader
import org.orbeon.darius.impl.io.UTF8Reader
import org.orbeon.darius.impl.msg.XMLMessageFormatter
import org.orbeon.darius.impl.validation.ValidationManager
import org.orbeon.darius.util.AugmentationsImpl
import org.orbeon.darius.util.EncodingMap
import org.orbeon.darius.util.SecurityManager
import org.orbeon.darius.util.SymbolTable
import org.orbeon.darius.util.XMLChar
import org.orbeon.darius.util.XMLEntityDescriptionImpl
import org.orbeon.darius.util.XMLResourceIdentifierImpl
import org.orbeon.darius.xni.XMLResourceIdentifier
import org.orbeon.darius.xni.XNIException
import org.orbeon.darius.xni.parser.XMLComponent
import org.orbeon.darius.xni.parser.XMLComponentManager
import org.orbeon.darius.xni.parser.XMLConfigurationException
import org.orbeon.darius.xni.parser.XMLEntityResolver
import org.orbeon.darius.xni.parser.XMLInputSource

import scala.collection.mutable
import scala.util.control.Breaks

object XMLEntityManager {

  /**
   Default buffer size (2048).
   */
  val DEFAULT_BUFFER_SIZE = 2048

  /**
   Default buffer size before we've finished with the XMLDecl:
   */
  val DEFAULT_XMLDECL_BUFFER_SIZE = 64

  /**
   Default internal entity buffer size (512).
   */
  val DEFAULT_INTERNAL_BUFFER_SIZE = 512

  /**
   Feature identifier: validation.
   */
  protected val VALIDATION = Constants.SAX_FEATURE_PREFIX + Constants.VALIDATION_FEATURE

  /**
   Feature identifier: external general entities.
   */
  protected val EXTERNAL_GENERAL_ENTITIES = Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_GENERAL_ENTITIES_FEATURE

  /**
   Feature identifier: external parameter entities.
   */
  protected val EXTERNAL_PARAMETER_ENTITIES = Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_PARAMETER_ENTITIES_FEATURE

  /**
   Feature identifier: allow Java encodings.
   */
  protected val ALLOW_JAVA_ENCODINGS = Constants.XERCES_FEATURE_PREFIX + Constants.ALLOW_JAVA_ENCODINGS_FEATURE

  /**
   Feature identifier: warn on duplicate EntityDef
   */
  protected val WARN_ON_DUPLICATE_ENTITYDEF = Constants.XERCES_FEATURE_PREFIX + Constants.WARN_ON_DUPLICATE_ENTITYDEF_FEATURE

  /**
   Feature identifier: standard uri conformant
   */
  protected val STANDARD_URI_CONFORMANT = Constants.XERCES_FEATURE_PREFIX + Constants.STANDARD_URI_CONFORMANT_FEATURE

  protected val PARSER_SETTINGS = Constants.XERCES_FEATURE_PREFIX + Constants.PARSER_SETTINGS

  /**
   Property identifier: symbol table.
   */
  protected val SYMBOL_TABLE = Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY

  /**
   Property identifier: error reporter.
   */
  protected val ERROR_REPORTER = Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY

  /**
   Property identifier: entity resolver.
   */
  protected val ENTITY_RESOLVER = Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_RESOLVER_PROPERTY

  protected val VALIDATION_MANAGER = Constants.XERCES_PROPERTY_PREFIX + Constants.VALIDATION_MANAGER_PROPERTY

  /**
   property identifier: buffer size.
   */
  protected val BUFFER_SIZE = Constants.XERCES_PROPERTY_PREFIX + Constants.BUFFER_SIZE_PROPERTY

  /**
   property identifier: security manager.
   */
  protected val SECURITY_MANAGER = Constants.XERCES_PROPERTY_PREFIX + Constants.SECURITY_MANAGER_PROPERTY

  /**
   Recognized features.
   */
  private val RECOGNIZED_FEATURES = Array(VALIDATION, EXTERNAL_GENERAL_ENTITIES, EXTERNAL_PARAMETER_ENTITIES, ALLOW_JAVA_ENCODINGS, WARN_ON_DUPLICATE_ENTITYDEF, STANDARD_URI_CONFORMANT)

  /**
   Feature defaults.
   */
  private val FEATURE_DEFAULTS = Array[java.lang.Boolean](null, true, true, false, false, false)

  /**
   Recognized properties.
   */
  private val RECOGNIZED_PROPERTIES = Array(SYMBOL_TABLE, ERROR_REPORTER, ENTITY_RESOLVER, VALIDATION_MANAGER, BUFFER_SIZE, SECURITY_MANAGER)

  /**
   Property defaults.
   */
  private val PROPERTY_DEFAULTS = Array(null, null, null, null, new java.lang.Integer(DEFAULT_BUFFER_SIZE), null)

  private val XMLEntity = "[xml]".intern()

  private val DTDEntity = "[dtd]".intern()

  /**
   * Debug printing of buffer. This debugging flag works best when you
   * resize the DEFAULT_BUFFER_SIZE down to something reasonable like
   * 64 characters.
   */
  private val DEBUG_BUFFER = false

  /**
   Debug some basic entities.
   */
  private val DEBUG_ENTITIES = false

  /**
   Debug switching readers for encodings.
   */
  private val DEBUG_ENCODINGS = false

  private val DEBUG_RESOLVER = false

  private var gUserDir: String = _

  private var gUserDirURI: URI = _

  private val gNeedEscaping = new Array[Boolean](128)

  private val gAfterEscaping1 = new Array[Char](128)

  private val gAfterEscaping2 = new Array[Char](128)
  
  private val gHexChs = Array[Char]('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

  var i = 0
  while (i <= 0x1f) {
    gNeedEscaping(i) = true
    gAfterEscaping1(i) = gHexChs(i >> 4)
    gAfterEscaping2(i) = gHexChs(i & 0xf)
    i += 1
  }

  gNeedEscaping(0x7f) = true

  gAfterEscaping1(0x7f) = '7'

  gAfterEscaping2(0x7f) = 'F'

  val escChs = Array(' ', '<', '>', '#', '%', '"', '{', '}', '|', '\\', '^', '~', '[', ']', '`')

  val len = escChs.length

  var ch: Char = 0

  for (i ← 0 until len) {
    ch = escChs(i)
    gNeedEscaping(ch) = true
    gAfterEscaping1(ch) = gHexChs(ch >> 4)
    gAfterEscaping2(ch) = gHexChs(ch & 0xf)
  }

  /**
   * Expands a system id and returns the system id as a URI, if
   * it can be expanded. A return value of null means that the
   * identifier is already expanded. An exception thrown
   * indicates a failure to expand the id.
   *
   * @param systemId The systemId to be expanded.
   *
   * @return Returns the URI string representing the expanded system
   *         identifier. A null value indicates that the given
   *         system identifier is already expanded.
   *
   */
  def expandSystemId(systemId: String, baseSystemId: String, strict: Boolean): String = {
    if (systemId eq null) {
      return null
    }
    val systemURI = new URI(systemId)//, true
    if (systemURI.isAbsolute) {
      return systemId
    }
    val baseURI = new URI(baseSystemId)//, true
    baseURI.resolve(systemURI).toString
  }

  /**
   Prints the contents of the buffer.
   */
  def print(currentEntity: XMLEntityManager#ScannedEntity): Unit = {
    if (DEBUG_BUFFER) {
      if (currentEntity ne null) {
        System.out.print('[')
        System.out.print(currentEntity.count)
        System.out.print(' ')
        System.out.print(currentEntity.position)
        if (currentEntity.count > 0) {
          System.out.print(" \"")
          for (i ← 0 until currentEntity.count) {
            if (i == currentEntity.position) {
              System.out.print('^')
            }
            val c = currentEntity.ch(i)
            c match {
              case '\n' ⇒
                System.out.print("\\n")
              case '\r' ⇒
                System.out.print("\\r")
              case '\t' ⇒
                System.out.print("\\t")
              case '\\' ⇒
                System.out.print("\\\\")
              case _ ⇒
                System.out.print(c)
            }
          }
          if (currentEntity.position == currentEntity.count) {
            System.out.print('^')
          }
          System.out.print('"')
        }
        System.out.print(']')
        System.out.print(" @ ")
        System.out.print(currentEntity.lineNumber)
        System.out.print(',')
        System.out.print(currentEntity.columnNumber)
      } else {
        System.out.print("*NO CURRENT ENTITY*")
      }
    }
  }

  /**
   * Entity information.
   */
  abstract class Entity(val name: String, val inExternalSubset: Boolean) {
    def isEntityDeclInExternalSubset: Boolean = inExternalSubset
    def isExternal: Boolean
    def isUnparsed: Boolean
  }

  /**
   * Internal entity.
   */
  protected class InternalEntity(name: String, var text: String, inExternalSubset: Boolean)
    extends Entity(name, inExternalSubset) {

    /**
     Returns true if this is an external entity.
     */
    def isExternal: Boolean = false

    /**
     Returns true if this is an unparsed entity.
     */
    def isUnparsed: Boolean = false
  }

  /**
   * External entity.
   */
  protected class ExternalEntity(name: String, 
        var entityLocation: XMLResourceIdentifier, 
        var notation: String, 
        inExternalSubset: Boolean)
    extends Entity(name, inExternalSubset) {

    /**
     Returns true if this is an external entity.
     */
    def isExternal: Boolean = true

    /**
     Returns true if this is an unparsed entity.
     */
    def isUnparsed: Boolean = notation ne null
  }

  object EncodingInfo {

    /**
     UTF-8 *
     */
    val UTF_8 = new EncodingInfo("UTF-8", null, false)

    /**
     UTF-8, with BOM *
     */
    val UTF_8_WITH_BOM = new EncodingInfo("UTF-8", null, true)

    /**
     UTF-16, big-endian *
     */
    val UTF_16_BIG_ENDIAN = new EncodingInfo("UTF-16", true, false)

    /**
     UTF-16, big-endian with BOM *
     */
    val UTF_16_BIG_ENDIAN_WITH_BOM = new EncodingInfo("UTF-16", true, true)

    /**
     UTF-16, little-endian *
     */
    val UTF_16_LITTLE_ENDIAN = new EncodingInfo("UTF-16", false, false)

    /**
     UTF-16, little-endian with BOM *
     */
    val UTF_16_LITTLE_ENDIAN_WITH_BOM = new EncodingInfo("UTF-16", false, true)

    /**
     UCS-4, big-endian *
     */
    val UCS_4_BIG_ENDIAN = new EncodingInfo("ISO-10646-UCS-4", true, false)

    /**
     UCS-4, little-endian *
     */
    val UCS_4_LITTLE_ENDIAN = new EncodingInfo("ISO-10646-UCS-4", false, false)

    /**
     UCS-4, unusual byte-order (2143) or (3412) *
     */
    val UCS_4_UNUSUAL_BYTE_ORDER = new EncodingInfo("ISO-10646-UCS-4", null, false)

    /**
     EBCDIC *
     */
    val EBCDIC = new EncodingInfo("CP037", null, false)
  }

  /**
   * Information about auto-detectable encodings.
   */
  private[impl] class EncodingInfo private (val encoding: String, val isBigEndian: java.lang.Boolean, val hasBOM: Boolean)
  
  object ByteBufferPool {
    private val DEFAULT_POOL_SIZE = 3
  }

  /**
   * Pool of byte buffers for the java.io.Readers.
   */
  private class ByteBufferPool(var fPoolSize: Int, var fBufferSize: Int) {

    private var fByteBufferPool: Array[Array[Byte]] = new Array[Array[Byte]](fPoolSize)

    private var fDepth: Int = 0

    def this(bufferSize: Int) {
      this(ByteBufferPool.DEFAULT_POOL_SIZE, bufferSize)
    }

    /**
     Retrieves a byte buffer from the pool. *
     */
    def getBuffer: Array[Byte] = {
      if (fDepth > 0) {
        fDepth -= 1
        fByteBufferPool(fDepth)
      } else
        new Array[Byte](fBufferSize)
    }

    /**
     Returns byte buffer to pool. *
     */
    def returnBuffer(buffer: Array[Byte]): Unit = {
      if (fDepth < fByteBufferPool.length) {
        fByteBufferPool(fDepth) = buffer
        fDepth += 1
      }
    }

    /**
     Sets the size of the buffers and dumps the old pool. *
     */
    def setBufferSize(bufferSize: Int): Unit = {
      fBufferSize = bufferSize
      fByteBufferPool = new Array[Array[Byte]](fPoolSize)
      fDepth = 0
    }
  }

  /**
   * Buffer used in entity manager to reuse character arrays instead
   * of creating new ones every time.
   */
  private[impl] class CharacterBuffer(val isExternal: Boolean, size: Int) {

    /**
     character buffer
     */
    private[impl] val ch = new Array[Char](size)
  }

  object CharacterBufferPool {
    private val DEFAULT_POOL_SIZE = 3
  }

  /**
   * Stores a number of character buffers and provides it to the entity
   * manager to use when an entity is seen.
   */
  private class CharacterBufferPool(var fPoolSize: Int, var fExternalBufferSize: Int, var fInternalBufferSize: Int) {

    private var fInternalBufferPool: Array[CharacterBuffer] = _
    private var fExternalBufferPool: Array[CharacterBuffer] = _
    private var fInternalTop: Int = _
    private var fExternalTop: Int = _

    init()

    def this(externalBufferSize: Int, internalBufferSize: Int) {
      this(CharacterBufferPool.DEFAULT_POOL_SIZE, externalBufferSize, internalBufferSize)
    }

    /**
     Initializes buffer pool. *
     */
    private def init(): Unit = {
      fInternalBufferPool = new Array[CharacterBuffer](fPoolSize)
      fExternalBufferPool = new Array[CharacterBuffer](fPoolSize)
      fInternalTop = -1
      fExternalTop = -1
    }

    /**
     Retrieves buffer from pool. *
     */
    def getBuffer(external: Boolean): CharacterBuffer = {
      if (external) {
        if (fExternalTop > -1) {
          val result = fExternalBufferPool(fExternalTop)
          fExternalTop -= 1
          result
        } else {
          new CharacterBuffer(true, fExternalBufferSize)
        }
      } else {
        if (fInternalTop > -1) {
          val result = fInternalBufferPool(fInternalTop)
          fInternalTop -= 1
          result
        } else {
          new CharacterBuffer(false, fInternalBufferSize)
        }
      }
    }

    /**
     Returns buffer to pool. *
     */
    def returnBuffer(buffer: CharacterBuffer): Unit = {
      if (buffer.isExternal) {
        if (fExternalTop < fExternalBufferPool.length - 1) {
          fExternalTop += 1
          fExternalBufferPool(fExternalTop) = buffer
        }
      } else if (fInternalTop < fInternalBufferPool.length - 1) {
        fInternalTop += 1
        fInternalBufferPool(fInternalTop) = buffer
      }
    }

    /**
     Sets the size of external buffers and dumps the old pool. *
     */
    def setExternalBufferSize(bufferSize: Int): Unit = {
      fExternalBufferSize = bufferSize
      fExternalBufferPool = new Array[CharacterBuffer](fPoolSize)
      fExternalTop = -1
    }
  }
}

/**
 * The entity manager handles the registration of general and parameter
 * entities; resolves entities; and starts entities. The entity manager
 * is a central component in a standard parser configuration and this
 * class works directly with the entity scanner to manage the underlying
 * xni.
 * 
 * This component requires the following features and properties from the
 * component manager that uses it:
 * 
 *  - http://xml.org/sax/features/validation
 *  - http://xml.org/sax/features/external-general-entities
 *  - http://xml.org/sax/features/external-parameter-entities
 *  - http://apache.org/xml/features/allow-java-encodings
 *  - http://apache.org/xml/properties/internal/symbol-table
 *  - http://apache.org/xml/properties/internal/error-reporter
 *  - http://apache.org/xml/properties/internal/entity-resolver
 * 
 */
class XMLEntityManager(entityManager: XMLEntityManager) extends XMLComponent with XMLEntityResolver {

  /**
   * Validation. This feature identifier is:
   * http://xml.org/sax/features/validation
   */
  protected var fValidation: Boolean = _

  /**
   * External general entities. This feature identifier is:
   * http://xml.org/sax/features/external-general-entities
   */
  protected var fExternalGeneralEntities: Boolean = true

  /**
   * External parameter entities. This feature identifier is:
   * http://xml.org/sax/features/external-parameter-entities
   */
  protected var fExternalParameterEntities: Boolean = true

  /**
   * Allow Java encoding names. This feature identifier is:
   * http://apache.org/xml/features/allow-java-encodings
   */
  protected var fAllowJavaEncodings: Boolean = _

  /**
   warn on duplicate Entity declaration.
   *  http://apache.org/xml/features/warn-on-duplicate-entitydef
   */
  protected var fWarnDuplicateEntityDef: Boolean = _

  /**
   * standard uri conformant (strict uri).
   * http://apache.org/xml/features/standard-uri-conformant
   */
  protected var fStrictURI: Boolean = _

  /**
   * Symbol table. This property identifier is:
   * http://apache.org/xml/properties/internal/symbol-table
   */
  protected var fSymbolTable: SymbolTable = _

  /**
   * Error reporter. This property identifier is:
   * http://apache.org/xml/properties/internal/error-reporter
   */
  protected var fErrorReporter: XMLErrorReporter = _

  /**
   * Entity resolver. This property identifier is:
   * http://apache.org/xml/properties/internal/entity-resolver
   */
  protected var fEntityResolver: XMLEntityResolver = _

  /**
   * Validation manager. This property identifier is:
   * http://apache.org/xml/properties/internal/validation-manager
   */
  protected var fValidationManager: ValidationManager = _

  /**
   * Buffer size. We get this value from a property. The default size
   * is used if the input buffer size property is not specified.
   * REVISIT: do we need a property for internal entity buffer size?
   */
  protected var fBufferSize: Int = DEFAULT_BUFFER_SIZE

  protected var fSecurityManager: SecurityManager = null

  /**
   * True if the document entity is standalone. This should really
   * only be set by the document source (e.g. XMLDocumentScanner).
   */
  protected var fStandalone: Boolean = _

  /**
   * True if the current document contains parameter entity references.
   */
  protected var fHasPEReferences: Boolean = _

  protected var fInExternalSubset: Boolean = false

  /**
   Entity handler.
   */
  protected var fEntityHandler: XMLEntityHandler = _

  /**
   Current entity scanner.
   */
  protected var fEntityScanner: XMLEntityScanner = _

  /**
   XML 1.0 entity scanner.
   */
  protected var fXML10EntityScanner: XMLEntityScanner = _

  /**
   XML 1.1 entity scanner.
   */
  protected var fXML11EntityScanner: XMLEntityScanner = _

  protected var fEntityExpansionLimit: Int = 0

  protected var fEntityExpansionCount: Int = 0

  /**
   Entities.
   */
  protected val fEntities = new mutable.HashMap[String, Entity]()

  /**
   Entity stack.
   */
  protected val fEntityStack = new mutable.Stack[Entity]()

  /**
   Current entity.
   */
  protected[impl] var fCurrentEntity: ScannedEntity = _

  /**
   Shared declared entities.
   */
  protected var fDeclaredEntities = if (entityManager ne null) entityManager.getDeclaredEntities else null

  /**
   Resource identifier.
   */
  private val fResourceIdentifier = new XMLResourceIdentifierImpl()

  /**
   Augmentations for entities.
   */
  private val fEntityAugs = new AugmentationsImpl()

  /**
   Pool of byte buffers for single byte and variable width encodings, such as US-ASCII and UTF-8.
   */
  private val fSmallByteBufferPool = new ByteBufferPool(fBufferSize)

  /**
   Pool of byte buffers for 2-byte encodings, such as UTF-16. *
   */
  private val fLargeByteBufferPool = new ByteBufferPool(fBufferSize << 1)

  /**
   Temporary storage for the current entity's byte buffer.
   */
  private var fTempByteBuffer: Array[Byte] = null

  /**
   Pool of character buffers.
   */
  private val fCharacterBufferPool = new CharacterBufferPool(fBufferSize, DEFAULT_INTERNAL_BUFFER_SIZE)

  setScannerVersion(Constants.XML_VERSION_1_0)

  /**
   Default constructor.
   */
  def this() {
    this(null)
  }

  /**
   * Sets whether the document entity is standalone.
   *
   * @param standalone True if document entity is standalone.
   */
  def setStandalone(standalone: Boolean): Unit = {
    fStandalone = standalone
  }

  /**
   Returns true if the document entity is standalone.
   */
  def isStandalone: Boolean = fStandalone

  /**
   * Notifies the entity manager that the current document
   * being processed contains parameter entity references.
   */
  def notifyHasPEReferences(): Unit = {
    fHasPEReferences = true
  }

  /**
   * Returns true if the document contains parameter entity references.
   */
  def hasPEReferences: Boolean = fHasPEReferences

  /**
   * Sets the entity handler. When an entity starts and ends, the
   * entity handler is notified of the change.
   *
   * @param entityHandler The new entity handler.
   */
  def setEntityHandler(entityHandler: XMLEntityHandler): Unit = {
    fEntityHandler = entityHandler
  }

  def getCurrentResourceIdentifier: XMLResourceIdentifier = fResourceIdentifier

  def getCurrentEntity: ScannedEntity = fCurrentEntity

  /**
   * Adds an internal entity declaration.
   * 
   * *Note:* This method ignores subsequent entity
   * declarations.
   * 
   * *Note:* The name should be a unique symbol. The
   * SymbolTable can be used for this purpose.
   *
   * @param name The name of the entity.
   * @param text The text of the entity.
   */
  def addInternalEntity(name: String, text: String): Unit = {
    if (!fEntities.contains(name)) {
      val entity = new InternalEntity(name, text, fInExternalSubset)
      fEntities.put(name, entity)
    } else {
      if (fWarnDuplicateEntityDef) {
        fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "MSG_DUPLICATE_ENTITY_DEFINITION", 
          Array(name), XMLErrorReporter.SEVERITY_WARNING)
      }
    }
  }

  /**
   * Adds an external entity declaration.
   * 
   * *Note:* This method ignores subsequent entity
   * declarations.
   * 
   * *Note:* The name should be a unique symbol. The
   * SymbolTable can be used for this purpose.
   *
   * @param name         The name of the entity.
   * @param publicId     The public identifier of the entity.
   * @param literalSystemId     The system identifier of the entity.
   * @param _baseSystemId The base system identifier of the entity.
   *                     This is the system identifier of the entity
   *                     where *the entity being added* and
   *                     is used to expand the system identifier when
   *                     the system identifier is a relative URI.
   *                     When null the system identifier of the first
   *                     external entity on the stack is used instead.
   */
  def addExternalEntity(name: String, 
      publicId: String, 
      literalSystemId: String, 
      _baseSystemId: String): Unit = {
    var baseSystemId = _baseSystemId
    if (!fEntities.contains(name)) {
      if (baseSystemId eq null) {
        val size = fEntityStack.size
        if (size == 0 && (fCurrentEntity ne null) && (fCurrentEntity.entityLocation ne null)) {
          baseSystemId = fCurrentEntity.entityLocation.getExpandedSystemId
        }
        var i = size - 1
        val whileBreaks = new Breaks
        whileBreaks.breakable {
          while (i >= 0) {
            val externalEntity = fEntityStack(i).asInstanceOf[ScannedEntity]
            if ((externalEntity.entityLocation ne null) && 
              (externalEntity.entityLocation.getExpandedSystemId ne null)) {
              baseSystemId = externalEntity.entityLocation.getExpandedSystemId
              whileBreaks.break()
            }
            i -= 1
          }
        }
      }
      val entity = new ExternalEntity(name, new XMLEntityDescriptionImpl(name, publicId, literalSystemId, 
        baseSystemId, expandSystemId(literalSystemId, baseSystemId, strict = false)), null, fInExternalSubset)
      fEntities.put(name, entity)
    } else {
      if (fWarnDuplicateEntityDef) {
        fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "MSG_DUPLICATE_ENTITY_DEFINITION", 
          Array(name), XMLErrorReporter.SEVERITY_WARNING)
      }
    }
  }

  /**
   * Checks whether an entity given by name is external.
   *
   * @param entityName The name of the entity to check.
   * @return True if the entity is external, false otherwise
   * (including when the entity is not declared).
   */
  def isExternalEntity(entityName: String): Boolean = {
    val entity = fEntities.get(entityName).orNull
    if (entity eq null) {
      return false
    }
    entity.isExternal
  }

  /**
   * Checks whether the declaration of an entity given by name is
   // in the external subset.
   *
   * @param entityName The name of the entity to check.
   * @return True if the entity was declared in the external subset, false otherwise
   *           (including when the entity is not declared).
   */
  def isEntityDeclInExternalSubset(entityName: String): Boolean = {
    val entity = fEntities.get(entityName).orNull
    if (entity eq null) {
      return false
    }
    entity.isEntityDeclInExternalSubset
  }

  /**
   * Adds an unparsed entity declaration.
   * 
   * *Note:* This method ignores subsequent entity
   * declarations.
   * 
   * *Note:* The name should be a unique symbol. The
   * SymbolTable can be used for this purpose.
   *
   * @param name     The name of the entity.
   * @param publicId The public identifier of the entity.
   * @param systemId The system identifier of the entity.
   * @param notation The name of the notation.
   */
  def addUnparsedEntity(name: String, 
      publicId: String, 
      systemId: String, 
      baseSystemId: String, 
      notation: String): Unit = {
    if (!fEntities.contains(name)) {
      val entity = new ExternalEntity(name, new XMLEntityDescriptionImpl(name, publicId, systemId, baseSystemId, 
        null), notation, fInExternalSubset)
      fEntities.put(name, entity)
    } else {
      if (fWarnDuplicateEntityDef) {
        fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "MSG_DUPLICATE_ENTITY_DEFINITION", 
          Array(name), XMLErrorReporter.SEVERITY_WARNING)
      }
    }
  }

  /**
   * Checks whether an entity given by name is unparsed.
   *
   * @param entityName The name of the entity to check.
   * @return True if the entity is unparsed, false otherwise
   *          (including when the entity is not declared).
   */
  def isUnparsedEntity(entityName: String): Boolean = {
    val entity = fEntities.get(entityName).orNull
    if (entity eq null) {
      return false
    }
    entity.isUnparsed
  }

  /**
   * Checks whether an entity given by name is declared.
   *
   * @param entityName The name of the entity to check.
   * @return True if the entity is declared, false otherwise.
   */
  def isDeclaredEntity(entityName: String): Boolean = {
    val entity = fEntities.get(entityName).orNull
    entity ne null
  }

  /**
   * Resolves the specified public and system identifiers. This
   * method first attempts to resolve the entity based on the
   * EntityResolver registered by the application. If no entity
   * resolver is registered or if the registered entity handler
   * is unable to resolve the entity, then default entity
   * resolution will occur.
   *
   * @param resourceIdentifier The XMLResourceIdentifier for the resource to resolve.
   *
   * @return Returns an input source that wraps the resolved entity.
   *         This method will never return null.
   *
   * @throws IOException  Thrown on i/o error.
   * @throws XNIException Thrown by entity resolver to signal an error.
   */
  def resolveEntity(resourceIdentifier: XMLResourceIdentifier): XMLInputSource = {
    if (resourceIdentifier eq null) return null
    val publicId = resourceIdentifier.getPublicId
    val literalSystemId = resourceIdentifier.getLiteralSystemId
    var baseSystemId = resourceIdentifier.getBaseSystemId
    var expandedSystemId = resourceIdentifier.getExpandedSystemId
    var needExpand = expandedSystemId eq null
    if ((baseSystemId eq null) && (fCurrentEntity ne null) && (fCurrentEntity.entityLocation ne null)) {
      baseSystemId = fCurrentEntity.entityLocation.getExpandedSystemId
      if (baseSystemId ne null) needExpand = true
    }
    var xmlInputSource: XMLInputSource = null
    if (fEntityResolver ne null) {
      if (needExpand) {
        expandedSystemId = expandSystemId(literalSystemId, baseSystemId, strict = false)
      }
      resourceIdentifier.setBaseSystemId(baseSystemId)
      resourceIdentifier.setExpandedSystemId(expandedSystemId)
      xmlInputSource = fEntityResolver.resolveEntity(resourceIdentifier)
    }
    if (xmlInputSource eq null) {
      xmlInputSource = new XMLInputSource(publicId, literalSystemId, baseSystemId)
    }
    if (DEBUG_RESOLVER) {
      System.err.println("XMLEntityManager.resolveEntity(" + publicId + ")")
      System.err.println(" = " + xmlInputSource)
    }
    xmlInputSource
  }

  /**
   * Starts a named entity.
   *
   * @param entityName The name of the entity to start.
   * @param literal    True if this entity is started within a literal
   *                   value.
   *
   * @throws IOException  Thrown on i/o error.
   * @throws XNIException Thrown by entity handler to signal an error.
   */
  def startEntity(entityName: String, literal: Boolean): Unit = {
    val entity = fEntities.get(entityName).orNull
    if (entity eq null) {
      if (fEntityHandler ne null) {
        val encoding: String = null
        fResourceIdentifier.clear()
        fEntityAugs.removeAllItems()
        fEntityAugs.putItem(Constants.ENTITY_SKIPPED, true)
        fEntityHandler.startEntity(entityName, fResourceIdentifier, encoding, fEntityAugs)
        fEntityAugs.removeAllItems()
        fEntityAugs.putItem(Constants.ENTITY_SKIPPED, true)
        fEntityHandler.endEntity(entityName, fEntityAugs)
      }
      return
    }
    val external = entity.isExternal
    if (external && 
      ((fValidationManager eq null) || !fValidationManager.isCachedDTD)) {
      val unparsed = entity.isUnparsed
      val parameter = entityName.startsWith("%")
      val general = !parameter
      if (unparsed || (general && !fExternalGeneralEntities) || (parameter && !fExternalParameterEntities)) {
        if (fEntityHandler ne null) {
          fResourceIdentifier.clear()
          val encoding: String = null
          val externalEntity = entity.asInstanceOf[ExternalEntity]
          val extLitSysId = if (externalEntity.entityLocation ne null) externalEntity.entityLocation.getLiteralSystemId else null
          val extBaseSysId = if (externalEntity.entityLocation ne null) externalEntity.entityLocation.getBaseSystemId else null
          val expandedSystemId = expandSystemId(extLitSysId, extBaseSysId, strict = false)
          fResourceIdentifier.setValues(if (externalEntity.entityLocation ne null) externalEntity.entityLocation.getPublicId else null, 
            extLitSysId, extBaseSysId, expandedSystemId)
          fEntityAugs.removeAllItems()
          fEntityAugs.putItem(Constants.ENTITY_SKIPPED, true)
          fEntityHandler.startEntity(entityName, fResourceIdentifier, encoding, fEntityAugs)
          fEntityAugs.removeAllItems()
          fEntityAugs.putItem(Constants.ENTITY_SKIPPED, true)
          fEntityHandler.endEntity(entityName, fEntityAugs)
        }
        return
      }
    }
    val size = fEntityStack.size
    var i = size
    while (i >= 0) {
      var activeEntity = if (i == size) fCurrentEntity else fEntityStack(i)
      if (activeEntity.name == entityName) {
        val path = new StringBuffer(entityName)
        for (j ← i + 1 until size) {
          activeEntity = fEntityStack(j)
          path.append(" -> ")
          path.append(activeEntity.name)
        }
        path.append(" -> ")
        path.append(fCurrentEntity.name)
        path.append(" -> ")
        path.append(entityName)
        fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "RecursiveReference", Array(entityName, path.toString), 
          XMLErrorReporter.SEVERITY_FATAL_ERROR)
        if (fEntityHandler ne null) {
          fResourceIdentifier.clear()
          val encoding: String = null
          if (external) {
            val externalEntity = entity.asInstanceOf[ExternalEntity]
            val extLitSysId = if (externalEntity.entityLocation ne null) externalEntity.entityLocation.getLiteralSystemId else null
            val extBaseSysId = if (externalEntity.entityLocation ne null) externalEntity.entityLocation.getBaseSystemId else null
            val expandedSystemId = expandSystemId(extLitSysId, extBaseSysId, strict = false)
            fResourceIdentifier.setValues(if (externalEntity.entityLocation ne null) externalEntity.entityLocation.getPublicId else null, 
              extLitSysId, extBaseSysId, expandedSystemId)
          }
          fEntityAugs.removeAllItems()
          fEntityAugs.putItem(Constants.ENTITY_SKIPPED, true)
          fEntityHandler.startEntity(entityName, fResourceIdentifier, encoding, fEntityAugs)
          fEntityAugs.removeAllItems()
          fEntityAugs.putItem(Constants.ENTITY_SKIPPED, true)
          fEntityHandler.endEntity(entityName, fEntityAugs)
        }
        return
      }
      i -= 1
    }
    val xmlInputSource =
      if (external) {
        val externalEntity = entity.asInstanceOf[ExternalEntity]
        resolveEntity(externalEntity.entityLocation)
      } else {
        val internalEntity = entity.asInstanceOf[InternalEntity]
        val reader = new StringReader(internalEntity.text)
        new XMLInputSource(null, null, null, reader, null)
      }
    startEntity(entityName, xmlInputSource, literal, external)
  }

  /**
   * Starts the document entity. The document entity has the "[xml]"
   * pseudo-name.
   *
   * @param xmlInputSource The input source of the document entity.
   *
   * @throws IOException  Thrown on i/o error.
   * @throws XNIException Thrown by entity handler to signal an error.
   */
  def startDocumentEntity(xmlInputSource: XMLInputSource): Unit = {
    startEntity(XMLEntity, xmlInputSource, literal = false, isExternal = true)
  }

  /**
   * Starts the DTD entity. The DTD entity has the "[dtd]"
   * pseudo-name.
   *
   * @param xmlInputSource The input source of the DTD entity.
   *
   * @throws IOException  Thrown on i/o error.
   * @throws XNIException Thrown by entity handler to signal an error.
   */
  def startDTDEntity(xmlInputSource: XMLInputSource): Unit = {
    startEntity(DTDEntity, xmlInputSource, literal = false, isExternal = true)
  }

  def startExternalSubset(): Unit = {
    fInExternalSubset = true
  }

  def endExternalSubset(): Unit = {
    fInExternalSubset = false
  }

  /**
   * Starts an entity.
   * 
   * This method can be used to insert an application defined XML
   * entity stream into the parsing stream.
   *
   * @param name           The name of the entity.
   * @param xmlInputSource The input source of the entity.
   * @param literal        True if this entity is started within a
   *                       literal value.
   * @param isExternal    whether this entity should be treated as an internal or external entity.
   *
   * @throws IOException  Thrown on i/o error.
   * @throws XNIException Thrown by entity handler to signal an error.
   */
  def startEntity(name: String, 
      xmlInputSource: XMLInputSource, 
      literal: Boolean, 
      isExternal: Boolean): Unit = {
    val encoding = setupCurrentEntity(name, xmlInputSource, literal, isExternal)
    if ((fSecurityManager ne null) && fEntityExpansionCount > fEntityExpansionLimit) {
      fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "EntityExpansionLimitExceeded", Array(new java.lang.Integer(fEntityExpansionLimit)), 
        XMLErrorReporter.SEVERITY_FATAL_ERROR)
      fEntityExpansionCount = 0
    }
    fEntityExpansionCount += 1
    if (fEntityHandler ne null) {
      fEntityHandler.startEntity(name, fResourceIdentifier, encoding, null)
    }
  }

  /**
   * This method uses the passed-in XMLInputSource to make
   * fCurrentEntity usable for reading.
   * @param name  name of the entity (XML is it's the document entity)
   * @param xmlInputSource    the input source, with sufficient information
   *      to begin scanning characters.
   * @param literal        True if this entity is started within a
   *                       literal value.
   * @param isExternal    whether this entity should be treated as an internal or external entity.
   * @throws IOException  if anything can't be read
   *  XNIException    If any parser-specific goes wrong.
   * @return the encoding of the new entity or null if a character stream was employed
   */
  def setupCurrentEntity(name: String, 
      xmlInputSource: XMLInputSource, 
      literal: Boolean, 
      isExternal: Boolean): String = {
    val publicId = xmlInputSource.getPublicId
    val literalSystemId = xmlInputSource.getSystemId
    var baseSystemId = xmlInputSource.getBaseSystemId
    var encoding = xmlInputSource.getEncoding
    val encodingExternallySpecified = encoding ne null
    var isBigEndian: java.lang.Boolean = null
    fTempByteBuffer = null
    var stream: InputStream = null
    var reader = xmlInputSource.getCharacterStream
    val expandedSystemId = expandSystemId(literalSystemId, baseSystemId, fStrictURI)
    if (baseSystemId eq null) {
      baseSystemId = expandedSystemId
    }
    if (reader eq null) {
      stream = xmlInputSource.getByteStream
      assert(stream ne null)
      val rewindableStream = new RewindableInputStream(stream)
      stream = rewindableStream
      if (encoding eq null) {
        val b4 = new Array[Byte](4)
        var count = 0
        while (count < 4) {
          b4(count) = rewindableStream.readAndBuffer().toByte
          count += 1
        }
        if (count == 4) {
          val info = getEncodingInfo(b4, count)
          encoding = info.encoding
          isBigEndian = info.isBigEndian
          stream.reset()
          if (info.hasBOM) {
            if (encoding == "UTF-8") {
              stream.skip(3)
            } else if (encoding == "UTF-16") {
              stream.skip(2)
            }
          }
          reader = createReader(stream, encoding, isBigEndian)
        } else {
          reader = createReader(stream, encoding, isBigEndian)
        }
      } else {
        encoding = encoding.toUpperCase//Locale.ENGLISH
        if (encoding == "UTF-8") {
          val b3 = new Array[Int](3)
          var count = 0
          val whileBreaks = new Breaks
          whileBreaks.breakable {
            while (count < 3) {
              b3(count) = rewindableStream.readAndBuffer()
              if (b3(count) == -1)
                whileBreaks.break()
              count += 1
            }
          }
          if (count == 3) {
            if (b3(0) != 0xEF || b3(1) != 0xBB || b3(2) != 0xBF) {
              stream.reset()
            }
          } else {
            stream.reset()
          }
          reader = createReader(stream, "UTF-8", isBigEndian)
        } else if (encoding == "UTF-16") {
          val b4 = new Array[Int](4)
          var count = 0
          val whileBreaks = new Breaks
          whileBreaks.breakable {
            while (count < 4) {
              b4(count) = rewindableStream.readAndBuffer()
              if (b4(count) == -1)
                whileBreaks.break()
              count += 1
            }
          }
          stream.reset()
          if (count >= 2) {
            val b0 = b4(0)
            val b1 = b4(1)
            if (b0 == 0xFE && b1 == 0xFF) {
              isBigEndian = true
              stream.skip(2)
            } else if (b0 == 0xFF && b1 == 0xFE) {
              isBigEndian = false
              stream.skip(2)
            } else if (count == 4) {
              val b2 = b4(2)
              val b3 = b4(3)
              if (b0 == 0x00 && b1 == 0x3C && b2 == 0x00 && b3 == 0x3F) {
                isBigEndian = true
              }
              if (b0 == 0x3C && b1 == 0x00 && b2 == 0x3F && b3 == 0x00) {
                isBigEndian = false
              }
            }
          }
          reader = createReader(stream, "UTF-16", isBigEndian)
        } else if (encoding == "ISO-10646-UCS-4") {
          val b4 = new Array[Int](4)
          var count = 0
          val whileBreaks = new Breaks
          whileBreaks.breakable {
            while (count < 4) {
              b4(count) = rewindableStream.readAndBuffer()
              if (b4(count) == -1)
                whileBreaks.break()
              count += 1
            }
          }
          stream.reset()
          if (count == 4) {
            if (b4(0) == 0x00 && b4(1) == 0x00 && b4(2) == 0x00 && b4(3) == 0x3C) {
              isBigEndian = true
            } else if (b4(0) == 0x3C && b4(1) == 0x00 && b4(2) == 0x00 && b4(3) == 0x00) {
              isBigEndian = false
            }
          }
          reader = createReader(stream, encoding, isBigEndian)
        } else if (encoding == "ISO-10646-UCS-2") {
          val b4 = new Array[Int](4)
          var count = 0
          val whileBreaks = new Breaks
          whileBreaks.breakable {
            while (count < 4) {
              b4(count) = rewindableStream.readAndBuffer()
              if (b4(count) == -1)
                whileBreaks.break()
              count += 1
            }
          }
          stream.reset()
          if (count == 4) {
            if (b4(0) == 0x00 && b4(1) == 0x3C && b4(2) == 0x00 && b4(3) == 0x3F) {
              isBigEndian = true
            } else if (b4(0) == 0x3C && b4(1) == 0x00 && b4(2) == 0x3F && b4(3) == 0x00) {
              isBigEndian = false
            }
          }
          reader = createReader(stream, encoding, isBigEndian)
        } else {
          reader = createReader(stream, encoding, isBigEndian)
        }
      }
      if (DEBUG_ENCODINGS) {
        println("$$$ no longer wrapping reader in OneCharReader")
      }
    }
    fReaderStack.push(reader)
    if (fCurrentEntity ne null) {
      fEntityStack.push(fCurrentEntity)
    }
    fCurrentEntity = new ScannedEntity(name, new XMLResourceIdentifierImpl(publicId, literalSystemId, 
      baseSystemId, expandedSystemId), stream, reader, fTempByteBuffer, encoding, literal, false, isExternal)
    fCurrentEntity.setEncodingExternallySpecified(encodingExternallySpecified)
    fEntityScanner.setCurrentEntity(fCurrentEntity)
    fResourceIdentifier.setValues(publicId, literalSystemId, baseSystemId, expandedSystemId)
    encoding
  }

  def setScannerVersion(version: Short): Unit = {
    if (fXML10EntityScanner eq null) {
      fXML10EntityScanner = new XMLEntityScanner()
    }
    fXML10EntityScanner.reset(fSymbolTable, this, fErrorReporter)
    fEntityScanner = fXML10EntityScanner
    fEntityScanner.setCurrentEntity(fCurrentEntity)
  }

  /**
   Returns the entity scanner.
   */
  def getEntityScanner: XMLEntityScanner = {
    if (fEntityScanner eq null) {
      if (fXML10EntityScanner eq null) {
        fXML10EntityScanner = new XMLEntityScanner()
      }
      fXML10EntityScanner.reset(fSymbolTable, this, fErrorReporter)
      fEntityScanner = fXML10EntityScanner
    }
    fEntityScanner
  }

  protected var fReaderStack = new mutable.Stack[Reader]()

  /**
   * Close all opened InputStreams and Readers opened by this parser.
   */
  def closeReaders(): Unit = {
    var i = fReaderStack.size - 1
    while (i >= 0) {
      try {
        fReaderStack.pop().close()
      } catch {
        case e: IOException ⇒ 
      }
      i -= 1
    }
  }

  /**
   * Resets the component. The component can query the component manager
   * about any features and properties that affect the operation of the
   * component.
   */
  def reset(componentManager: XMLComponentManager): Unit = {
    var parser_settings: Boolean = false
    try {
      parser_settings = componentManager.getFeature(PARSER_SETTINGS)
    } catch {
      case e: XMLConfigurationException ⇒ parser_settings = true
    }
		if (! parser_settings) {
			reset()
			return
		}
    try {
      fValidation = componentManager.getFeature(VALIDATION)
    } catch {
      case e: XMLConfigurationException ⇒ fValidation = false
    }
    try {
      fExternalGeneralEntities = componentManager.getFeature(EXTERNAL_GENERAL_ENTITIES)
    } catch {
      case e: XMLConfigurationException ⇒ fExternalGeneralEntities = true
    }
    try {
      fExternalParameterEntities = componentManager.getFeature(EXTERNAL_PARAMETER_ENTITIES)
    } catch {
      case e: XMLConfigurationException ⇒ fExternalParameterEntities = true
    }
    try {
      fAllowJavaEncodings = componentManager.getFeature(ALLOW_JAVA_ENCODINGS)
    } catch {
      case e: XMLConfigurationException ⇒ fAllowJavaEncodings = false
    }
    try {
      fWarnDuplicateEntityDef = componentManager.getFeature(WARN_ON_DUPLICATE_ENTITYDEF)
    } catch {
      case e: XMLConfigurationException ⇒ fWarnDuplicateEntityDef = false
    }
    try {
      fStrictURI = componentManager.getFeature(STANDARD_URI_CONFORMANT)
    } catch {
      case e: XMLConfigurationException ⇒ fStrictURI = false
    }
    fSymbolTable = componentManager.getProperty(SYMBOL_TABLE).asInstanceOf[SymbolTable]
    fErrorReporter = componentManager.getProperty(ERROR_REPORTER).asInstanceOf[XMLErrorReporter]
    try {
      fEntityResolver = componentManager.getProperty(ENTITY_RESOLVER).asInstanceOf[XMLEntityResolver]
    } catch {
      case e: XMLConfigurationException ⇒ fEntityResolver = null
    }
    try {
      fValidationManager = componentManager.getProperty(VALIDATION_MANAGER).asInstanceOf[ValidationManager]
    } catch {
      case e: XMLConfigurationException ⇒ fValidationManager = null
    }
    try {
      fSecurityManager = componentManager.getProperty(SECURITY_MANAGER).asInstanceOf[SecurityManager]
    } catch {
      case e: XMLConfigurationException ⇒ fSecurityManager = null
    }
    reset()
  }

  def reset(): Unit = {
    fEntityExpansionLimit = if (fSecurityManager ne null) fSecurityManager.entityExpansionLimit else 0
    fStandalone = false
    fHasPEReferences = false
    fEntities.clear()
    fEntityStack.clear()
    fEntityExpansionCount = 0
    fCurrentEntity = null
    if (fXML10EntityScanner ne null) {
      fXML10EntityScanner.reset(fSymbolTable, this, fErrorReporter)
    }
    if (fXML11EntityScanner ne null) {
      fXML11EntityScanner.reset(fSymbolTable, this, fErrorReporter)
    }
    if (DEBUG_ENTITIES) {
      addInternalEntity("text", "Hello, World.")
      addInternalEntity("empty-element", "<foo/>")
      addInternalEntity("balanced-element", "<foo></foo>")
      addInternalEntity("balanced-element-with-text", "<foo>Hello, World</foo>")
      addInternalEntity("balanced-element-with-entity", "<foo>&text;</foo>")
      addInternalEntity("unbalanced-entity", "<foo>")
      addInternalEntity("recursive-entity", "<foo>&recursive-entity2;</foo>")
      addInternalEntity("recursive-entity2", "<bar>&recursive-entity3;</bar>")
      addInternalEntity("recursive-entity3", "<baz>&recursive-entity;</baz>")
      try {
        addExternalEntity("external-text", null, "external-text.ent", "test/external-text.xml")
        addExternalEntity("external-balanced-element", null, "external-balanced-element.ent", "test/external-balanced-element.xml")
        addExternalEntity("one", null, "ent/one.ent", "test/external-entity.xml")
        addExternalEntity("two", null, "ent/two.ent", "test/ent/one.xml")
      } catch {
        case ex: IOException ⇒ 
      }
    }
    if (fDeclaredEntities ne null) {
      for ((key, value) ← fDeclaredEntities)
        fEntities.put(key, value)
    }
    fEntityHandler = null
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
      if (suffixLength == Constants.ALLOW_JAVA_ENCODINGS_FEATURE.length && 
        featureId.endsWith(Constants.ALLOW_JAVA_ENCODINGS_FEATURE)) {
        fAllowJavaEncodings = state
      }
    }
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
      if (suffixLength == Constants.SYMBOL_TABLE_PROPERTY.length && 
        propertyId.endsWith(Constants.SYMBOL_TABLE_PROPERTY)) {
        fSymbolTable = value.asInstanceOf[SymbolTable]
        return
      }
      if (suffixLength == Constants.ERROR_REPORTER_PROPERTY.length && 
        propertyId.endsWith(Constants.ERROR_REPORTER_PROPERTY)) {
        fErrorReporter = value.asInstanceOf[XMLErrorReporter]
        return
      }
      if (suffixLength == Constants.ENTITY_RESOLVER_PROPERTY.length && 
        propertyId.endsWith(Constants.ENTITY_RESOLVER_PROPERTY)) {
        fEntityResolver = value.asInstanceOf[XMLEntityResolver]
        return
      }
      if (suffixLength == Constants.BUFFER_SIZE_PROPERTY.length && 
        propertyId.endsWith(Constants.BUFFER_SIZE_PROPERTY)) {
        val bufferSize = value.asInstanceOf[java.lang.Integer]
        if ((bufferSize ne null) && bufferSize.intValue() > DEFAULT_XMLDECL_BUFFER_SIZE) {
          fBufferSize = bufferSize.intValue()
          fEntityScanner.setBufferSize(fBufferSize)
          fSmallByteBufferPool.setBufferSize(fBufferSize)
          fLargeByteBufferPool.setBufferSize(fBufferSize << 1)
          fCharacterBufferPool.setExternalBufferSize(fBufferSize)
        }
      }
      if (suffixLength == Constants.SECURITY_MANAGER_PROPERTY.length && 
        propertyId.endsWith(Constants.SECURITY_MANAGER_PROPERTY)) {
        fSecurityManager = value.asInstanceOf[SecurityManager]
        fEntityExpansionLimit = if (fSecurityManager ne null) fSecurityManager.entityExpansionLimit else 0
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
   * Ends an entity.
   *
   * @throws XNIException Thrown by entity handler to signal an error.
   */
  def endEntity(): Unit = {
    if (DEBUG_BUFFER) {
      System.out.print("(endEntity: ")
      print(fCurrentEntity)
      println()
    }
    if (fEntityHandler ne null) {
      fEntityHandler.endEntity(fCurrentEntity.name, null)
    }
    try {
      fCurrentEntity.reader.close()
    } catch {
      case e: IOException ⇒ 
    }
    if (fReaderStack.nonEmpty) {
      fReaderStack.pop()
    }
    fCharacterBufferPool.returnBuffer(fCurrentEntity.fCharacterBuffer)
    if (fCurrentEntity.fByteBuffer ne null) {
      if (fCurrentEntity.fByteBuffer.length == fBufferSize) {
        fSmallByteBufferPool.returnBuffer(fCurrentEntity.fByteBuffer)
      } else {
        fLargeByteBufferPool.returnBuffer(fCurrentEntity.fByteBuffer)
      }
    }
    fCurrentEntity = if (fEntityStack.nonEmpty) fEntityStack.pop().asInstanceOf[ScannedEntity] else null
    fEntityScanner.setCurrentEntity(fCurrentEntity)
    if (DEBUG_BUFFER) {
      System.out.print(")endEntity: ")
      print(fCurrentEntity)
      println()
    }
  }

  /**
   * Returns the IANA encoding name that is auto-detected from
   * the bytes specified, with the endian-ness of that encoding where appropriate.
   *
   * @param b4    The first four bytes of the input.
   * @param count The number of bytes actually read.
   * @return an instance of EncodingInfo which represents the auto-detected encoding.
   */
  protected def getEncodingInfo(b4: Array[Byte], count: Int): EncodingInfo = {
    if (count < 2) {
      return EncodingInfo.UTF_8
    }
    val b0 = b4(0) & 0xFF
    val b1 = b4(1) & 0xFF
    if (b0 == 0xFE && b1 == 0xFF) {
      return EncodingInfo.UTF_16_BIG_ENDIAN_WITH_BOM
    }
    if (b0 == 0xFF && b1 == 0xFE) {
      return EncodingInfo.UTF_16_LITTLE_ENDIAN_WITH_BOM
    }
    if (count < 3) {
      return EncodingInfo.UTF_8
    }
    val b2 = b4(2) & 0xFF
    if (b0 == 0xEF && b1 == 0xBB && b2 == 0xBF) {
      return EncodingInfo.UTF_8_WITH_BOM
    }
    if (count < 4) {
      return EncodingInfo.UTF_8
    }
    val b3 = b4(3) & 0xFF
    if (b0 == 0x00 && b1 == 0x00 && b2 == 0x00 && b3 == 0x3C) {
      return EncodingInfo.UCS_4_BIG_ENDIAN
    }
    if (b0 == 0x3C && b1 == 0x00 && b2 == 0x00 && b3 == 0x00) {
      return EncodingInfo.UCS_4_LITTLE_ENDIAN
    }
    if (b0 == 0x00 && b1 == 0x00 && b2 == 0x3C && b3 == 0x00) {
      return EncodingInfo.UCS_4_UNUSUAL_BYTE_ORDER
    }
    if (b0 == 0x00 && b1 == 0x3C && b2 == 0x00 && b3 == 0x00) {
      return EncodingInfo.UCS_4_UNUSUAL_BYTE_ORDER
    }
    if (b0 == 0x00 && b1 == 0x3C && b2 == 0x00 && b3 == 0x3F) {
      return EncodingInfo.UTF_16_BIG_ENDIAN
    }
    if (b0 == 0x3C && b1 == 0x00 && b2 == 0x3F && b3 == 0x00) {
      return EncodingInfo.UTF_16_LITTLE_ENDIAN
    }
    if (b0 == 0x4C && b1 == 0x6F && b2 == 0xA7 && b3 == 0x94) {
      return EncodingInfo.EBCDIC
    }
    EncodingInfo.UTF_8
  }

  /**
   * Creates a reader capable of reading the given input stream in
   * the specified encoding.
   *
   * @param inputStream  The input stream.
   * @param encoding     The encoding name that the input stream is
   *                     encoded using. If the user has specified that
   *                     Java encoding names are allowed, then the
   *                     encoding name may be a Java encoding name;
   *                     otherwise, it is an ianaEncoding name.
   * @param isBigEndian   For encodings (like uCS-4), whose names cannot
   *                      specify a byte order, this tells whether the order is bigEndian. Null means
   *                      unknown or not relevant.
   *
   * @return Returns a reader.
   */
  protected def createReader(inputStream: InputStream, encoding: String, isBigEndian: java.lang.Boolean): Reader = {
    if (encoding == "UTF-8" || (encoding eq null)) {
      return createUTF8Reader(inputStream)
    }
    if (encoding == "UTF-16" && (isBigEndian ne null)) {
      return createUTF16Reader(inputStream, isBigEndian.booleanValue())
    }
    val ENCODING = encoding.toUpperCase//Locale.ENGLISH
    if (ENCODING == "UTF-8") {
      return createUTF8Reader(inputStream)
    }
    if (ENCODING == "UTF-16BE") {
      return createUTF16Reader(inputStream, isBigEndian = true)
    }
    if (ENCODING == "UTF-16LE") {
      return createUTF16Reader(inputStream, isBigEndian = false)
    }
    if (ENCODING == "ISO-10646-UCS-4") {
      if (isBigEndian ne null) {
        val isBE = isBigEndian.booleanValue()
        if (isBE) {
          return new UCSReader(inputStream, UCSReader.UCS4BE)
        } else {
          return new UCSReader(inputStream, UCSReader.UCS4LE)
        }
      } else {
        fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "EncodingByteOrderUnsupported", Array(encoding), 
          XMLErrorReporter.SEVERITY_FATAL_ERROR)
      }
    }
    if (ENCODING == "ISO-10646-UCS-2") {
      if (isBigEndian ne null) {
        val isBE = isBigEndian.booleanValue()
        if (isBE) {
          return new UCSReader(inputStream, UCSReader.UCS2BE)
        } else {
          return new UCSReader(inputStream, UCSReader.UCS2LE)
        }
      } else {
        fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "EncodingByteOrderUnsupported", Array(encoding), 
          XMLErrorReporter.SEVERITY_FATAL_ERROR)
      }
    }
    val validIANA = XMLChar.isValidIANAEncoding(encoding)
    val validJava = XMLChar.isValidJavaEncoding(encoding)
    if (!validIANA || (fAllowJavaEncodings && !validJava)) {
      fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "EncodingDeclInvalid", Array(encoding), 
        XMLErrorReporter.SEVERITY_FATAL_ERROR)
      return createLatin1Reader(inputStream)
    }
    var javaEncoding = EncodingMap.getIANA2JavaMapping(ENCODING)
    if (javaEncoding eq null) {
      if (fAllowJavaEncodings) {
        javaEncoding = encoding
      } else {
        fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN, "EncodingDeclInvalid", Array(encoding), 
          XMLErrorReporter.SEVERITY_FATAL_ERROR)
        return createLatin1Reader(inputStream)
      }
    } else if (javaEncoding == "ASCII") {
      return createASCIIReader(inputStream)
    } else if (javaEncoding == "ISO8859_1") {
      return createLatin1Reader(inputStream)
    }
    if (DEBUG_ENCODINGS) {
      System.out.print("$$$ creating Java InputStreamReader: encoding=" + javaEncoding)
      if (javaEncoding == encoding) {
        System.out.print(" (IANA encoding)")
      }
      println()
    }
    new InputStreamReader(inputStream, javaEncoding)
  }

  /**
   Create a new UTF-8 reader from the InputStream. *
   */
  private def createUTF8Reader(stream: InputStream): Reader = {
    if (DEBUG_ENCODINGS) {
      println("$$$ creating UTF8Reader")
    }
    if (fTempByteBuffer eq null) {
      fTempByteBuffer = fSmallByteBufferPool.getBuffer
    }
    new UTF8Reader(stream, fTempByteBuffer, fErrorReporter.getMessageFormatter(XMLMessageFormatter.XML_DOMAIN))
  }

  /**
   Create a new UTF-16 reader from the InputStream. *
   */
  private def createUTF16Reader(stream: InputStream, isBigEndian: Boolean): Reader = {
    if (DEBUG_ENCODINGS) {
      println("$$$ creating UTF16Reader")
    }
    if (fTempByteBuffer eq null) {
      fTempByteBuffer = fLargeByteBufferPool.getBuffer
    } else if (fTempByteBuffer.length == fBufferSize) {
      fSmallByteBufferPool.returnBuffer(fTempByteBuffer)
      fTempByteBuffer = fLargeByteBufferPool.getBuffer
    }
    new UTF16Reader(stream, fTempByteBuffer, isBigEndian, fErrorReporter.getMessageFormatter(XMLMessageFormatter.XML_DOMAIN))
  }

  /**
   Create a new ASCII reader from the InputStream. *
   */
  private def createASCIIReader(stream: InputStream): Reader = {
    if (DEBUG_ENCODINGS) {
      println("$$$ creating ASCIIReader")
    }
    if (fTempByteBuffer eq null) {
      fTempByteBuffer = fSmallByteBufferPool.getBuffer
    }
    new ASCIIReader(stream, fTempByteBuffer, fErrorReporter.getMessageFormatter(XMLMessageFormatter.XML_DOMAIN))
  }

  /**
   Create a new ISO-8859-1 reader from the InputStream. *
   */
  private def createLatin1Reader(stream: InputStream): Reader = {
    if (DEBUG_ENCODINGS) {
      println("$$$ creating Latin1Reader")
    }
    if (fTempByteBuffer eq null) {
      fTempByteBuffer = fSmallByteBufferPool.getBuffer
    }
    new Latin1Reader(stream, fTempByteBuffer)
  }

  /**
   * Returns the hashtable of declared entities.
   * 
   * *REVISIT:*
   * This should be done the "right" way by designing a better way to
   * enumerate the declared entities. For now, this method is needed
   * by the constructor that takes an XMLEntityManager parameter.
   */
  def getDeclaredEntities: mutable.HashMap[String, Entity] = fEntities

  /**
   * Entity state.
   */
  class ScannedEntity(
    name               : String, 
    var entityLocation : XMLResourceIdentifier, 
    var stream         : InputStream, 
    var reader         : Reader, 
    byteBuffer         : Array[Byte], 
    var encoding       : String, 
    var literal        : Boolean, 
    var mayReadChunks  : Boolean, 
    var isExternal     : Boolean
  ) extends Entity(name, XMLEntityManager.this.fInExternalSubset) {

    var lineNumber: Int = 1
    var columnNumber: Int = 1

    /**
     * Encoding has been set externally, for example
     * using a SAX InputSource or a DOM LSInput.
     */
    var externallySpecifiedEncoding: Boolean = false
    
    var xmlVersion: String = "1.0"

    /**
     Position in character buffer.
     */
    var position: Int = _

    /**
     Base character offset for computing absolute character offset.
     */
    var baseCharOffset: Int = _

    /**
     Start position in character buffer.
     */
    var startPosition: Int = _

    /**
     Count of characters in buffer.
     */
    var count: Int = _

    /**
     Character buffer container.
     */
    private[impl] val fCharacterBuffer = fCharacterBufferPool.getBuffer(isExternal)

    /**
     Byte buffer.
     */
    private[impl] var fByteBuffer: Array[Byte] = byteBuffer
    
    /**
     Character buffer.
     */
    var ch: Array[Char] = fCharacterBuffer.ch

    /**
     Returns true if this is an external entity.
     */
//    def isExternal(): Boolean = isExternal

    /**
     Returns true if this is an unparsed entity.
     */
    def isUnparsed: Boolean = false

    def setReader(stream: InputStream, encoding: String, isBigEndian: java.lang.Boolean): Unit = {
      fTempByteBuffer = fByteBuffer
      reader = createReader(stream, encoding, isBigEndian)
      fByteBuffer = fTempByteBuffer
    }

    def getExpandedSystemId: String = {
      val size = fEntityStack.size
      var i = size - 1
      while (i >= 0) {
        val externalEntity = fEntityStack(i).asInstanceOf[ScannedEntity]
        if ((externalEntity.entityLocation ne null) && 
          (externalEntity.entityLocation.getExpandedSystemId ne null)) {
          return externalEntity.entityLocation.getExpandedSystemId
        }
        i -= 1
      }
      null
    }

    def getLiteralSystemId: String = {
      val size = fEntityStack.size
      var i = size - 1
      while (i >= 0) {
        val externalEntity = fEntityStack(i).asInstanceOf[ScannedEntity]
        if ((externalEntity.entityLocation ne null) && 
          (externalEntity.entityLocation.getLiteralSystemId ne null)) {
          return externalEntity.entityLocation.getLiteralSystemId
        }
        i -= 1
      }
      null
    }

    def getLineNumber: Int = {
      val size = fEntityStack.size
      var i = size - 1
      while (i >= 0) {
        val firstExternalEntity = fEntityStack(i).asInstanceOf[ScannedEntity]
        if (firstExternalEntity.isExternal) {
          return firstExternalEntity.lineNumber
        }
        i -= 1
      }
      -1
    }

    def getColumnNumber: Int = {
      val size = fEntityStack.size
      var i = size - 1
      while (i >= 0) {
        val firstExternalEntity = fEntityStack(i).asInstanceOf[ScannedEntity]
        if (firstExternalEntity.isExternal) {
          return firstExternalEntity.columnNumber
        }
        i -= 1
      }
      -1
    }

    def getCharacterOffset: Int = {
      val size = fEntityStack.size
      var i = size - 1
      while (i >= 0) {
        val firstExternalEntity = fEntityStack(i).asInstanceOf[ScannedEntity]
        if (firstExternalEntity.isExternal) {
          return firstExternalEntity.baseCharOffset + 
            (firstExternalEntity.position - firstExternalEntity.startPosition)
        }
        i -= 1
      }
      -1
    }

    def getEncoding: String = {
      val size = fEntityStack.size
      var i = size - 1
      while (i >= 0) {
        val firstExternalEntity = fEntityStack(i).asInstanceOf[ScannedEntity]
        if (firstExternalEntity.isExternal) {
          return firstExternalEntity.encoding
        }
        i -= 1
      }
      null
    }

    def getXMLVersion: String = {
      val size = fEntityStack.size
      var i = size - 1
      while (i >= 0) {
        val firstExternalEntity = fEntityStack(i).asInstanceOf[ScannedEntity]
        if (firstExternalEntity.isExternal) {
          return firstExternalEntity.xmlVersion
        }
        i -= 1
      }
      null
    }

    /**
     Returns whether the encoding of this entity was externally specified. *
     */
    def isEncodingExternallySpecified: Boolean = externallySpecifiedEncoding

    /**
     Sets whether the encoding of this entity was externally specified. *
     */
    def setEncodingExternallySpecified(value: Boolean): Unit = {
      externallySpecifiedEncoding = value
    }

    /**
     Returns a string representation of this object.
     */
    override def toString: String = {
      val str = new StringBuffer()
      str.append("name=\"").append(name).append('"')
      str.append(",ch=")
      str.append(ch)
      str.append(",position=").append(position)
      str.append(",count=").append(count)
      str.append(",baseCharOffset=").append(baseCharOffset)
      str.append(",startPosition=").append(startPosition)
      str.toString
    }
  }

  /**
   * This class wraps the byte inputstreams we're presented with.
   * We need it because java.io.InputStreams don't provide
   * functionality to reread processed bytes, and they have a habit
   * of reading more than one character when you call their read()
   * methods.  This means that, once we discover the true (declared)
   * encoding of a document, we can neither backtrack to read the
   * whole doc again nor start reading where we are with a new
   * reader.
   *
   * This class allows rewinding an inputStream by allowing a mark
   * to be set, and the stream reset to that position.  *The
   * class assumes that it needs to read one character per
   * invocation when it's read() method is invked, but uses the
   * underlying InputStream's read(char[], offset length) method--it
   * won't buffer data read this way!*
   */
  protected class RewindableInputStream(var fInputStream: InputStream) extends InputStream {

    private var fData: Array[Byte] = new Array[Byte](DEFAULT_XMLDECL_BUFFER_SIZE)
    private var fStartOffset: Int = 0
    private var fEndOffset: Int = -1
    private var fOffset: Int = 0
    private var fLength: Int = 0
    private var fMark: Int = 0

    def setStartOffset(offset: Int): Unit = {
      fStartOffset = offset
    }

    def rewind(): Unit = {
      fOffset = fStartOffset
    }

    def readAndBuffer(): Int = {
      if (fOffset == fData.length) {
        val newData = new Array[Byte](fOffset << 1)
        System.arraycopy(fData, 0, newData, 0, fOffset)
        fData = newData
      }
      val b = fInputStream.read()
      if (b == -1) {
        fEndOffset = fOffset
        return -1
      }
      fData(fLength) = b.toByte
      fLength += 1
      fOffset += 1
      b & 0xff
    }

    def read(): Int = {
      if (fOffset < fLength) {
        val result = fData(fOffset) & 0xff
        fOffset += 1
        return result
      }
      if (fOffset == fEndOffset) {
        return -1
      }
      if (fCurrentEntity.mayReadChunks) {
        return fInputStream.read()
      }
      readAndBuffer()
    }

    override def read(b: Array[Byte], off: Int, _len: Int): Int = {
      var len = _len
      val bytesLeft = fLength - fOffset
      if (bytesLeft == 0) {
        if (fOffset == fEndOffset) {
          return -1
        }
        if (fCurrentEntity.mayReadChunks) {
          return fInputStream.read(b, off, len)
        }
        val returnedVal = readAndBuffer()
        if (returnedVal == -1) {
          fEndOffset = fOffset
          return -1
        }
        b(off) = returnedVal.toByte
        return 1
      }
      if (len < bytesLeft) {
        if (len <= 0) {
          return 0
        }
      } else {
        len = bytesLeft
      }
      if (b ne null) {
        System.arraycopy(fData, fOffset, b, off, len)
      }
      fOffset += len
      len
    }

    override def skip(_n: Long): Long = {
      var n = _n
      var bytesLeft: Int = 0
      if (n <= 0) {
        return 0
      }
      bytesLeft = fLength - fOffset
      if (bytesLeft == 0) {
        if (fOffset == fEndOffset) {
          return 0
        }
        return fInputStream.skip(n)
      }
      if (n <= bytesLeft) {
        fOffset += n.toInt // @ebruchez: Long ⇒ Int can lose precision! Was like this in Java code.
        return n
      }
      fOffset += bytesLeft
      if (fOffset == fEndOffset) {
        return bytesLeft
      }
      n -= bytesLeft
      fInputStream.skip(n) + bytesLeft
    }

    override def available(): Int = {
      val bytesLeft = fLength - fOffset
      if (bytesLeft == 0) {
        if (fOffset == fEndOffset) {
          return -1
        }
        return if (fCurrentEntity.mayReadChunks) fInputStream.available() else 0
      }
      bytesLeft
    }

    override def mark(howMuch: Int): Unit = {
      fMark = fOffset
    }

    override def reset(): Unit = {
      fOffset = fMark
    }

    override def markSupported(): Boolean = true

    override def close(): Unit = {
      if (fInputStream ne null) {
        fInputStream.close()
        fInputStream = null
      }
    }
  }
}
