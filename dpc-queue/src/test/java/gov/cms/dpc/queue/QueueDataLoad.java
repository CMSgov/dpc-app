package gov.cms.dpc.queue;

import com.google.common.base.Stopwatch;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QueueDataLoad {

    private static final String DB_URL = "jdbc:postgresql://dpc-hyyc-queuev2-performance-test.clwhxzagcmjv.us-east-1.rds.amazonaws.com:5432/load_test";
    private static final Integer ITEMS = 5000;
    private static final Integer THREADS = 10;


    public static void main(String... args) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(THREADS);

        for ( int i = 0; i < THREADS; i++ ) {
            executorService.execute(new QueueRunner());
        }

        executorService.shutdown();
    }

    public static void runQueueTest() throws Exception {
        Stopwatch overallTimer = Stopwatch.createStarted();

        // Create the DB connection
        Connection connection = createConnection();

        try {
            for (int i = 0; i < ITEMS; i++) {
                Stopwatch itemTimer = Stopwatch.createStarted();
                System.out.println("Start Item: " + i);

                queueItem(connection);

                System.out.println("End Item: " + i + " (" + itemTimer.stop() + ")");
            }
        } finally {
            connection.close();
        }

        System.out.println("Done processing " + ITEMS + " items (" + overallTimer.stop() + ")");
    }

    public static Connection createConnection() throws Exception {
        Class.forName("org.postgresql.Driver");
        Connection connection = DriverManager.getConnection(DB_URL, "postgres", "dpc-safe");
        connection.setAutoCommit(false);
        return connection;
    }

    public static void queueItem(Connection connection) {

        try {
            Statement stmt = connection.createStatement();

            String sql = "SELECT job_id FROM queue.queue_single_table WHERE status = 0 ORDER by created FOR UPDATE SKIP LOCKED LIMIT 1";
            ResultSet set = stmt.executeQuery(sql);

            String jobId = null;
            while ( set.next() ) {
                jobId = set.getString(1);
            }

            if ( jobId == null ) {
                return;
            }

            String finish = "UPDATE queue.queue_single_table SET status = 1 WHERE job_id = '" + jobId + "'";
            stmt.executeUpdate(finish);

            connection.commit();
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();

            try {
                connection.rollback();
            } catch ( Exception ex ) {
                ex.printStackTrace();
            }
        }
    }

    public static void queueMultiItem(Connection connection) {
        try {
            Statement stmt = connection.createStatement();

            String sql = "SELECT job_id FROM queue.queue_multi_table ORDER by created FOR UPDATE SKIP LOCKED LIMIT 1";
            ResultSet set = stmt.executeQuery(sql);

            String jobId = null;
            while ( set.next() ) {
                jobId = set.getString(1);
            }

            if ( jobId == null ) {
                return;
            }

            String finish = "DELETE FROM queue.queue_multi_table WHERE job_id = '" + jobId + "'";
            stmt.executeUpdate(finish);

            connection.commit();
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();

            try {
                connection.rollback();
            } catch ( Exception ex ) {
                ex.printStackTrace();
            }
        }
    }

}

class QueueRunner implements Runnable {

    @Override
    public void run() {
        try {
            QueueDataLoad.runQueueTest();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}