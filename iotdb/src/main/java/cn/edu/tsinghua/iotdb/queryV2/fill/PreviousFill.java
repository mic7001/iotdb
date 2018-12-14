package cn.edu.tsinghua.iotdb.queryV2.fill;


import cn.edu.tsinghua.iotdb.exception.PathErrorException;
import cn.edu.tsinghua.iotdb.exception.ProcessorException;
import cn.edu.tsinghua.tsfile.file.metadata.enums.TSDataType;
import cn.edu.tsinghua.tsfile.read.common.BatchData;
import cn.edu.tsinghua.tsfile.read.common.Path;

import java.io.IOException;


public class PreviousFill extends IFill {

    private long beforeRange;

    private Path path;

    private BatchData result;

    public PreviousFill(Path path, TSDataType dataType, long queryTime, long beforeRange) {
        super(dataType, queryTime);
        this.path = path;
        this.beforeRange = beforeRange;
        result = new BatchData(dataType, true, true);
    }

    public PreviousFill(long beforeRange) {
        this.beforeRange = beforeRange;
    }

    @Override
    public IFill copy(Path path) {
        return new PreviousFill(path, dataType, queryTime, beforeRange);
    }

    public long getBeforeRange() {
        return beforeRange;
    }

    @Override
    public BatchData getFillResult() throws ProcessorException, IOException, PathErrorException {
        long beforeTime;
        if (beforeRange == -1) {
            beforeTime = 0;
        } else {
            beforeTime = queryTime - beforeRange;
        }

//        SingleSeriesFilterExpression leftFilter = gtEq(timeFilterSeries(), beforeTime, true);
//        SingleSeriesFilterExpression rightFilter = ltEq(timeFilterSeries(), queryTime, true);
//        SingleSeriesFilterExpression fillTimeFilter = (SingleSeriesFilterExpression) and(leftFilter, rightFilter);
//
//        String deltaObjectId = path.getDevice();
//        String measurementId = path.getMeasurement();
//        String recordReaderPrefix = ReadCachePrefix.addQueryPrefix("PreviousFill", -1);
//
//        FillRecordReader recordReader = (FillRecordReader) RecordReaderFactory.getInstance().getRecordReader(deltaObjectId, measurementId,
//                fillTimeFilter, null, null, recordReaderPrefix, ReaderType.FILL);
//
//        recordReader.getPreviousFillResult(result, fillTimeFilter, beforeTime, queryTime);

        return result;
    }
}
