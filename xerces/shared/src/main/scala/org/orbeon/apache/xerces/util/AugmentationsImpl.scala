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

package org.orbeon.apache.xerces.util

import org.orbeon.apache.xerces.xni.Augmentations

// @ebruchez: stub to get the code to compile as we don't need Augmentations.
class AugmentationsImpl extends Augmentations {
  def putItem(key: String, item: Any): Any = null
  def removeItem(key: String): Any = ???
  def getItem(key: String): Any = ???
  def keys(): Iterator[String] = ???
  def removeAllItems(): Unit = ()
}
