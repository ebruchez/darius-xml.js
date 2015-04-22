package org.apache.xerces.util

import org.apache.xerces.util.XMLAttributesImpl._
import org.apache.xerces.xni.Augmentations
import org.apache.xerces.xni.QName
import org.apache.xerces.xni.XMLAttributes

import scala.util.control.Breaks

object XMLAttributesImpl {

  /**
   Default table size.
   */
  protected val TABLE_SIZE = 101

  /**
   * Threshold at which an instance is treated
   * as a large attribute list.
   */
  protected val SIZE_LIMIT = 20

  /**
   * Attribute information.
   */
  class Attribute {

    val name = new QName()
    var `type`: String = _
    var value: String = _
    var nonNormalizedValue: String = _
    var specified: Boolean = _

    /**
     * Augmentations information for this attribute.
     * XMLAttributes has no knowledge if any augmentations
     * were attached to Augmentations.
     */
    var augs: Augmentations = new AugmentationsImpl

    /**
     Pointer to the next attribute in the chain. *
     */
    var next: Attribute = _
  }
}

/**
 * The XMLAttributesImpl class is an implementation of the XMLAttributes
 * interface which defines a collection of attributes for an element.
 * In the parser, the document source would scan the entire start element
 * and collect the attributes. The attributes are communicated to the
 * document handler in the startElement method.
 * 
 * The attributes are read-write so that subsequent stages in the document
 * pipeline can modify the values or change the attributes that are
 * propogated to the next stage.
 *
 * @see org.apache.xerces.xni.XMLDocumentHandler#startElement
 */
class XMLAttributesImpl(protected var fTableViewBuckets: Int) extends XMLAttributes {

  /**
   Namespaces.
   */
  protected var fNamespaces: Boolean = true

  /**
   * Usage count for the attribute table view.
   * Incremented each time all attributes are removed
   * when the attribute table view is in use.
   */
  protected var fLargeCount: Int = 1

  /**
   Attribute count.
   */
  protected var fLength: Int = _

  /**
   Attribute information.
   */
  protected var fAttributes: Array[Attribute] = new Array[Attribute](4)

  /**
   * Hashtable of attribute information.
   * Provides an alternate view of the attribute specification.
   */
  protected var fAttributeTableView: Array[Attribute] = _

  /**
   * Tracks whether each chain in the hash table is stale
   * with respect to the current state of this object.
   * A chain is stale if its state is not the same as the number
   * of times the attribute table view has been used.
   */
  protected var fAttributeTableViewChainState: Array[Int] = _

  /**
   * Indicates whether the table view contains consistent data.
   */
  protected var fIsTableViewConsistent: Boolean = _

  for (i ← fAttributes.indices) {
    fAttributes(i) = new Attribute()
  }

  /**
   Default constructor.
   */
  def this() {
    this(TABLE_SIZE)
  }

  /**
   * Sets whether namespace processing is being performed. This state
   * is needed to return the correct value from the getLocalName method.
   *
   * @param namespaces True if namespace processing is turned on.
   *
   * @see #getLocalName
   */
  def setNamespaces(namespaces: Boolean): Unit = {
    fNamespaces = namespaces
  }

