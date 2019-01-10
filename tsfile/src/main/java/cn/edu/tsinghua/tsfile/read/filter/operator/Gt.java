package cn.edu.tsinghua.tsfile.read.filter.operator;

import cn.edu.tsinghua.tsfile.read.filter.DigestForFilter;
import cn.edu.tsinghua.tsfile.read.filter.basic.UnaryFilter;
import cn.edu.tsinghua.tsfile.read.filter.factory.FilterType;

/**
 * Greater than
 *
 * @param <T> comparable data type
 */
public class Gt<T extends Comparable<T>> extends UnaryFilter<T> {

    private static final long serialVersionUID = -2088181659871608986L;

    public Gt(T value, FilterType filterType) {
        super(value, filterType);
    }

    @Override
    public boolean satisfy(DigestForFilter digest) {
        if (filterType == FilterType.TIME_FILTER) {
            return ((Long) value) < digest.getMaxTime();
        } else {
            return value.compareTo(digest.getMaxValue()) < 0;
        }
    }

    @Override
    public boolean satisfy(long time, Object value) {
        Object v = filterType == FilterType.TIME_FILTER ? time : value;
        return this.value.compareTo((T) v) < 0;
    }

    @Override
    public boolean satisfyStartEndTime(long startTime, long endTime) {
        if (filterType == FilterType.TIME_FILTER) {
            long time = (Long) value;
            if (time >= endTime) {
                return false;
            }
            return true;
        } else {
            return true;
        }
    }

    @Override
    public String toString() {
        return getFilterType() + " > " + value;
    }
}