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

package za.co.absa.enceladus.conformance

import java.time.ZonedDateTime

import org.scalatest.FunSuite
import za.co.absa.enceladus.dao.auth.{MenasKerberosCredentials, MenasPlainCredentials}
import za.co.absa.enceladus.model.Dataset
import za.co.absa.enceladus.utils.testUtils.SparkTestBase

class ConfConfigSuite extends FunSuite with SparkTestBase {

  private val year = "2018"
  private val month = "12"
  private val day = "31"
  private val dateTokens = Array(year, month, day)
  private val hdfsRawPath = "/bigdatahdfs/datalake/raw/system/feed"
  private val hdfsPublishPath = "/bigdatahdfs/datalake/publish/system/feed"
  private val hdfsPublishPathOverride = "/bigdatahdfs/datalake/publish/system/feed/override"
  private val menasCredentialsFile = "src/test/resources/menas-credentials.conf"
  private val menasCredentials = MenasPlainCredentials.fromFile(menasCredentialsFile)
  private val keytabPath = "src/test/resources/user.keytab.example"
  private val menasKeytab = MenasKerberosCredentials("user@EXAMPLE.COM", keytabPath)
  private val datasetName = "test-dataset-name"
  private val datasetVersion = 2
  private val description = None
  private val schemaName = "test-schema-name"
  private val schemaVersion = 4
  private val dateCreated = ZonedDateTime.now()
  private val userCreated = "user"
  private val lastUpdated = ZonedDateTime.now()
  private val userUpdated = "user"
  private val reportDate = s"$year-$month-$day"
  private val reportVersion = 3
  private val disabled = false
  private val dateDisabled = None
  private val userDisabled = None
  private val rawFormat = "parquet"
  private val folderPrefix = s"year=$year/month=$month/day=$day"
  private val infoDateColumn = "enceladus_info_date"
  private val infoVersionColumn = "enceladus_info_version"

  private object TestDynamicConformance extends ConformanceExecution

  test("Test credentials file parsing "){
    val credentials = MenasPlainCredentials.fromFile(menasCredentialsFile)

    assert(credentials.username == "user")
    assert(credentials.password == "changeme")
  }

  test("Test keytab file parsing "){
    val credentials = MenasKerberosCredentials.fromFile(keytabPath)

    assert(credentials === menasKeytab)
  }

  test("folder-prefix parameter") {
    val cmdConfigNoFolderPrefix = ConfCmdConfig.getCmdLineArguments(
      Array(
        "--dataset-name", datasetName,
        "--dataset-version", datasetVersion.toString,
        "--report-date", reportDate,
        "--report-version", reportVersion.toString,
        "--menas-credentials-file", menasCredentialsFile))

    val actualPlainMenasCredentials = cmdConfigNoFolderPrefix.jobConfig.menasCredentialsFactory.getInstance()

    assert(cmdConfigNoFolderPrefix.jobConfig.datasetName === datasetName)
    assert(cmdConfigNoFolderPrefix.jobConfig.datasetVersion === datasetVersion)
    assert(cmdConfigNoFolderPrefix.jobConfig.reportDate === reportDate)
    assert(cmdConfigNoFolderPrefix.jobConfig.reportVersion.get === reportVersion)
    assert(cmdConfigNoFolderPrefix.jobConfig.folderPrefix.isEmpty)
    assert(cmdConfigNoFolderPrefix.publishPathOverride.isEmpty)
    assert(actualPlainMenasCredentials === menasCredentials)

    val cmdConfigFolderPrefix = ConfCmdConfig.getCmdLineArguments(
      Array(
        "--dataset-name", datasetName,
        "--dataset-version", datasetVersion.toString,
        "--report-date", reportDate,
        "--report-version", reportVersion.toString,
        "--menas-auth-keytab", keytabPath,
        "--folder-prefix", folderPrefix))

    val actualMenasKerberosCredentials = cmdConfigFolderPrefix.jobConfig.menasCredentialsFactory.getInstance()

    assert(cmdConfigFolderPrefix.jobConfig.datasetName === datasetName)
    assert(cmdConfigFolderPrefix.jobConfig.datasetVersion === datasetVersion)
    assert(cmdConfigFolderPrefix.jobConfig.reportDate === reportDate)
    assert(cmdConfigFolderPrefix.jobConfig.reportVersion.get === reportVersion)
    assert(cmdConfigFolderPrefix.jobConfig.folderPrefix.nonEmpty)
    assert(cmdConfigFolderPrefix.jobConfig.folderPrefix.get === folderPrefix)
    assert(cmdConfigFolderPrefix.publishPathOverride.isEmpty)
    assert(actualMenasKerberosCredentials === menasKeytab)

    val cmdConfigPublishPathOverrideAndFolderPrefix = ConfCmdConfig.getCmdLineArguments(
      Array(
        "--dataset-name", datasetName,
        "--dataset-version", datasetVersion.toString,
        "--report-date", reportDate,
        "--report-version", reportVersion.toString,
        "--menas-credentials-file", menasCredentialsFile,
        "--debug-set-publish-path", hdfsPublishPathOverride,
        "--folder-prefix", folderPrefix))

    assert(cmdConfigPublishPathOverrideAndFolderPrefix.jobConfig.datasetName === datasetName)
    assert(cmdConfigPublishPathOverrideAndFolderPrefix.jobConfig.datasetVersion === datasetVersion)
    assert(cmdConfigPublishPathOverrideAndFolderPrefix.jobConfig.reportDate === reportDate)
    assert(cmdConfigPublishPathOverrideAndFolderPrefix.jobConfig.reportVersion.get === reportVersion)
    assert(cmdConfigPublishPathOverrideAndFolderPrefix.jobConfig.folderPrefix.nonEmpty)
    assert(cmdConfigPublishPathOverrideAndFolderPrefix.jobConfig.folderPrefix.get === folderPrefix)
    assert(cmdConfigPublishPathOverrideAndFolderPrefix.publishPathOverride.nonEmpty)
    assert(cmdConfigPublishPathOverrideAndFolderPrefix.publishPathOverride.get === hdfsPublishPathOverride)
  }