  /**
   * Adds an attribute. The attribute's non-normalized value of the
   * attribute will have the same value as the attribute value until
   * set using the `setNonNormalizedValue` method. Also,
   * the added attribute will be marked as specified in the XML instance
   * document unless set otherwise using the `setSpecified`
   * method.
   * 
   * *Note:* If an attribute of the same name already
   * exists, the old values for the attribute are replaced by the new
   * values.
   *
   * @param name  The attribute name.
   * @param type  The attribute type. The type name is determined by
   *                  the type specified for this attribute in the DTD.
   *                  For example: "CDATA", "ID", "NMTOKEN", etc. However,
   *                  attributes of type enumeration will have the type
   *                  value specified as the pipe ('|') separated list of
   *                  the enumeration values prefixed by an open
   *                  parenthesis and suffixed by a close parenthesis.
   *                  For example: "(true|false)".
   * @param value The attribute value.
   *
   * @return Returns the attribute index.
   *
   * @see #setNonNormalizedValue
   * @see #setSpecified
   */
  def addAttribute(name: QName, `type`: String, value: String): Int = {
    var index: Int = 0
    if (fLength < SIZE_LIMIT) {
      index =
        if ((name.uri ne null) && name.uri.length != 0)
          getIndexFast(name.uri, name.localpart)
        else
          getIndexFast(name.rawname)
      if (index == -1) {
        index = fLength
        if (fLength == fAttributes.length) {
          val attributes = new Array[Attribute](fAttributes.length + 4)
          System.arraycopy(fAttributes, 0, attributes, 0, fAttributes.length)
          for (i ← fAttributes.length until attributes.length) {
            attributes(i) = new Attribute()
          }
          fAttributes = attributes
        }
        fLength += 1
      }
    } else if ((name.uri eq null) || name.uri.length == 0 || 
      { index = getIndexFast(name.uri, name.localpart); index } == -1) {
      if (!fIsTableViewConsistent || fLength == SIZE_LIMIT) {
        prepareAndPopulateTableView()
        fIsTableViewConsistent = true
      }
      val bucket = getTableViewBucket(name.rawname)
      if (fAttributeTableViewChainState(bucket) != fLargeCount) {
        index = fLength
        if (fLength == fAttributes.length) {
          val attributes = new Array[Attribute](fAttributes.length << 1)
          System.arraycopy(fAttributes, 0, attributes, 0, fAttributes.length)
          for (i ← fAttributes.length until attributes.length) {
            attributes(i) = new Attribute()
          }
          fAttributes = attributes
        }
        fLength += 1
        fAttributeTableViewChainState(bucket) = fLargeCount
        fAttributes(index).next = null
        fAttributeTableView(bucket) = fAttributes(index)
      } else {
        var found = fAttributeTableView(bucket)
        val whileBreaks = new Breaks
        whileBreaks.breakable {
          while (found ne null) {
            if (found.name.rawname == name.rawname) {
              whileBreaks.break()
            }
            found = found.next
          }
        }
        if (found eq null) {
          index = fLength
          if (fLength == fAttributes.length) {
            val attributes = new Array[Attribute](fAttributes.length << 1)
            System.arraycopy(fAttributes, 0, attributes, 0, fAttributes.length)
            for (i ← fAttributes.length until attributes.length) {
              attributes(i) = new Attribute()
            }
            fAttributes = attributes
          }
          fLength += 1
          fAttributes(index).next = fAttributeTableView(bucket)
          fAttributeTableView(bucket) = fAttributes(index)
        } else {
          index = getIndexFast(name.rawname)
        }
      }
    }
    val attribute = fAttributes(index)
    attribute.name.setValues(name)
    attribute.`type` = `type`
    attribute.value = value
    attribute.nonNormalizedValue = value
    attribute.specified = false
    attribute.augs.removeAllItems()
    index
  }

  /**
   * Removes all of the attributes. This method will also remove all
   * entities associated to the attributes.
   */
  def removeAllAttributes(): Unit = {
    fLength = 0
  }

  /**
   * Removes the attribute at the specified index.
   * 
   * *Note:* This operation changes the indexes of all
   * attributes following the attribute at the specified index.
   *
   * @param attrIndex The attribute index.
   */
  def removeAttributeAt(attrIndex: Int): Unit = {
    fIsTableViewConsistent = false
    if (attrIndex < fLength - 1) {
      val removedAttr = fAttributes(attrIndex)
      System.arraycopy(fAttributes, attrIndex + 1, fAttributes, attrIndex, fLength - attrIndex - 1)
      fAttributes(fLength - 1) = removedAttr
    }
    fLength -= 1
  }

