package com.pr.nlp.data;

public class LMStatisticWordBean {

    private String firstWord;
    private String secondWord;
    private double conditionalProb;

    public LMStatisticWordBean(String firstWord, String secondWord, double conditionalProb) {
        this.firstWord = firstWord;
        this.secondWord = secondWord;
        this.conditionalProb = conditionalProb;
    }

    public String getFirstWord() {
        return firstWord;
    }

    public String getSecondWord() {
        return secondWord;
    }

    public double getConditionalProb() {
        return conditionalProb;
    }

    public void setFirstWord(String firstWord) {
        this.firstWord = firstWord;
    }

    public void setSecondWord(String secondWord) {
        this.secondWord = secondWord;
    }

    public void setConditionalProb(double conditionalProb) {
        this.conditionalProb = conditionalProb;
    }

    @Override
    public String toString() {
        return firstWord + "," + secondWord + "," + conditionalProb;
    }

    public String show() {
        return "LMStatisticWordBean{" +
                "firstWord='" + firstWord + '\'' +
                ", secondWord='" + secondWord + '\'' +
                ", conditionalProb=" + conditionalProb +
                '}';
    }
}
