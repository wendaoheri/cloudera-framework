package com.cloudera.framework.example.three;

import com.cloudera.framework.example.three.test.Model;
import com.cloudera.framework.testing.server.DfsServer;
import com.cloudera.framework.testing.server.SparkServer;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
  Model.class,
})

public class TestSuite {

  @ClassRule
  public static TestRule cdhServers = RuleChain
    .outerRule(DfsServer.getInstance(DfsServer.Runtime.CLUSTER_DFS))
    .around(SparkServer.getInstance());

}
