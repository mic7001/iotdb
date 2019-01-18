/**
 * Copyright © 2019 Apache IoTDB(incubating) (dev@iotdb.apache.org)
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.iotdb.db.engine.overflow.metadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OFRowGroupListMetadataTest {

  private String path = "OFRowGroupListMetadataTest";

  @Before
  public void setUp() throws Exception {

  }

  @After
  public void tearDown() throws Exception {
    File file = new File(path);
    if (file.exists()) {
      file.delete();
    }
  }

  @Test
  public void testOFRowGroupListMetadata() throws Exception {
    OFRowGroupListMetadata ofRowGroupListMetadata = OverflowTestHelper
        .createOFRowGroupListMetadata();
    serialize(ofRowGroupListMetadata);
    OFRowGroupListMetadata deOfRowGroupListMetadata = deSerialized();
    OverflowUtils.isOFRowGroupListMetadataEqual(ofRowGroupListMetadata, deOfRowGroupListMetadata);
  }

  private void serialize(OFRowGroupListMetadata ofRowGroupListMetadata)
      throws FileNotFoundException {
    FileOutputStream outputStream = new FileOutputStream(path);
    try {
      ofRowGroupListMetadata.serializeTo(outputStream);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (outputStream != null) {
        try {
          outputStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private OFRowGroupListMetadata deSerialized() throws FileNotFoundException {
    FileInputStream inputStream = new FileInputStream(path);
    try {
      OFRowGroupListMetadata ofRowGroupListMetadata = OFRowGroupListMetadata
          .deserializeFrom(inputStream);
      return ofRowGroupListMetadata;
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return null;
  }
}