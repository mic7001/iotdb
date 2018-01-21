package cn.edu.tsinghua.iotdb.queryV2.reader;

import cn.edu.tsinghua.iotdb.queryV2.engine.overflow.OverflowUpdateOperation;
import cn.edu.tsinghua.iotdb.queryV2.engine.overflow.OverflowUpdateOperationReader;
import cn.edu.tsinghua.iotdb.queryV2.engine.reader.series.SeriesWithUpdateOpReader;
import cn.edu.tsinghua.tsfile.timeseries.readV2.datatype.TimeValuePair;
import cn.edu.tsinghua.tsfile.timeseries.readV2.datatype.TsPrimitiveType;
import cn.edu.tsinghua.tsfile.timeseries.readV2.reader.SeriesReader;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangjinrui on 2018/1/19.
 */
public class SeriesWithUpdateOpReaderTest {

    @Test
    public void test1() throws IOException {
        OverflowUpdateOperation[] updateOps = new OverflowUpdateOperation[]{
                new OverflowUpdateOperation(0L, 100L, new TsPrimitiveType.TsInt(100)),
                new OverflowUpdateOperation(150L, 200L, new TsPrimitiveType.TsInt(200)),
                new OverflowUpdateOperation(300L, 400L, new TsPrimitiveType.TsInt(300))
        };
        testRead(genSeriesReader(1000), updateOps);
    }

    @Test
    public void test2() throws IOException {
        OverflowUpdateOperation[] updateOps = new OverflowUpdateOperation[]{
                new OverflowUpdateOperation(0L, 1L, new TsPrimitiveType.TsInt(100)),
        };
        testRead(genSeriesReader(1000), updateOps);
    }

    @Test
    public void test3() throws IOException {
        OverflowUpdateOperation[] updateOps = new OverflowUpdateOperation[]{
                new OverflowUpdateOperation(10000L, 20000L, new TsPrimitiveType.TsInt(100)),
        };
        testRead(genSeriesReader(1000), updateOps);
    }

    @Test
    public void test4() throws IOException {
        OverflowUpdateOperation[] updateOps = new OverflowUpdateOperation[]{
                new OverflowUpdateOperation(-1L, 0L, new TsPrimitiveType.TsInt(100)),
        };
        testRead(genSeriesReader(1000), updateOps);
    }

    private SeriesMergeSortReaderTest.FakedSeriesReader genSeriesReader(int size) {
        int count = size;
        long[] timestamps = new long[count];
        for (int i = 0; i < count; i++) {
            timestamps[i] = i;
        }
        return new SeriesMergeSortReaderTest.FakedSeriesReader(timestamps);
    }

    private void testRead(SeriesReader seriesReader, OverflowUpdateOperation[] updateOperations) throws IOException {
        FakedOverflowUpdateOperationReader updateOpReader = new FakedOverflowUpdateOperationReader(updateOperations);
        SeriesWithUpdateOpReader seriesWithUpdateOpReader = new SeriesWithUpdateOpReader(seriesReader, updateOpReader);
        check(updateOpReader, seriesWithUpdateOpReader);
    }

    private void check(FakedOverflowUpdateOperationReader opReader, SeriesWithUpdateOpReader seriesWithUpdateOpReader) throws IOException {
        opReader.reset();
        List<OverflowUpdateOperation> overflowUpdateOperations = new ArrayList<>();
        while (opReader.hasNext()) {
            overflowUpdateOperations.add(opReader.next());
        }
        opReader.reset();
        while (seriesWithUpdateOpReader.hasNext()) {
            TimeValuePair timeValuePair = seriesWithUpdateOpReader.next();
            boolean satisfied = false;
            for (int i = 0; i < overflowUpdateOperations.size(); i++) {
                if (satisfied(overflowUpdateOperations.get(i), timeValuePair.getTimestamp())) {
                    Assert.assertEquals(overflowUpdateOperations.get(i).getValue(), timeValuePair.getValue());
                    satisfied = true;
                }
            }
            if (!satisfied) {
                Assert.assertEquals(new TsPrimitiveType.TsLong(1), timeValuePair.getValue());
            }
        }
    }

    private boolean satisfied(OverflowUpdateOperation op, long timestamp) {
        if (op.getLeftBound() <= timestamp && timestamp <= op.getRightBound()) {
            return true;
        }
        return false;
    }

    public static class FakedOverflowUpdateOperationReader implements OverflowUpdateOperationReader {

        private OverflowUpdateOperation[] updataOps;
        private int index = 0;

        public FakedOverflowUpdateOperationReader(OverflowUpdateOperation[] updataOps) {
            this.updataOps = updataOps;
        }

        @Override
        public boolean hasNext() {
            return index < updataOps.length;
        }

        @Override
        public OverflowUpdateOperation next() {
            index++;
            return updataOps[index - 1];
        }

        @Override
        public void close() throws IOException {

        }

        public void reset() {
            index = 0;
        }
    }
}