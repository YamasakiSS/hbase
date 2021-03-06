/**
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
package org.apache.hadoop.hbase.client;

import static org.junit.Assert.fail;

import java.net.UnknownHostException;
import org.apache.hadoop.hbase.HBaseClassTestRule;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.testclassification.ClientTests;
import org.apache.hadoop.hbase.testclassification.MediumTests;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests that we fail fast when hostname resolution is not working and do not cache unresolved
 * InetSocketAddresses.
 */
@Category({ MediumTests.class, ClientTests.class })
public class TestCIBadHostname {

  @ClassRule
  public static final HBaseClassTestRule CLASS_RULE =
    HBaseClassTestRule.forClass(TestCIBadHostname.class);

  private static HBaseTestingUtility TEST_UTIL = new HBaseTestingUtility();
  private static AsyncConnectionImpl CONN;

  @BeforeClass
  public static void setupBeforeClass() throws Exception {
    TEST_UTIL.startMiniCluster();
    CONN = (AsyncConnectionImpl) ConnectionFactory
      .createAsyncConnection(TEST_UTIL.getConfiguration()).get();
  }

  @AfterClass
  public static void teardownAfterClass() throws Exception {
    CONN.close();
    TEST_UTIL.shutdownMiniCluster();
  }

  @Test(expected = UnknownHostException.class)
  public void testGetAdminBadHostname() throws Exception {
    // verify that we can get an instance with the cluster hostname
    ServerName master = TEST_UTIL.getHBaseCluster().getMaster().getServerName();
    try {
      CONN.getAdminStub(master);
    } catch (UnknownHostException uhe) {
      fail("Obtaining admin to the cluster master should have succeeded");
    }

    // test that we fail to get a client to an unresolvable hostname, which
    // means it won't be cached
    ServerName badHost = ServerName.valueOf("unknownhost.invalid:" + HConstants.DEFAULT_MASTER_PORT,
      System.currentTimeMillis());
    CONN.getAdminStub(badHost);
    fail("Obtaining admin to unresolvable hostname should have failed");
  }

  @Test(expected = UnknownHostException.class)
  public void testGetClientBadHostname() throws Exception {
    // verify that we can get an instance with the cluster hostname
    ServerName rs = TEST_UTIL.getHBaseCluster().getRegionServer(0).getServerName();
    try {
      CONN.getRegionServerStub(rs);
    } catch (UnknownHostException uhe) {
      fail("Obtaining client to the cluster regionserver should have succeeded");
    }

    // test that we fail to get a client to an unresolvable hostname, which
    // means it won't be cached
    ServerName badHost = ServerName.valueOf(
      "unknownhost.invalid:" + HConstants.DEFAULT_REGIONSERVER_PORT, System.currentTimeMillis());
    CONN.getRegionServerStub(badHost);
    fail("Obtaining client to unresolvable hostname should have failed");
  }
}
