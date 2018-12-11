package cn.edu.tsinghua.iotdb.qp.logical.crud;

import java.util.ArrayList;
import java.util.List;

import cn.edu.tsinghua.iotdb.exception.qp.LogicalOperatorException;
import cn.edu.tsinghua.iotdb.qp.logical.Operator;
import cn.edu.tsinghua.tsfile.read.common.Path;


/**
 * this class maintains information from {@code FROM} clause
 */
public class FromOperator extends Operator {
    private List<Path> prefixList;

    public FromOperator(int tokenIntType) {
        super(tokenIntType);
        operatorType = OperatorType.FROM;
        prefixList = new ArrayList<>();
    }

    public void addPrefixTablePath(Path prefixPath) throws LogicalOperatorException {
        prefixList.add(prefixPath);
    }

    public List<Path> getPrefixPaths() {
        return prefixList;
    }

}
