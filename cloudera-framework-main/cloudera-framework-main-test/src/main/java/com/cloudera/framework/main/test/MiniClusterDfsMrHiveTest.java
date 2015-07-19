package com.cloudera.framework.main.test;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.conf.HiveConf.ConfVars;
import org.apache.hadoop.hive.ql.Driver;
import org.apache.hadoop.hive.ql.exec.CopyTask;
import org.apache.hadoop.hive.ql.processors.CommandProcessor;
import org.apache.hadoop.hive.ql.processors.CommandProcessorFactory;
import org.apache.hadoop.hive.ql.session.SessionState;
import org.apache.hadoop.mapreduce.MRConfig;
import org.apache.hive.jdbc.miniHS2.MiniHS2;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all local-cluster DFS, MR and Hive tests, multi-process,
 * multi-threaded DFS, MR and Hive daemons, exercises the full read/write path
 * of the stack, provides isolated and idempotent runtime
 */
public class MiniClusterDfsMrHiveTest extends BaseTest {

  private static final String COMMAND_DELIMETER = ";";

  private static Logger LOG = LoggerFactory.getLogger(MiniClusterDfsMrHiveTest.class);

  private static HiveConf conf;
  private static MiniHS2 miniHs2;
  private static FileSystem fileSystem;

  public MiniClusterDfsMrHiveTest() {
    super();
  }

  public MiniClusterDfsMrHiveTest(String[] sources, String[] destinations, String[] datasets, String[][] subsets,
      String[][][] labels, @SuppressWarnings("rawtypes") Map[] counters) {
    super(sources, destinations, datasets, subsets, labels, counters);
  }

  @Override
  public Configuration getConf() {
    return conf;
  }

  @Override
  public FileSystem getFileSystem() {
    return fileSystem;
  }

  /**
   * Process a <code>statement</code>
   *
   * @param statement
   * @return {@link List} of {@link String} results, no result will be indicated
   *         by 1-length empty {@link String} {@link List}
   * @throws Exception
   */
  public static List<String> processStatement(String statement) throws Exception {
    return processStatement(statement, new HashMap<String, String>());
  }

  /**
   * Process a <code>statement</code>, making <code>hivevar</code> substitutions
   * from <code>parameters</code>
   *
   * @param statement
   * @param parameters
   * @return {@link List} of {@link String} results, no result will be indicated
   *         by 1-length empty {@link String} {@link List}
   * @throws Exception
   */
  public static List<String> processStatement(String statement, Map<String, String> parameters) throws Exception {
    long time = debugMessageHeader(LOG, "processStatement");
    List<String> results = new ArrayList<String>();
    CommandProcessor commandProcessor = CommandProcessorFactory.getForHiveCommand(
        (statement = new StrSubstitutor(parameters, "${hivevar:", "}").replace(statement.trim())).split("\\s+"), conf);
    if (LOG.isDebugEnabled()) {
      LOG.debug(LOG_PREFIX + " [processStatement] pre-execute, statement:\n" + statement);
    }
    int responseCode = (commandProcessor = commandProcessor == null ? new Driver(conf) : commandProcessor)
        .run(statement).getResponseCode();
    if (commandProcessor instanceof Driver) {
      ((Driver) commandProcessor).getResults(results);
    }
    if (results.isEmpty()) {
      results.add("");
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug(LOG_PREFIX + " [processStatement] post-execute, result:\n" + StringUtils.join(results.toArray(), "\n"));
    }
    debugMessageFooter(LOG, "processStatement", time);
    if (responseCode != 0) {
      throw new SQLException("Statement executed with error response code [" + responseCode + "]");
    }
    return results;
  }

  /**
   * Process a set of <code>;</code> delimited statements from a
   * <code>file</code> in a <code>directory</code>
   *
   * @param directory
   * @param file
   * @return {@link List} of {@link List} of {@link String} results per
   *         statement, no result will be indicated by 1-length empty
   *         {@link String} {@link List}
   * @throws Exception
   */
  public static List<List<String>> processStatement(String directory, String file) throws Exception {
    return processStatement(directory, file, new HashMap<String, String>());
  }

  /**
   * Process a set of <code>;</code> delimited statements from a
   * <code>file</code> in a <code>directory</code>, making <code>hivevar</code>
   * substitutions from <code>parameters</code>
   *
   * @param directory
   * @param file
   * @param parameters
   * @return {@link List} of {@link List} of {@link String} results per
   *         statement, no result will be indicated by 1-length empty
   *         {@link String} {@link List}
   * @throws Exception
   */
  public static List<List<String>> processStatement(String directory, String file, Map<String, String> parameters)
      throws Exception {
    List<List<String>> results = new ArrayList<List<String>>();
    for (String statement : readFileToLines(directory, file, COMMAND_DELIMETER)) {
      results.add(processStatement(statement, parameters));
    }
    return results;
  }

  @BeforeClass
  public static void setUpRuntime() throws Exception {
    long time = debugMessageHeader(LOG, "setUpRuntime");
    HiveConf hiveConf = new HiveConf(new Configuration(), CopyTask.class);
    miniHs2 = new MiniHS2(hiveConf, true);
    Map<String, String> hiveConfOverlay = new HashMap<String, String>();
    hiveConfOverlay.put(ConfVars.HIVE_SUPPORT_CONCURRENCY.varname, "false");
    hiveConfOverlay.put(MRConfig.FRAMEWORK_NAME, MRConfig.LOCAL_FRAMEWORK_NAME);
    miniHs2.start(hiveConfOverlay);
    fileSystem = miniHs2.getDfs().getFileSystem();
    SessionState.start(new SessionState(hiveConf));
    conf = miniHs2.getHiveConf();
    debugMessageFooter(LOG, "setUpRuntime", time);
  }

  @Before
  public void setUpSchema() throws Exception {
    long time = debugMessageHeader(LOG, "setUpSchema");
    for (String table : processStatement("SHOW TABLES")) {
      if (table.length() > 0) {
        processStatement("DROP TABLE " + table);
      }
    }
    debugMessageFooter(LOG, "setUpSchema", time);
  }

  @AfterClass
  public static void tearDownRuntime() throws Exception {
    long time = debugMessageHeader(LOG, "tearDownRuntime");
    if (miniHs2 != null) {
      miniHs2.stop();
    }
    debugMessageFooter(LOG, "tearDownRuntime", time);
  }

  private static List<String> readFileToLines(String directory, String file, String delimeter) throws IOException {
    List<String> lines = new ArrayList<String>();
    InputStream inputStream = MiniClusterDfsMrHiveTest.class.getResourceAsStream(directory + "/" + file);
    if (inputStream != null) {
      try {
        for (String line : IOUtils.toString(inputStream).split(delimeter)) {
          if (!line.trim().equals("")) {
            lines.add(line.trim());
          }
        }
        return lines;
      } finally {
        inputStream.close();
      }
    }
    throw new IOException("Could not load file [" + directory + "/" + file + "] from classpath");
  }

}