  test("Test buildPublishPath") {
    val conformanceDataset = Dataset(
      datasetName,
      datasetVersion,
      description,
      hdfsRawPath,
      hdfsPublishPath,
      schemaName,
      schemaVersion,
      dateCreated,
      userCreated,
      lastUpdated,
      userUpdated,
      disabled,
      dateDisabled,
      userDisabled,
      List()
    )
    val cmdConfigNoFolderPrefix = ConfCmdConfig.getCmdLineArguments(
      Array(
        "--dataset-name", datasetName,
        "--dataset-version", datasetVersion.toString,
        "--report-date", reportDate,
        "--report-version", reportVersion.toString,
        "--menas-credentials-file", menasCredentialsFile
      ))
    val cmdConfigFolderPrefix = ConfCmdConfig.getCmdLineArguments(
      Array(
        "--dataset-name", datasetName,
        "--dataset-version", datasetVersion.toString,
        "--report-date", reportDate,
        "--report-version", reportVersion.toString,
        "--menas-credentials-file", menasCredentialsFile,
        "--folder-prefix", folderPrefix))
    val cmdConfigPublishPathOverride = ConfCmdConfig.getCmdLineArguments(
      Array(
        "--dataset-name", datasetName,
        "--dataset-version", datasetVersion.toString,
        "--report-date", reportDate,
        "--report-version", reportVersion.toString,
        "--menas-credentials-file", menasCredentialsFile,
        "--debug-set-publish-path", hdfsPublishPathOverride))
    val cmdConfigPublishPathOverrideAndFolderPrefix = ConfCmdConfig.getCmdLineArguments(
      Array(
        "--dataset-name", datasetName,
        "--dataset-version", datasetVersion.toString,
        "--report-date", reportDate,
        "--report-version", reportVersion.toString,
        "--folder-prefix", folderPrefix,
        "--menas-credentials-file", menasCredentialsFile,
        "--debug-set-publish-path", hdfsPublishPathOverride))
    val publishPathNoFolderPrefix = TestDynamicConformance.buildPublishPath(cmdConfigNoFolderPrefix,
      conformanceDataset, cmdConfigNoFolderPrefix.jobConfig.reportVersion.get)
    assert(publishPathNoFolderPrefix === s"$hdfsPublishPath/$infoDateColumn=$reportDate/$infoVersionColumn=$reportVersion")
    val publishPathFolderPrefix = TestDynamicConformance.buildPublishPath(cmdConfigFolderPrefix,
      conformanceDataset, cmdConfigFolderPrefix.jobConfig.reportVersion.get)
    assert(publishPathFolderPrefix === s"$hdfsPublishPath/$folderPrefix/$infoDateColumn=$reportDate/$infoVersionColumn=$reportVersion")
    val publishPathPublishPathOverride = TestDynamicConformance.buildPublishPath(cmdConfigPublishPathOverride, conformanceDataset, cmdConfigPublishPathOverride.jobConfig.reportVersion.get)
    assert(publishPathPublishPathOverride === hdfsPublishPathOverride)

    val publishPathPublishPathOverrideAndFolderPrefix =
      TestDynamicConformance.buildPublishPath(cmdConfigPublishPathOverrideAndFolderPrefix,
        conformanceDataset, cmdConfigPublishPathOverrideAndFolderPrefix.jobConfig.reportVersion.get)
    assert(publishPathPublishPathOverrideAndFolderPrefix === hdfsPublishPathOverride)
  }

}
