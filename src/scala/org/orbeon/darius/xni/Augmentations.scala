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

package org.orbeon.darius.xni

/**
 * The Augmentations interface defines a table of additional data that may
 * be passed along the document pipeline. The information can contain extra
 * arguments or infoset augmentations, for example PSVI. This additional
 * information is identified by a String key.
 * 
 * *Note:*
 * Methods that receive Augmentations are required to copy the information
 * if it is to be saved for use beyond the scope of the method.
 * The Augmentations content is volatile, and maybe modified by any method in
 * any component in the pipeline. Therefore, methods passed this structure
 * should not save any reference to the structure.
 */
trait Augmentations {

  /**
   * Add additional information identified by a key to the Augmentations structure.
   *
   * @param key    Identifier, can't be `null`
   * @param item   Additional information
   *
   * @return the previous value of the specified key in the Augmentations structure,
   *         or `null` if it did not have one.
   */
  def putItem(key: String, item: Any): Any

  /**
   * Get information identified by a key from the Augmentations structure
   *
   * @param key    Identifier, can't be `null`
   *
   * @return the value to which the key is mapped in the Augmentations structure;
   *         `null` if the key is not mapped to any value.
   */
  def getItem(key: String): Any

  /**
   * Remove additional info from the Augmentations structure
   *
   * @param key    Identifier, can't be `null`
   * @return the previous value of the specified key in the Augmentations structure,
   *         or `null` if it did not have one.
   */
  def removeItem(key: String): Any

  /**
   * Returns an enumeration of the keys in the Augmentations structure
   *
   */
  def keys(): java.util.Enumeration[String]

  /**
   * Remove all objects from the Augmentations structure.
   */
  def removeAllItems(): Unit
}
