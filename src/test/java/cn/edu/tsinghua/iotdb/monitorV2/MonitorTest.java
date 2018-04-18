package cn.edu.tsinghua.iotdb.monitorV2;

import cn.edu.tsinghua.iotdb.MonitorV2.MonitorConstants;
import cn.edu.tsinghua.iotdb.MonitorV2.StatMonitor;
import cn.edu.tsinghua.iotdb.conf.TsfileDBConfig;
import cn.edu.tsinghua.iotdb.conf.TsfileDBDescriptor;
import cn.edu.tsinghua.iotdb.jdbc.TsfileJDBCConfig;
import cn.edu.tsinghua.iotdb.service.IoTDB;
import cn.edu.tsinghua.iotdb.utils.EnvironmentUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.sql.*;
import java.util.*;

/**
 * @author Liliang
 */

public class MonitorTest {
    private TsfileDBConfig tsdbconfig = TsfileDBDescriptor.getInstance().getConfig();

    private IoTDB deamon;
    private StatMonitor statMonitor;

    private List<String> storagegroupList = new ArrayList<String>() {
        {
            add("root.d1");
            add("root.d2");
        }
    };
    private List<String> timeseiresList = new ArrayList<String>() {
        {
            add("root.d1.s1");
            add("root.d1.s2");
            add("root.d2.s1");
        }
    };
    private String STATISTIC_NAME = "TOTAL_POINTS_SUCCESS";

    private final long START_TIME = 1;
    private final long TIME_LENGTH = 1000;
    private final long TIME_SHIFT = 100;

    @Before
    public void setUp() throws Exception {
        EnvironmentUtils.closeMemControl();
        tsdbconfig.enableStatMonitor = true;
        tsdbconfig.backLoopPeriodSec = 1;
        deamon = IoTDB.getInstance();
        deamon.active();
        statMonitor = StatMonitor.getInstance();
        EnvironmentUtils.envSetUp();

        Thread.sleep(2000);
    }

    @After
    public void tearDown() throws Exception {
        tsdbconfig.enableStatMonitor = false;
        deamon.stop();
        Thread.sleep(2000);
        EnvironmentUtils.cleanEnv();
    }

