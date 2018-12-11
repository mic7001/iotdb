package cn.edu.tsinghua.tsfile.read.filter.operator;

import cn.edu.tsinghua.tsfile.read.filter.DigestForFilter;
import cn.edu.tsinghua.tsfile.read.filter.basic.BinaryFilter;
import cn.edu.tsinghua.tsfile.read.filter.basic.Filter;

import java.io.Serializable;

/**
 * Either of the left and right operators of AndExpression must satisfy the condition.
 */
public class OrFilter extends BinaryFilter implements Serializable {

    private static final long serialVersionUID = -968055896528472694L;

    public OrFilter(Filter left, Filter right) {
        super(left, right);
    }

    @Override
    public String toString() {
        return "(" + left + " || " + right + ")";
    }


    @Override
    public boolean satisfy(DigestForFilter digest) {
        return left.satisfy(digest) || right.satisfy(digest);
    }

    @Override
    public boolean satisfy(long time, Object value) {
        return left.satisfy(time, value) || right.satisfy(time, value);
    }

}