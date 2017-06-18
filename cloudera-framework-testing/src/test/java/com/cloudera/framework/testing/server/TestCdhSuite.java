package com.cloudera.framework.testing.server;

import com.cloudera.framework.testing.server.test.TestDfsServerDefault;
import com.cloudera.framework.testing.server.test.TestFlumeServer;
import com.cloudera.framework.testing.server.test.TestKafkaServer;
import com.cloudera.framework.testing.server.test.TestKuduServer;
import com.cloudera.framework.testing.server.test.TestMqttServer;
import com.cloudera.framework.testing.server.test.TestMrServerDefault;
import com.cloudera.framework.testing.server.test.TestPythonServer;
import com.cloudera.framework.testing.server.test.TestZooKeeperServer;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ //
  TestDfsServerDefault.class, //
  TestKuduServer.class, //
  TestZooKeeperServer.class, //
  TestKafkaServer.class, //
  TestMqttServer.class, //
  TestFlumeServer.class, //
  TestMrServerDefault.class, //

  // TODO: Re-enable once Spark local is re-entrant
  // TestSparkServerDefault.class, //
  // TestHiveServerMrDefault.class, //
  
  TestPythonServer.class, //
})

public class TestCdhSuite {

  @ClassRule
  public static TestRule cdhServers = RuleChain //
    .outerRule(DfsServer.getInstance()) //
    .around(KuduServer.getInstance()) //
    .around(ZooKeeperServer.getInstance()) //
    .around(KafkaServer.getInstance()) //
    .around(MqttServer.getInstance()) //
    .around(FlumeServer.getInstance()) //
    .around(MrServer.getInstance()) //
    .around(SparkServer.getInstance()) //
    .around(HiveServer.getInstance()) //
    .around(PythonServer.getInstance());

}
