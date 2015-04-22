package org.orbeon.darius.util

import org.orbeon.darius.xni.Augmentations

// @ebruchez: stub to get the code to compile as we don't need Augmentations.
class AugmentationsImpl extends Augmentations {
  def putItem(key: String, item: Any): Any = null
  def removeItem(key: String): Any = ???
  def getItem(key: String): Any = ???
  def keys(): java.util.Enumeration[String] = ???
  def removeAllItems(): Unit = ()
}
