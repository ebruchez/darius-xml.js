package org.orbeon.darius.util

import scala.beans.BeanProperty

object SecurityManager {

  /**
   Default value for entity expansion limit. *
   */
  private val DEFAULT_ENTITY_EXPANSION_LIMIT = 100000

  /**
   Default value of number of nodes created. *
   */
  private val DEFAULT_MAX_OCCUR_NODE_LIMIT = 3000
}

/**
 * This class is a container for parser settings that relate to
 * security, or more specifically, it is intended to be used to prevent denial-of-service
 * attacks from being launched against a system running Xerces.
 * Any component that is aware of a denial-of-service attack that can arise
 * from its processing of a certain kind of document may query its Component Manager
 * for the property (http://apache.org/xml/properties/security-manager)
 * whose value will be an instance of this class.
 * If no value has been set for the property, the component should proceed in the "usual" (spec-compliant)
 * manner.  If a value has been set, then it must be the case that the component in
 * question needs to know what method of this class to query.  This class
 * will provide defaults for all known security issues, but will also provide
 * setters so that those values can be tailored by applications that care.
 */
class SecurityManager {

  /**
   * Entity expansion limit.
   */
  var entityExpansionLimit: Int = _

  /**
   * W3C XML Schema maxOccurs limit.
   */
  private var maxOccurLimit: Int = _

  /**
   * Sets the limit of the number of content model nodes
   * that may be created when building a grammar for a W3C
   * XML Schema that contains maxOccurs attributes with values
   * other than "unbounded".
   *
   * @param limit the maximum value for maxOccurs other
   * than "unbounded"
   */
  def setMaxOccurNodeLimit(limit: Int): Unit = {
    maxOccurLimit = limit
  }

  /**
   * Returns the limit of the number of content model nodes
   * that may be created when building a grammar for a W3C
   * XML Schema that contains maxOccurs attributes with values
   * other than "unbounded".
   *
   * @return the maximum value for maxOccurs other
   * than "unbounded"
   */
  def getMaxOccurNodeLimit: Int = maxOccurLimit
}
