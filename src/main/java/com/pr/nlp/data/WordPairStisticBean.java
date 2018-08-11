package com.pr.nlp.data;

public class WordPairStisticBean {

    public String firstWord;
    public String sencondWord;

    public double frequent;
    public double frequency;

    public WordPairStisticBean(String firstWord, String sencondWord, double frequent, double frequency) {
        this.firstWord = firstWord;
        this.sencondWord = sencondWord;
        this.frequent = frequent;
        this.frequency = frequency;
    }

    public WordPairStisticBean(String firstWord, String sencondWord) {
        this(firstWord, sencondWord, 0, 0);
    }

    public WordPairStisticBean(String firstWord, String sencondWord, double frequent) {
        this(firstWord, sencondWord, frequent, 0);
    }

    public String getFirstWord() {
        return firstWord;
    }

    public String getSencondWord() {
        return sencondWord;
    }

    public double getFrequent() {
        return frequent;
    }

    public double getFrequency() {
        return frequency;
    }

    public void setFirstWord(String firstWord) {
        this.firstWord = firstWord;
    }

    public void setSencondWord(String sencondWord) {
        this.sencondWord = sencondWord;
    }

    public void setFrequent(long frequent) {
        this.frequent = frequent;
    }

    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }

    public void addOneFrequent() {
        this.addFrequent(1);
    }

    public void addFrequent(long num) {
        this.frequent += num;
    }


    public String show() {
        return "WordPairStisticBean{" +
                "firstWord='" + firstWord + '\'' +
                ", sencondWord='" + sencondWord + '\'' +
                ", frequent=" + frequent +
                ", frequency=" + frequency +
                '}';
    }

    @Override
    public String toString() {
        return firstWord + "," + sencondWord + "," + frequent + "," + frequency;
    }
}