    private void init() throws SQLException, ClassNotFoundException {
        Class.forName(TsfileJDBCConfig.JDBC_DRIVER_NAME);
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:tsfile://127.0.0.1:6667/", "root", "root");
            Statement statement = connection.createStatement();

            for (String storagegroup : storagegroupList) {
                statement.execute("SET STORAGE GROUP TO " + storagegroup);
            }
            for (String timeseries : timeseiresList) {
                statement.execute("CREATE TIMESERIES " + timeseries + " WITH DATATYPE=INT32, ENCODING=TS_2DIFF");
            }
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    private int getStatisticWidth(){
        int count = 0;
        Set<String> paths = new HashSet<>();
        for(String path : storagegroupList){
            while (path.contains(MonitorConstants.STORAGEGROUP_PATH_SEPERATOR)){
                if(!paths.contains(path)){
                    count++;
                    paths.add(path);
                }
                path = path.substring(0, path.lastIndexOf(MonitorConstants.STORAGEGROUP_PATH_SEPERATOR));
            }
            if(!paths.contains(path)){
                count++;
                paths.add(path);
            }
        }
        return count * 4 + 1;
    }

    private void insertDataByTimestamp(long start, long end) throws SQLException, ClassNotFoundException {
        Class.forName(TsfileJDBCConfig.JDBC_DRIVER_NAME);
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:tsfile://127.0.0.1:6667/", "root", "root");
            Statement statement = connection.createStatement();

            for (String path : timeseiresList) {
                for (long i = start; i < end; i++) {
                    int index = path.lastIndexOf(MonitorConstants.STORAGEGROUP_PATH_SEPERATOR);
                    String deltaobjectID = path.substring(0, index);
                    String measurementID = path.substring(index + 1);
                    statement.execute("INSERT INTO " + deltaobjectID + " (timestamp, " + measurementID + ") VALUES (" + i + ", 1)");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    private void flush() throws SQLException, ClassNotFoundException {
        Class.forName(TsfileJDBCConfig.JDBC_DRIVER_NAME);
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:tsfile://127.0.0.1:6667/", "root", "root");
            Statement statement = connection.createStatement();
            statement.execute("flush");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    private void merge() throws SQLException, ClassNotFoundException {
        Class.forName(TsfileJDBCConfig.JDBC_DRIVER_NAME);
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:tsfile://127.0.0.1:6667/", "root", "root");
            Statement statement = connection.createStatement();
            statement.execute("merge");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    private Map<String, Long> getStatisticInDB() throws ClassNotFoundException, SQLException, InterruptedException {
        Map<String, Long> res = new HashMap<>();

        Class.forName(TsfileJDBCConfig.JDBC_DRIVER_NAME);
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:tsfile://127.0.0.1:6667/", "root", "root");
            Statement statement = connection.createStatement();

            Thread.sleep(tsdbconfig.backLoopPeriodSec);

            boolean hasResultSet = statement.execute("select * from root.stats");
            Assert.assertTrue(hasResultSet);
            ResultSet resultSet = statement.getResultSet();
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            int columnsize = resultSetMetaData.getColumnCount();
            Assert.assertEquals(getStatisticWidth(), columnsize);
            while (resultSet.next()) {
                for(int i = 1;i <= columnsize;i++){
                    String path = resultSetMetaData.getColumnName(i);
                    String value = resultSet.getString(i);
                    long real_v = 0;
                    if(!value.equals("null"))real_v = Long.valueOf(value);
                    res.put(path, real_v);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.close();
            }
            return res;
        }
    }

    @Test
    public void test() {
        try {
            init();

            insertDataByTimestamp(START_TIME, START_TIME + TIME_LENGTH);
            flush();
            Map<String, Long> statistics = getStatisticInDB();
            Assert.assertEquals(TIME_LENGTH * 2, (long)statistics.get(MonitorConstants.convertStorageGroupPathToStatisticPath(storagegroupList.get(0)) + "." + STATISTIC_NAME));
            Assert.assertEquals(TIME_LENGTH * 1, (long)statistics.get(MonitorConstants.convertStorageGroupPathToStatisticPath(storagegroupList.get(1)) + "." + STATISTIC_NAME));
            Assert.assertEquals(TIME_LENGTH * 3, (long)statistics.get(MonitorConstants.convertStorageGroupPathToStatisticPath("root") + "." + STATISTIC_NAME));

            insertDataByTimestamp(START_TIME, TIME_LENGTH / 2);
            merge();
            statMonitor.dealWithAllEvents();
            statistics = getStatisticInDB();
            Assert.assertEquals(TIME_LENGTH * 2, (long)statistics.get(MonitorConstants.convertStorageGroupPathToStatisticPath(storagegroupList.get(0)) + "." + STATISTIC_NAME));
            Assert.assertEquals(TIME_LENGTH * 1, (long)statistics.get(MonitorConstants.convertStorageGroupPathToStatisticPath(storagegroupList.get(1)) + "." + STATISTIC_NAME));
            Assert.assertEquals(TIME_LENGTH * 3, (long)statistics.get(MonitorConstants.convertStorageGroupPathToStatisticPath("root") + "." + STATISTIC_NAME));

            insertDataByTimestamp(START_TIME, TIME_LENGTH / 2);
            insertDataByTimestamp(START_TIME, TIME_LENGTH / 2);
            merge();
            statMonitor.dealWithAllEvents();
            statistics = getStatisticInDB();
            Assert.assertEquals(TIME_LENGTH * 2, (long)statistics.get(MonitorConstants.convertStorageGroupPathToStatisticPath(storagegroupList.get(0)) + "." + STATISTIC_NAME));
            Assert.assertEquals(TIME_LENGTH * 1, (long)statistics.get(MonitorConstants.convertStorageGroupPathToStatisticPath(storagegroupList.get(1)) + "." + STATISTIC_NAME));
            Assert.assertEquals(TIME_LENGTH * 3, (long)statistics.get(MonitorConstants.convertStorageGroupPathToStatisticPath("root") + "." + STATISTIC_NAME));

            insertDataByTimestamp(START_TIME + TIME_LENGTH + TIME_SHIFT, START_TIME + TIME_LENGTH + TIME_SHIFT + TIME_LENGTH );
            flush();
            merge();
            statMonitor.dealWithAllEvents();
            statistics = getStatisticInDB();
            Assert.assertEquals(TIME_LENGTH * 4, (long)statistics.get(MonitorConstants.convertStorageGroupPathToStatisticPath(storagegroupList.get(0)) + "." + STATISTIC_NAME));
            Assert.assertEquals(TIME_LENGTH * 2, (long)statistics.get(MonitorConstants.convertStorageGroupPathToStatisticPath(storagegroupList.get(1)) + "." + STATISTIC_NAME));
            Assert.assertEquals(TIME_LENGTH * 6, (long)statistics.get(MonitorConstants.convertStorageGroupPathToStatisticPath("root") + "." + STATISTIC_NAME));
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}