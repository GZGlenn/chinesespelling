package com.pr.nlp.data;

public class FeatureData {

    private double lmfeat;
    private double pmifeat;
    private double wordNum;

    public FeatureData(double lmfeat, double pmifeat, double wordNum) {
        this.lmfeat = lmfeat;
        this.pmifeat = pmifeat;
        this.wordNum = wordNum;
    }

    public double getLmfeat() {
        return lmfeat;
    }

    public double getPmifeat() {
        return pmifeat;
    }

    public double getWordNum() {
        return wordNum;
    }

    public void setLmfeat(double lmfeat) {
        this.lmfeat = lmfeat;
    }

    public void setPmifeat(double pmifeat) {
        this.pmifeat = pmifeat;
    }

    public void setWordNum(double wordNum) {
        this.wordNum = wordNum;
    }
}
