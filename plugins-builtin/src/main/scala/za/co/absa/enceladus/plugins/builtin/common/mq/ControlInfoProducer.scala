/*
 * Copyright 2018 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.enceladus.plugins.builtin.common.mq

import za.co.absa.enceladus.plugins.builtin.controlinfo.DceControlInfo

/**
 * Base interface for control info metrics (aka INFO file) producer for messaging queues.
 */
trait ControlInfoProducer {

  /**
   * Send control metrics to a messaging queue.
   *
   * @param controlInfo Control info metrics to send.
   */
  def send(controlInfo: DceControlInfo): Unit

  /**
   * This method should be called when the producer is no longer needed.
   */
  def close(): Unit
}