  /**
   * Sets the name of the attribute at the specified index.
   *
   * @param attrIndex The attribute index.
   * @param attrName  The new attribute name.
   */
  def setName(attrIndex: Int, attrName: QName): Unit = {
    fAttributes(attrIndex).name.setValues(attrName)
  }

  /**
   * Sets the fields in the given QName structure with the values
   * of the attribute name at the specified index.
   *
   * @param attrIndex The attribute index.
   * @param attrName  The attribute name structure to fill in.
   */
  def getName(attrIndex: Int, attrName: QName): Unit = {
    attrName.setValues(fAttributes(attrIndex).name)
  }

  /**
   * Sets the type of the attribute at the specified index.
   *
   * @param attrIndex The attribute index.
   * @param attrType  The attribute type. The type name is determined by
   *                  the type specified for this attribute in the DTD.
   *                  For example: "CDATA", "ID", "NMTOKEN", etc. However,
   *                  attributes of type enumeration will have the type
   *                  value specified as the pipe ('|') separated list of
   *                  the enumeration values prefixed by an open
   *                  parenthesis and suffixed by a close parenthesis.
   *                  For example: "(true|false)".
   */
  def setType(attrIndex: Int, attrType: String): Unit = {
    fAttributes(attrIndex).`type` = attrType
  }

  /**
   * Sets the value of the attribute at the specified index. This
   * method will overwrite the non-normalized value of the attribute.
   *
   * @param attrIndex The attribute index.
   * @param attrValue The new attribute value.
   *
   * @see #setNonNormalizedValue
   */
  def setValue(attrIndex: Int, attrValue: String): Unit = {
    val attribute = fAttributes(attrIndex)
    attribute.value = attrValue
    attribute.nonNormalizedValue = attrValue
  }

  /**
   * Sets the non-normalized value of the attribute at the specified
   * index.
   *
   * @param attrIndex The attribute index.
   * @param _attrValue The new non-normalized attribute value.
   */
  def setNonNormalizedValue(attrIndex: Int, _attrValue: String): Unit = {
    var attrValue = _attrValue
    if (attrValue eq null) {
      attrValue = fAttributes(attrIndex).value
    }
    fAttributes(attrIndex).nonNormalizedValue = attrValue
  }

  /**
   * Returns the non-normalized value of the attribute at the specified
   * index. If no non-normalized value is set, this method will return
   * the same value as the `getValue(int)` method.
   *
   * @param attrIndex The attribute index.
   */
  def getNonNormalizedValue(attrIndex: Int): String = {
    val value = fAttributes(attrIndex).nonNormalizedValue
    value
  }

  /**
   * Sets whether an attribute is specified in the instance document
   * or not.
   *
   * @param attrIndex The attribute index.
   * @param specified True if the attribute is specified in the instance
   *                  document.
   */
  def setSpecified(attrIndex: Int, specified: Boolean): Unit = {
    fAttributes(attrIndex).specified = specified
  }

  /**
   * Returns true if the attribute is specified in the instance document.
   *
   * @param attrIndex The attribute index.
   */
  def isSpecified(attrIndex: Int): Boolean = fAttributes(attrIndex).specified

  /**
   * Return the number of attributes in the list.
   *
   * Once you know the number of attributes, you can iterate
   * through the list.
   *
   * @return The number of attributes in the list.
   */
  def getLength: Int = fLength

  /**
   * Look up an attribute's type by index.
   *
   * The attribute type is one of the strings "CDATA", "ID",
   * "IDREF", "IDREFS", "NMTOKEN", "NMTOKENS", "ENTITY", "ENTITIES",
   * or "NOTATION" (always in upper case).
   *
   * If the parser has not read a declaration for the attribute,
   * or if the parser does not report attribute types, then it must
   * return the value "CDATA" as stated in the XML 1.0 Recommentation
   * (clause 3.3.3, "Attribute-Value Normalization").
   *
   * For an enumerated attribute that is not a notation, the
   * parser will report the type as "NMTOKEN".
   *
   * @param index The attribute index (zero-based).
   * @return The attribute's type as a string, or null if the
   *         index is out of range.
   * @see #getLength
   */
  def getType(index: Int): String = {
    if (index < 0 || index >= fLength) {
      return null
    }
    getReportableType(fAttributes(index).`type`)
  }

