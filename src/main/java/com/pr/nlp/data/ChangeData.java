package com.pr.nlp.data;

import java.io.Serializable;
import java.util.Objects;

public class ChangeData implements Serializable {

    private int startInd;
    private int endInd;
    private String changeStr;

    public ChangeData(int startInd, int endInd, String changeStr) {
        this.startInd = startInd;
        this.endInd = endInd;
        this.changeStr = changeStr;
    }

    public int getStartInd() {
        return startInd;
    }

    public int getEndInd() {
        return endInd;
    }

    public String getChangeStr() {
        return changeStr;
    }

    public void setStartInd(int startInd) {
        this.startInd = startInd;
    }

    public void setEndInd(int endInd) {
        this.endInd = endInd;
    }

    public void setChangeStr(String changeStr) {
        this.changeStr = changeStr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChangeData that = (ChangeData) o;
        return startInd == that.startInd &&
                endInd == that.endInd &&
                Objects.equals(changeStr, that.changeStr);
    }

    @Override
    public int hashCode() {

        return Objects.hash(startInd, endInd, changeStr);
    }
}
