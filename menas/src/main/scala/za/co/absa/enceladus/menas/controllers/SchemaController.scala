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

package za.co.absa.enceladus.menas.controllers

import java.util.Optional
import java.util.concurrent.CompletableFuture

import javax.servlet.http.HttpServletResponse
import org.apache.spark.sql.types.StructType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation._
import org.springframework.web.multipart.MultipartFile
import za.co.absa.enceladus.menas.models.SchemaApiFeatures
import za.co.absa.enceladus.menas.models.rest.exceptions.SchemaParsingException
import za.co.absa.enceladus.menas.repositories.RefCollection
import za.co.absa.enceladus.menas.services.{AttachmentService, SchemaRegistryService, SchemaService}
import za.co.absa.enceladus.menas.utils.SchemaType
import za.co.absa.enceladus.menas.utils.converters.SparkMenasSchemaConvertor
import za.co.absa.enceladus.menas.utils.parsers.SchemaParser
import za.co.absa.enceladus.model.Schema
import za.co.absa.enceladus.model.menas._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


@RestController
@RequestMapping(Array("/api/schema"))
class SchemaController @Autowired()(
                                     schemaService: SchemaService,
                                     attachmentService: AttachmentService,
                                     sparkMenasConvertor: SparkMenasSchemaConvertor,
                                     schemaRegistryService: SchemaRegistryService
                                     )
  extends VersionedModelController(schemaService) {

  import za.co.absa.enceladus.menas.utils.implicits._

  import scala.concurrent.ExecutionContext.Implicits.global

  @PostMapping(Array("/remote"))
  @ResponseStatus(HttpStatus.CREATED)
  def handleRemoteFile(@AuthenticationPrincipal principal: UserDetails,
                       @RequestParam remoteUrl: String,
                       @RequestParam version: Int,
                       @RequestParam name: String,
                       @RequestParam format: Optional[String]): CompletableFuture[Option[Schema]] = {

    val schemaType: SchemaType.Value = SchemaType.fromOptSchemaName(format)
    val schemaResponse = schemaRegistryService.loadSchemaByUrl(remoteUrl)
    val sparkStruct = SchemaParser.getFactory(sparkMenasConvertor).getParser(schemaType).parse(schemaResponse.fileContent)

    val menasFile = MenasAttachment(refCollection = RefCollection.SCHEMA.name().toLowerCase,
      refName = name,
      refVersion = version + 1,  // version is the current one, refVersion is the to-be-created one
      attachmentType = MenasAttachment.ORIGINAL_SCHEMA_ATTACHMENT,
      filename = schemaResponse.url.getFile,
      fileContent = schemaResponse.fileContent.getBytes,
      fileMIMEType = schemaResponse.mimeType)

    uploadSchemaToMenas(principal.getUsername, menasFile, sparkStruct, schemaType)
  }

  @PostMapping(Array("/registry"))
  @ResponseStatus(HttpStatus.CREATED)
  def handleSubject(@AuthenticationPrincipal principal: UserDetails,
                      @RequestParam subject: String,
                      @RequestParam version: Int,
                      @RequestParam name: String,
                      @RequestParam format: Optional[String]): CompletableFuture[Option[Schema]] = {

    val schemaType: SchemaType.Value = SchemaType.fromOptSchemaName(format)

    val valueSchemaResponse = Try {
      schemaRegistryService.loadSchemaBySubjectName(s"$subject")
    } match {
      case Success(schemaResponse) => schemaResponse
      case Failure(_) => schemaRegistryService.loadSchemaBySubjectName(s"$subject-value") // fallback to -value
    }

    val valueSparkStruct = SchemaParser.getFactory(sparkMenasConvertor).getParser(schemaType).parse(valueSchemaResponse.fileContent)

    val menasFile = MenasAttachment(refCollection = RefCollection.SCHEMA.name().toLowerCase,
      refName = name,
      refVersion = version + 1,  // version is the current one, refVersion is the to-be-created one
      attachmentType = MenasAttachment.ORIGINAL_SCHEMA_ATTACHMENT,
      filename = valueSchemaResponse.url.getFile, // only the value file gets saved as an attachment
      fileContent = valueSchemaResponse.fileContent.getBytes,
      fileMIMEType = valueSchemaResponse.mimeType)

    uploadSchemaToMenas(principal.getUsername, menasFile, valueSparkStruct, schemaType)
  }

  @PostMapping(Array("/upload"))
  @ResponseStatus(HttpStatus.CREATED)
  def handleFileUpload(@AuthenticationPrincipal principal: UserDetails,
                       @RequestParam file: MultipartFile,
                       @RequestParam version: Int,
                       @RequestParam name: String,
                       @RequestParam format: Optional[String]): CompletableFuture[Option[Schema]] = {

    val fileContent = new String(file.getBytes)

    val schemaType = SchemaType.fromOptSchemaName(format)
    val sparkStruct = SchemaParser.getFactory(sparkMenasConvertor).getParser(schemaType).parse(fileContent)

    // for avro schema type, always force the same mime-type to be persisted
    val mime = if (schemaType == SchemaType.Avro) {
     SchemaController.avscContentType
    } else {
      file.getContentType
    }

    val menasFile = MenasAttachment(refCollection = RefCollection.SCHEMA.name().toLowerCase,
      refName = name,
      refVersion = version + 1, // version is the current one, refVersion is the to-be-created one
      attachmentType = MenasAttachment.ORIGINAL_SCHEMA_ATTACHMENT,
      filename = file.getOriginalFilename,
      fileContent = file.getBytes,
      fileMIMEType = mime)

    uploadSchemaToMenas(principal.getUsername, menasFile, sparkStruct, schemaType)
  }

  /**
   * Common for [[SchemaController#handleFileUpload]] and [[SchemaController#handleRemoteFile]]
   */
  private def uploadSchemaToMenas(username: String, menasAttachment: MenasAttachment, sparkStruct: StructType,
                                  schemaType: SchemaType.Value): CompletableFuture[Option[Schema]] = {
    try {
      for {
        // the parsing of sparkStruct can fail, therefore we try to save it first before saving the attachment
        update <- schemaService.schemaUpload(username, menasAttachment.refName, menasAttachment.refVersion - 1, sparkStruct)
        _ <- attachmentService.uploadAttachment(menasAttachment)
      } yield update
    } catch {
      case e: SchemaParsingException => throw e.copy(schemaType = schemaType) //adding schema type
    }
  }

  @GetMapping(path = Array("/export/{name}/{version}"))
  @ResponseStatus(HttpStatus.OK)
  def exportSchema(@AuthenticationPrincipal principal: UserDetails,
                   @PathVariable name: String,
                   @PathVariable version: Int,
                   response: HttpServletResponse): CompletableFuture[Array[Byte]] = {
    attachmentService.getSchemaByNameAndVersion(name, version).map { attachment =>
      response.addHeader("mime-type", attachment.fileMIMEType)
      attachment.fileContent
    }
  }

  @GetMapping(path = Array("/json/{name}/{version}"), produces = Array("application/json"))
  @ResponseStatus(HttpStatus.OK)
  def getJson(@PathVariable name: String,
              @PathVariable version: Int,
              @RequestParam(defaultValue = "false") pretty: Boolean): CompletableFuture[String] = {
    schemaService.getVersion(name, version).map {
      case Some(schema) =>
        if (schema.fields.isEmpty) throw notFound()

        val sparkStruct = StructType(sparkMenasConvertor.convertMenasToSparkFields(schema.fields))
        if (pretty) sparkStruct.prettyJson else sparkStruct.json
      case None =>
        throw notFound()
    }
  }

  @GetMapping(path = Array("/features"))
  @ResponseStatus(HttpStatus.OK)
  def getAvailability(): CompletableFuture[SchemaApiFeatures] = {
    val registryAvailable = schemaRegistryService.schemaRegistryBaseUrl.isDefined

    // the ability to upload or input a remoteUrl is always enabled, so it is not part of the availability endpoint at the moment
    Future.successful(SchemaApiFeatures(registry = registryAvailable))
  }
}

object SchemaController {
  val avscContentType = "application/vnd.schemaregistry.v1+json"
}
