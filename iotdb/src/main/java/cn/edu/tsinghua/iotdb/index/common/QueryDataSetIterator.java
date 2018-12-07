package cn.edu.tsinghua.iotdb.index.common;

import cn.edu.tsinghua.tsfile.common.exception.ProcessorException;
import cn.edu.tsinghua.tsfile.common.utils.Pair;
import cn.edu.tsinghua.tsfile.timeseries.filter.expression.QueryFilter;
import cn.edu.tsinghua.tsfile.timeseries.filter.factory.FilterFactory;
import cn.edu.tsinghua.tsfile.timeseries.filter.expression.impl.SeriesFilter;
import cn.edu.tsinghua.tsfile.timeseries.filter.definition.filterseries.FilterSeries;
import cn.edu.tsinghua.tsfile.timeseries.filter.definition.operators.And;
import cn.edu.tsinghua.tsfile.timeseries.filter.definition.operators.GtEq;
import cn.edu.tsinghua.tsfile.timeseries.filter.definition.operators.LtEq;
import cn.edu.tsinghua.tsfile.timeseries.read.common.Path;
import cn.edu.tsinghua.tsfile.timeseries.read.query.OnePassQueryDataSet;
import cn.edu.tsinghua.tsfile.timeseries.read.support.RowRecord;
import cn.edu.tsinghua.iotdb.conf.TsfileDBDescriptor;
import cn.edu.tsinghua.iotdb.exception.PathErrorException;
import cn.edu.tsinghua.iotdb.query.engine.OverflowQueryEngine;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * This is an iterator wrap class used for multi-batch fetching query data set in query process.
 *
 * @author CGF, Jiaye Wu
 */
public class QueryDataSetIterator {

    private OverflowQueryEngine overflowQueryEngine;

    private OnePassQueryDataSet queryDataSet;

    private List<Path> pathList;

    private QueryFilter QueryFilter;

    private int readToken;

    public QueryDataSetIterator(OverflowQueryEngine overflowQueryEngine, Path path, List<Pair<Long, Long>> timeIntervals,
                                int readToken) throws ProcessorException, PathErrorException, IOException {
        pathList = Collections.singletonList(path);

        for (int i = 0; i < timeIntervals.size(); i++) {
            Pair<Long, Long> pair = timeIntervals.get(i);
            FilterSeries<Long> timeSeries = FilterFactory.timeFilterSeries();
            GtEq gtEq = FilterFactory.gtEq(timeSeries, pair.left, true);
            LtEq ltEq = FilterFactory.ltEq(timeSeries, pair.right, true);
            if (i == 0) {
                QueryFilter = FilterFactory.and(gtEq, ltEq);
            } else {
                And tmpAnd = (And) FilterFactory.and(gtEq, ltEq);
                QueryFilter = FilterFactory.or(QueryFilter, tmpAnd);
            }
        }

        this.overflowQueryEngine = overflowQueryEngine;
        this.readToken = readToken;
        this.queryDataSet = this.overflowQueryEngine.query(0, pathList, QueryFilter, null, null,
                null, TsfileDBDescriptor.getInstance().getConfig().fetchSize, readToken);
//        formNumber++;
    }

    public boolean hasNext() throws IOException, PathErrorException, ProcessorException {
        if (queryDataSet.hasNextRecord()) {
            return true;
        } else {
            queryDataSet = overflowQueryEngine.query(0, pathList, QueryFilter, null, null,
                    queryDataSet, TsfileDBDescriptor.getInstance().getConfig().fetchSize, readToken);
//            formNumber++;
            return queryDataSet.hasNextRecord();
        }
    }

    public RowRecord getRowRecord() {
        return queryDataSet.getCurrentRecord();
    }

}