  /**
   * Look up an attribute's type by XML 1.0 qualified name.
   *
   * See {@link #getType(int) getType(int)} for a description
   * of the possible types.
   *
   * @param qname The XML 1.0 qualified name.
   * @return The attribute type as a string, or null if the
   *         attribute is not in the list or if qualified names
   *         are not available.
   */
  def getType(qname: String): String = {
    val index = getIndex(qname)
    if (index != -1) getReportableType(fAttributes(index).`type`) else null
  }

  /**
   * Look up an attribute's value by index.
   *
   * If the attribute value is a list of tokens (IDREFS,
   * ENTITIES, or NMTOKENS), the tokens will be concatenated
   * into a single string with each token separated by a
   * single space.
   *
   * @param index The attribute index (zero-based).
   * @return The attribute's value as a string, or null if the
   *         index is out of range.
   * @see #getLength
   */
  def getValue(index: Int): String = {
    if (index < 0 || index >= fLength) {
      return null
    }
    fAttributes(index).value
  }

  /**
   * Look up an attribute's value by XML 1.0 qualified name.
   *
   * See {@link #getValue(int) getValue(int)} for a description
   * of the possible values.
   *
   * @param qname The XML 1.0 qualified name.
   * @return The attribute value as a string, or null if the
   *         attribute is not in the list or if qualified names
   *         are not available.
   */
  def getValue(qname: String): String = {
    val index = getIndex(qname)
    if (index != -1) fAttributes(index).value else null
  }

  /**
   * Return the name of an attribute in this list (by position).
   *
   * The names must be unique: the SAX parser shall not include the
   * same attribute twice.  Attributes without values (those declared
   * #IMPLIED without a value specified in the start tag) will be
   * omitted from the list.
   *
   * If the attribute name has a namespace prefix, the prefix
   * will still be attached.
   *
   * @param index The index of the attribute in the list (starting at 0).
   * @return The name of the indexed attribute, or null
   *         if the index is out of range.
   * @see #getLength
   */
  def getName(index: Int): String = {
    if (index < 0 || index >= fLength) {
      return null
    }
    fAttributes(index).name.rawname
  }

  /**
   * Look up the index of an attribute by XML 1.0 qualified name.
   *
   * @param qName The qualified (prefixed) name.
   * @return The index of the attribute, or -1 if it does not
   *         appear in the list.
   */
  def getIndex(qName: String): Int = {
    for (i ← 0 until fLength) {
      val attribute = fAttributes(i)
      if ((attribute.name.rawname ne null) && attribute.name.rawname == qName) {
        return i
      }
    }
    -1
  }

  /**
   * Look up the index of an attribute by Namespace name.
   *
   * @param uri The Namespace URI, or null if
   *        the name has no Namespace URI.
   * @param localPart The attribute's local name.
   * @return The index of the attribute, or -1 if it does not
   *         appear in the list.
   */
  def getIndex(uri: String, localPart: String): Int = {
    for (i ← 0 until fLength) {
      val attribute = fAttributes(i)
      if ((attribute.name.localpart ne null) && attribute.name.localpart == localPart && 
        ((uri == attribute.name.uri) || 
        ((uri ne null) && (attribute.name.uri ne null) && attribute.name.uri == uri))) {
        return i
      }
    }
    -1
  }

