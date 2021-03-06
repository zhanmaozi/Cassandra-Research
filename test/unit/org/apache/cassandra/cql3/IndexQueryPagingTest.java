package org.apache.cassandra.cql3;

import org.junit.Test;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;

import static org.junit.Assert.assertEquals;

public class IndexQueryPagingTest extends CQLTester
{
    /*
     * Some simple tests to verify the behaviour of paging during
     * 2i queries. We only use a single index type (CompositesIndexOnRegular)
     * as the code we want to exercise here is in their abstract
     * base class.
     */

    @Test
    public void pagingOnRegularColumn() throws Throwable
    {
        createTable("CREATE TABLE %s (" +
                    " k1 int," +
                    " v1 int," +
                    "PRIMARY KEY (k1))");
        createIndex("CREATE INDEX ON %s(v1)");

        int rowCount = 3;
        for (int i=0; i<rowCount; i++)
            execute("INSERT INTO %s (k1, v1) VALUES (?, ?)", i, 0);

        executePagingQuery("SELECT * FROM %s WHERE v1=0", rowCount);
    }

    @Test
    public void pagingOnRegularColumnWithPartitionRestriction() throws Throwable
    {
        createTable("CREATE TABLE %s (" +
                    " k1 int," +
                    " c1 int," +
                    " v1 int," +
                    "PRIMARY KEY (k1, c1))");
        createIndex("CREATE INDEX ON %s(v1)");

        int partitions = 3;
        int rowCount = 3;
        for (int i=0; i<partitions; i++)
            for (int j=0; j<rowCount; j++)
                execute("INSERT INTO %s (k1, c1, v1) VALUES (?, ?, ?)", i, j, 0);

        executePagingQuery("SELECT * FROM %s WHERE k1=0 AND v1=0", rowCount);
    }

    @Test
    public void pagingOnRegularColumnWithClusteringRestrictions() throws Throwable
    {
        createTable("CREATE TABLE %s (" +
                    " k1 int," +
                    " c1 int," +
                    " v1 int," +
                    "PRIMARY KEY (k1, c1))");
        createIndex("CREATE INDEX ON %s(v1)");

        int partitions = 3;
        int rowCount = 3;
        for (int i=0; i<partitions; i++)
            for (int j=0; j<rowCount; j++)
                execute("INSERT INTO %s (k1, c1, v1) VALUES (?, ?, ?)", i, j, 0);

        executePagingQuery("SELECT * FROM %s WHERE k1=0 AND c1>=0 AND c1<=3 AND v1=0", rowCount);
    }

    private void executePagingQuery(String cql, int rowCount)
    {
        // Execute an index query which should return all rows,
        // setting the fetch size < than the row count. Assert
        // that all rows are returned, so we know that paging
        // of the results was involved.
        Session session = sessionNet();
        Statement stmt = session.newSimpleStatement(String.format(cql, KEYSPACE + "." + currentTable()));
        stmt.setFetchSize(rowCount - 1);
        assertEquals(rowCount, session.execute(stmt).all().size());
    }
}
