package cn.edu.tsinghua.iotdb.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangjinrui on 2018/1/25.
 */
public class PrimitiveArrayList {

    private static final int MAX_SIZE_OF_ONE_ARRAY = 512;
    private static final int INITIAL_SIZE = 1;

    private Class clazz;
    private List<Object> values;
    private List<long[]> timestamps;

    private int length; // Total size of all objects of current ArrayList
    private int currentIndex; //current index of array
    private int currentArrayIndex; //current index of element in current array
    private int currentArraySize; //size of current array

    public PrimitiveArrayList(Class clazz) {
        this.clazz = clazz;
        values = new ArrayList<>();
        timestamps = new ArrayList<>();
        values.add(Array.newInstance(clazz, INITIAL_SIZE));
        timestamps.add(new long[INITIAL_SIZE]);
        length = 0;

        currentIndex = 0;
        currentArraySize = INITIAL_SIZE;
        currentArrayIndex = -1;
    }

    private void capacity(int aimSize) {
        if (currentArraySize < aimSize) {
            if (currentArraySize < MAX_SIZE_OF_ONE_ARRAY) {
                //expand current Array
                int newCapacity = Math.min(MAX_SIZE_OF_ONE_ARRAY, currentArraySize * 2);
                values.set(currentIndex, expandArray(values.get(currentIndex), currentArraySize, newCapacity));
                timestamps.set(currentIndex, (long[]) expandArray(timestamps.get(currentIndex), currentArraySize, newCapacity));
                currentArraySize = newCapacity;
            } else {
                //add a new Array to the list;
                values.add(Array.newInstance(clazz, INITIAL_SIZE));
                timestamps.add(new long[INITIAL_SIZE]);
                currentIndex++;
                currentArraySize = INITIAL_SIZE;
                currentArrayIndex = -1;
            }
        }
    }

    private Object expandArray(Object array, int preLentgh, int aimLength) {
        Class clazz = array.getClass().getComponentType();
        Object newArray = Array.newInstance(clazz, aimLength);
        System.arraycopy(array, 0, newArray, 0, preLentgh);
        return newArray;
    }

    public void putTimestamp(long timestamp, Object value) {
        capacity(currentArrayIndex + 1 + 1);
        currentArrayIndex++;
        timestamps.get(currentIndex)[currentArrayIndex] = timestamp;
        Array.set(values.get(currentIndex), currentArrayIndex, value);
        length++;
    }

    public long getTimestamp(int index) {
        checkIndex(index);
        return timestamps.get(index / MAX_SIZE_OF_ONE_ARRAY)[index % MAX_SIZE_OF_ONE_ARRAY];
    }

    public Object getValue(int index) {
        checkIndex(index);
        return Array.get(values.get(index / MAX_SIZE_OF_ONE_ARRAY), index % MAX_SIZE_OF_ONE_ARRAY);
    }

    private void checkIndex(int index) {
        if (index < 0) {
            throw new NegativeArraySizeException("negetive array index:" + index);
        }
        if (index >= length) {
            throw new ArrayIndexOutOfBoundsException("index: " + index);
        }
    }

    public int size() {
        return length;
    }
//    public void putBooleanTimeValuePair(long timestamps, boolean values){
//        throw new UnsupportedOperationException("putBooleanTimeValuePair not supported for current PrimitiveArrayList");
//    }
//
//    public void putIntTimeValuePair(long timestamps, int values){
//        throw new UnsupportedOperationException("putLongTimeValuePair not supported for current PrimitiveArrayList");
//    }
//
//    public void putLongTimeValuePair(long timestamps, long values){
//        throw new UnsupportedOperationException("putLongTimeValuePair not supported for current PrimitiveArrayList");
//    }
//
//    public void putFloatTimeValuePair(long timestamps, float values){
//        throw new UnsupportedOperationException("putFloatTimeValuePair not supported for current PrimitiveArrayList");
//    }
//
//    public void putDoubleTimeValuePair(long timestamps, double values){
//        throw new UnsupportedOperationException("putDoubleTimeValuePair not supported for current PrimitiveArrayList");
//    }
//
//    public void putBinaryTimeValuePair(long timestamps, Binary values){
//        throw new UnsupportedOperationException("putBinaryTimeValuePair not supported for current PrimitiveArrayList");
//    }


}