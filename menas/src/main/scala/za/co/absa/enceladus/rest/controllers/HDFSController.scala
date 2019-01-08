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

package za.co.absa.enceladus.rest.controllers

import java.util.concurrent.CompletableFuture

import org.apache.hadoop.fs.Path
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation._
import za.co.absa.enceladus.model.api.HDFSFolder
import za.co.absa.enceladus.rest.services.HDFSService

import scala.concurrent._

@RestController
@RequestMapping(Array("/api/hdfs"))
class HDFSController @Autowired() (hdfsService: HDFSService) {
  
  import za.co.absa.enceladus.rest.utils.implicits._

  import scala.concurrent.ExecutionContext.Implicits.global

  private val logger = LoggerFactory.getLogger(this.getClass)
  
  @PostMapping(path = Array("/list"))
  def getHDFSFolder(@RequestBody pathStr: String): CompletableFuture[ResponseEntity[HDFSFolder]] = {
    val path = new Path(pathStr)

    hdfsService.exists(path).flatMap { exists =>
      if (exists) {
        hdfsService.getFolder(path).map(result => ResponseEntity.ok(result))
      } else {
        Future.successful(ResponseEntity.notFound().build[HDFSFolder]())
      }
    }
  }

}