  /**
   * Look up an attribute's local name by index.
   *
   * @param index The attribute index (zero-based).
   * @return The local name, or the empty string if Namespace
   *         processing is not being performed, or null
   *         if the index is out of range.
   * @see #getLength
   */
  def getLocalName(index: Int): String = {
    if (!fNamespaces) {
      return ""
    }
    if (index < 0 || index >= fLength) {
      return null
    }
    fAttributes(index).name.localpart
  }

  /**
   * Look up an attribute's XML 1.0 qualified name by index.
   *
   * @param index The attribute index (zero-based).
   * @return The XML 1.0 qualified name, or the empty string
   *         if none is available, or null if the index
   *         is out of range.
   * @see #getLength
   */
  def getQName(index: Int): String = {
    if (index < 0 || index >= fLength) {
      return null
    }
    val rawname = fAttributes(index).name.rawname
    if (rawname ne null) rawname else ""
  }

  /**
   * Look up an attribute's type by Namespace name.
   *
   * See {@link #getType(int) getType(int)} for a description
   * of the possible types.
   *
   * @param uri The Namespace URI, or null if the
   *        name has no Namespace URI.
   * @param localName The local name of the attribute.
   * @return The attribute type as a string, or null if the
   *         attribute is not in the list or if Namespace
   *         processing is not being performed.
   */
  def getType(uri: String, localName: String): String = {
    if (!fNamespaces) {
      return null
    }
    val index = getIndex(uri, localName)
    if (index != -1) getReportableType(fAttributes(index).`type`) else null
  }

  /**
   * Returns the prefix of the attribute at the specified index.
   *
   * @param index The index of the attribute.
   */
  def getPrefix(index: Int): String = {
    if (index < 0 || index >= fLength) {
      return null
    }
    val prefix = fAttributes(index).name.prefix
    if (prefix ne null) prefix else ""
  }

  /**
   * Look up an attribute's Namespace URI by index.
   *
   * @param index The attribute index (zero-based).
   * @return The Namespace URI
   * @see #getLength
   */
  def getURI(index: Int): String = {
    if (index < 0 || index >= fLength) {
      return null
    }
    val uri = fAttributes(index).name.uri
    uri
  }

  /**
   * Look up an attribute's value by Namespace name.
   *
   * See {@link #getValue(int) getValue(int)} for a description
   * of the possible values.
   *
   * @param uri The Namespace URI, or null if the
   * @param localName The local name of the attribute.
   * @return The attribute value as a string, or null if the
   *         attribute is not in the list.
   */
  def getValue(uri: String, localName: String): String = {
    val index = getIndex(uri, localName)
    if (index != -1) getValue(index) else null
  }

  /**
   * Look up an augmentations by Namespace name.
   *
   * @param uri The Namespace URI, or null if the
   * @param localName The local name of the attribute.
   * @return Augmentations
   */
  def getAugmentations(uri: String, localName: String): Augmentations = {
    val index = getIndex(uri, localName)
    if (index != -1) fAttributes(index).augs else null
  }

  /**
   * Look up an augmentation by XML 1.0 qualified name.
   * 
   *
   * @param qName The XML 1.0 qualified name.
   *
   * @return Augmentations
   *
   */
  def getAugmentations(qName: String): Augmentations = {
    val index = getIndex(qName)
    if (index != -1) fAttributes(index).augs else null
  }

  /**
   * Look up an augmentations by attributes index.
   *
   * @param attributeIndex The attribute index.
   * @return Augmentations
   */
  def getAugmentations(attributeIndex: Int): Augmentations = {
    if (attributeIndex < 0 || attributeIndex >= fLength) {
      return null
    }
    fAttributes(attributeIndex).augs
  }

  /**
   * Sets the augmentations of the attribute at the specified index.
   *
   * @param attrIndex The attribute index.
   * @param augs      The augmentations.
   */
  def setAugmentations(attrIndex: Int, augs: Augmentations): Unit = {
    fAttributes(attrIndex).augs = augs
  }

