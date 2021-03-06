package com.cloudera.framework.common;

import org.apache.hadoop.conf.Configuration;

public abstract class DriverSpark extends Driver {

  public DriverSpark() {
    super(Engine.SPARK);
  }

  public DriverSpark(Configuration conf) {
    super(conf, Engine.SPARK);
  }

}
