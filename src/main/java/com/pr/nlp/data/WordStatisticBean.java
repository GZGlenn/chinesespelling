package com.pr.nlp.data;

public class WordStatisticBean {
    public String word;
    public double frequent;
    public double frequency;

    public WordStatisticBean(String word) {
        this(word, 0,0);
    }

    public WordStatisticBean(String word, double frequent) {
        this(word, frequent, 0);
    }

    public WordStatisticBean(String word, double frequent, double frequency) {
        this.word = word;
        this.frequent = frequent;
        this.frequency = frequency;
    }

    public String getWord() {
        return word;
    }

    public double getFrequent() {
        return frequent;
    }

    public double getFrequency() {
        return frequency;
    }

    public void setWord(String word) {
        this.word = word;
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

    @Override
    public String toString() {
        return word + "," + frequent + "," + frequency;
    }

    public String show() {
        return "WordStatisticBean{" +
                "word='" + word + '\'' +
                ", frequent=" + frequent +
                ", frequency=" + frequency +
                '}';
    }
}