  /**
   * Sets the uri of the attribute at the specified index.
   *
   * @param attrIndex The attribute index.
   * @param uri       Namespace uri
   */
  def setURI(attrIndex: Int, uri: String): Unit = {
    fAttributes(attrIndex).name.uri = uri
  }

  /**
   * Look up the index of an attribute by XML 1.0 qualified name.
   * 
   * *Note:*
   * This method uses reference comparison, and thus should
   * only be used internally. We cannot use this method in any
   * code exposed to users as they may not pass in unique strings.
   *
   * @param qName The qualified (prefixed) name.
   * @return The index of the attribute, or -1 if it does not
   *         appear in the list.
   */
  def getIndexFast(qName: String): Int = {
    for (i ← 0 until fLength) {
      val attribute = fAttributes(i)
      if (attribute.name.rawname == qName) {
        return i
      }
    }
    -1
  }

  /**
   * Adds an attribute. The attribute's non-normalized value of the
   * attribute will have the same value as the attribute value until
   * set using the `setNonNormalizedValue` method. Also,
   * the added attribute will be marked as specified in the XML instance
   * document unless set otherwise using the `setSpecified`
   * method.
   * 
   * This method differs from `addAttribute` in that it
   * does not check if an attribute of the same name already exists
   * in the list before adding it. In order to improve performance
   * of namespace processing, this method allows uniqueness checks
   * to be deferred until all the namespace information is available
   * after the entire attribute specification has been read.
   * 
   * *Caution:* If this method is called it should
   * not be mixed with calls to `addAttribute` unless
   * it has been determined that all the attribute names are unique.
   *
   * @param name the attribute name
   * @param type the attribute type
   * @param value the attribute value
   *
   * @see #setNonNormalizedValue
   * @see #setSpecified
   * @see #checkDuplicatesNS
   */
  def addAttributeNS(name: QName, `type`: String, value: String): Unit = {
    val index = fLength
    if (fLength == fAttributes.length) {
      var attributes: Array[Attribute] = null
      attributes =
        if (fLength + 1 < SIZE_LIMIT)
          new Array[Attribute](fAttributes.length + 4)
        else
          new Array[Attribute](fAttributes.length << 1)
      System.arraycopy(fAttributes, 0, attributes, 0, fAttributes.length)
      for (i ← fAttributes.length until attributes.length) {
        attributes(i) = new Attribute()
      }
      fAttributes = attributes
    }
    fLength += 1
    val attribute = fAttributes(index)
    attribute.name.setValues(name)
    attribute.`type` = `type`
    attribute.value = value
    attribute.nonNormalizedValue = value
    attribute.specified = false
    attribute.augs.removeAllItems()
  }

  /**
   * Checks for duplicate expanded names (local part and namespace name
   * pairs) in the attribute specification. If a duplicate is found its
   * name is returned.
   * 
   * This should be called once all the in-scope namespaces for the element
   * enclosing these attributes is known, and after all the attributes
   * have gone through namespace binding.
   *
   * @return the name of a duplicate attribute found in the search,
   * otherwise null.
   */
  def checkDuplicatesNS(): QName = {
    if (fLength <= SIZE_LIMIT) {
      for (i ← 0 until fLength - 1) {
        val att1 = fAttributes(i)
        for (j ← i + 1 until fLength) {
          val att2 = fAttributes(j)
          if (att1.name.localpart == att2.name.localpart && att1.name.uri == att2.name.uri) {
            return att2.name
          }
        }
      }
    } else {
      fIsTableViewConsistent = false
      prepareTableView()
      var attr: Attribute = null
      var bucket: Int = 0
      var i = fLength - 1
      while (i >= 0) {
        attr = fAttributes(i)
        bucket = getTableViewBucket(attr.name.localpart, attr.name.uri)
        if (fAttributeTableViewChainState(bucket) != fLargeCount) {
          fAttributeTableViewChainState(bucket) = fLargeCount
          attr.next = null
          fAttributeTableView(bucket) = attr
        } else {
          var found = fAttributeTableView(bucket)
          while (found ne null) {
            if (found.name.localpart == attr.name.localpart && found.name.uri == attr.name.uri) {
              return attr.name
            }
            found = found.next
          }
          attr.next = fAttributeTableView(bucket)
          fAttributeTableView(bucket) = attr
        }
        i
      }
    }
    null
  }

