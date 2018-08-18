//package com.pr.nlp.data;
//
//import javafx.util.Pair;
//
//import java.io.Serializable;
//import java.util.ArrayList;
//
//public class CandidateData implements Serializable {
//
//    private String content;
//    private int modifiedSIdx;
//    private int modifiedEIdx;
//    private ArrayList<Pair<Integer, Integer>> modifiedWordList;
//
//    public CandidateData(String content) {
//        this.content = content;
//    }
//
//    public CandidateData(String content, int modifiedSIdx, int modifiedEIdx, ArrayList<Pair<Integer, Integer>> modifiedWordList) {
//        this.content = content;
//        this.modifiedSIdx = modifiedSIdx;
//        this.modifiedEIdx = modifiedEIdx;
//        this.modifiedWordList = modifiedWordList;
//    }
//
//    public String getContent() {
//        return content;
//    }
//
//    public int getModifiedSIdx() {
//        return modifiedSIdx;
//    }
//
//    public int getModifiedEIdx() {
//        return modifiedEIdx;
//    }
//
//    public ArrayList<Pair<Integer, Integer>> getModifiedWordList() {
//        return modifiedWordList;
//    }
//
//    public void setContent(String content) {
//        this.content = content;
//    }
//
//    public void setModifiedSIdx(int modifiedSIdx) {
//        this.modifiedSIdx = modifiedSIdx;
//    }
//
//    public void setModifiedEIdx(int modifiedEIdx) {
//        this.modifiedEIdx = modifiedEIdx;
//    }
//
//    public void setModifiedWordList(ArrayList<Pair<Integer, Integer>> modifiedWordList) {
//        this.modifiedWordList = modifiedWordList;
//    }
//}
