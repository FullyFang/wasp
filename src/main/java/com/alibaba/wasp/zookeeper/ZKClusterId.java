/**
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
package com.alibaba.wasp.zookeeper;

import com.alibaba.wasp.DeserializationException;import org.apache.hadoop.hbase.Abortable;
import com.alibaba.wasp.ClusterId;
import com.alibaba.wasp.DeserializationException;
import org.apache.zookeeper.KeeperException;

/**
 * Publishes and synchronizes a unique identifier specific to a given HBase
 * cluster. The stored identifier is read from the file system by the active
 * master on startup, and is subsequently available to all watchers (including
 * clients).
 */
public class ZKClusterId {
  private ZooKeeperWatcher watcher;
  private Abortable abortable;
  private ClusterId id;

  public ZKClusterId(ZooKeeperWatcher watcher, Abortable abortable) {
    this.watcher = watcher;
    this.abortable = abortable;
  }

  public boolean hasId() {
    return getId() != null;
  }

  public ClusterId getId() {
    try {
      if (id == null) {
        id = readClusterIdZNode(watcher);
      }
    } catch (KeeperException ke) {
      abortable.abort("Unexpected exception from ZooKeeper reading cluster ID",
          ke);
    }
    return id;
  }

  public static ClusterId readClusterIdZNode(ZooKeeperWatcher watcher)
      throws KeeperException {
    if (ZKUtil.checkExists(watcher, watcher.clusterIdZNode) != -1) {
      byte[] data = ZKUtil.getData(watcher, watcher.clusterIdZNode);
      if (data != null) {
        try {
          return ClusterId.parseFrom(data);
        } catch (DeserializationException e) {
          throw ZKUtil.convert(e);
        }
      }
    }
    return null;
  }

  public static void setClusterId(ZooKeeperWatcher watcher, ClusterId id)
      throws KeeperException {
    ZKUtil.createSetData(watcher, watcher.clusterIdZNode, id.toByteArray());
  }
}