  /**
   * Look up the index of an attribute by Namespace name.
   * 
   * *Note:*
   * This method uses reference comparison, and thus should
   * only be used internally. We cannot use this method in any
   * code exposed to users as they may not pass in unique strings.
   *
   * @param uri The Namespace URI, or null if
   *        the name has no Namespace URI.
   * @param localPart The attribute's local name.
   * @return The index of the attribute, or -1 if it does not
   *         appear in the list.
   */
  def getIndexFast(uri: String, localPart: String): Int = {
    for (i ← 0 until fLength) {
      val attribute = fAttributes(i)
      if (attribute.name.localpart == localPart && attribute.name.uri == uri) {
        return i
      }
    }
    -1
  }

  /**
   * Returns the value passed in or NMTOKEN if it's an enumerated type.
   *
   * @param type attribute type
   * @return the value passed in or NMTOKEN if it's an enumerated type.
   */
  private def getReportableType(`type`: String): String = {
    if (`type`.charAt(0) == '(') {
      return "NMTOKEN"
    }
    `type`
  }

  /**
   * Returns the position in the table view
   * where the given attribute name would be hashed.
   *
   * @param qname the attribute name
   * @return the position in the table view where the given attribute
   * would be hashed
   */
  protected def getTableViewBucket(qname: String): Int = {
    (qname.hashCode & 0x7FFFFFFF) % fTableViewBuckets
  }

  /**
   * Returns the position in the table view
   * where the given attribute name would be hashed.
   *
   * @param localpart the local part of the attribute
   * @param uri the namespace name of the attribute
   * @return the position in the table view where the given attribute
   * would be hashed
   */
  protected def getTableViewBucket(localpart: String, uri: String): Int = {
    if (uri eq null) {
      (localpart.hashCode & 0x7FFFFFFF) % fTableViewBuckets
    } else {
      ((localpart.hashCode + uri.hashCode) & 0x7FFFFFFF) % fTableViewBuckets
    }
  }

  /**
   * Purges all elements from the table view.
   */
  protected def cleanTableView(): Unit = {
    fLargeCount += 1
    if (fLargeCount < 0) {
      if (fAttributeTableViewChainState ne null) {
        val i = fTableViewBuckets - 1
        while (i >= 0) {
          fAttributeTableViewChainState(i) = 0
        }
      }
      fLargeCount = 1
    }
  }

  /**
   * Prepares the table view of the attributes list for use.
   */
  protected def prepareTableView(): Unit = {
    if (fAttributeTableView eq null) {
      fAttributeTableView = new Array[Attribute](fTableViewBuckets)
      fAttributeTableViewChainState = new Array[Int](fTableViewBuckets)
    } else {
      cleanTableView()
    }
  }

  /**
   * Prepares the table view of the attributes list for use,
   * and populates it with the attributes which have been
   * previously read.
   */
  protected def prepareAndPopulateTableView(): Unit = {
    prepareTableView()
    var attr: Attribute = null
    var bucket: Int = 0
    for (i ← 0 until fLength) {
      attr = fAttributes(i)
      bucket = getTableViewBucket(attr.name.rawname)
      if (fAttributeTableViewChainState(bucket) != fLargeCount) {
        fAttributeTableViewChainState(bucket) = fLargeCount
        attr.next = null
        fAttributeTableView(bucket) = attr
      } else {
        attr.next = fAttributeTableView(bucket)
        fAttributeTableView(bucket) = attr
      }
    }
  }
}
