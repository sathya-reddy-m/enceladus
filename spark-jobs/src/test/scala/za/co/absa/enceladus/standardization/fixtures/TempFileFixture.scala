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

package za.co.absa.enceladus.standardization.fixtures

import java.io.{DataOutputStream, File, FileOutputStream}
import java.nio.charset.Charset

import org.apache.spark.sql.{DataFrame, SaveMode}
import za.co.absa.commons.io.{TempDirectory, TempFile}

/**
  * This fixture adds ability for a unit test to create temporary files for using them in the tests.
  */
trait TempFileFixture {
  /**
    * Creates a temporary text file and returns the full path to it
    *
    * @param prefix       The prefix string to be used in generating the file's name
    *                     must be at least three characters long
    * @param suffix       The suffix string to be used in generating the file's name
    *                     may be <code>null</code>, in which case the suffix <code>".tmp"</code> will be used
    * @param charset      A charset of the data in the temporaty text file
    * @param content      A contents to put to the file
    * @param deleteOnExit If true the file will be deleted when not referenced anymore
    * @return The full path to the temporary file
    */
  def createTempFile(prefix: String, suffix: String, charset: Charset, content: String, deleteOnExit: Boolean = true): File = {
    createTempBinFile(prefix, suffix, content.getBytes(charset), deleteOnExit)
  }

  /**
   * Creates a temporary binary file and returns the full path to it
   *
   * @param prefix       The prefix string to be used in generating the file's name
   *                     must be at least three characters long
   * @param suffix       The suffix string to be used in generating the file's name
   *                     may be <code>null</code>, in which case the suffix <code>".tmp"</code> will be used
   * @param content      A contents to put to the file
   * @param deleteOnExit If true the file will be deleted when not referenced anymore
   * @return The full path to the temporary file
   */
  def createTempBinFile(prefix: String, suffix: String, content: Array[Byte], deleteOnExit: Boolean = true): File = {
    val tempFile = TempFile(prefix, suffix)
    if (deleteOnExit) {
      tempFile.deleteOnExit()
    }
    val result = tempFile.path.toFile
    val ostream = new DataOutputStream(new FileOutputStream(result))
    try {
      ostream.write(content)
    } finally {
      ostream.close()
    }
    result
  }

  /**
    * Creates a temporary directory and save the dataFrame data into in parquet format
    *
    * @param prefix       The prefix string to be used in generating the file's name
    *                     must be at least three characters long
    * @param data         data to be saved
    * @param deleteOnExit If true the directory will be deleted when not referenced anymore
    * @return
    */
  def createTempParquetFile(prefix: String, data: DataFrame, deleteOnExit: Boolean = true): File = {
    val tempDir = TempDirectory(prefix, ".parquet", pathOnly = false)
    if (deleteOnExit) {
      tempDir.deleteOnExit()
    }
    val result = tempDir.path.toFile
    data.write.mode(SaveMode.Overwrite).parquet(result.getAbsolutePath)
    result
  }

}
