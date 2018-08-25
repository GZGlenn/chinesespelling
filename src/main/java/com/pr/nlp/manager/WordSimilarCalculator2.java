package com.pr.nlp.manager;

import com.hankcs.hanlp.HanLP;
import com.pr.nlp.util.FileUtil;

import java.util.*;

public class WordSimilarCalculator2 {

    private String root_path = "/home/public/code/chinese_spelling/SimilarWordCalculator/src/main/resources/";
    private final String shape_similar_file_name = "shape_similar.txt";
    private final String pronounce_similar_file_name = "pronounce_similar.txt";
    private final String pinyin_word_similar_file_name = "pinyin_similar_word.txt";


    private ArrayList<ArrayList<String>> shapeSimilarWordList;
    private ArrayList<ArrayList<String>> pronunSimialrWordList;
    private ArrayList<ArrayList<String>> pinyinWordSimialrWordList;

    public WordSimilarCalculator2(String root_path) {
        this.root_path = root_path;
        initShapeSimilarWordList();
        initPronunSimilarWordList();
        initPinyinWordSimilarWordList();
    }

    public String getRoot_path() {
        return root_path;
    }

    public void setRoot_path(String root_path) {
        this.root_path = root_path;
    }


    private void initShapeSimilarWordList() {
        shapeSimilarWordList = new ArrayList<>();
        ArrayList<String> lines = FileUtil.readFileByLine(root_path + shape_similar_file_name);
        for (String line : lines) {
            shapeSimilarWordList.add(new ArrayList<String>(Arrays.asList(line.split(","))));
        }
    }

    private void initPronunSimilarWordList() {
        pronunSimialrWordList = new ArrayList<>();
        ArrayList<String> lines = FileUtil.readFileByLine(root_path + pronounce_similar_file_name);
        for (String line : lines) {
            pronunSimialrWordList.add(new ArrayList<String>(Arrays.asList(line.split(","))));
        }
    }

    private void initPinyinWordSimilarWordList() {
        pinyinWordSimialrWordList = new ArrayList<>();
        ArrayList<String> lines = FileUtil.readFileByLine(root_path + pinyin_word_similar_file_name);
        for (String line : lines) {
            pinyinWordSimialrWordList.add(new ArrayList<String>(Arrays.asList(line.split(","))));
        }
    }

    public HashSet<String> getSimilarWord(String word) {
        int wordnum = HanLP.segment(word).size();
        HashSet<String> result = new HashSet<>();
//        result.addAll(getPronunSimilarWord(word, wordnum));
//        result.addAll(getShapeSimilarWord(word, wordnum));
        result.addAll(getPinyinSimilarWord(word, wordnum));
        return result;
    }

    public ArrayList<String> getPronunSimilarWord(String word, int wordnum) {
        return getSpecialSimilarWord(word, wordnum, pronunSimialrWordList);
    }


    public ArrayList<String> getPinyinSimilarWord(String word, int wordnum) {
        ArrayList<String> result = new ArrayList<>();

        for (int i = 0 ; i < word.length(); i++) {
            String singleWord = word.charAt(i) + "";
            ArrayList<String> simWordList = new ArrayList<>();
            for (ArrayList<String> singleLine : pinyinWordSimialrWordList) {
                if (singleLine.get(0).equals(singleWord)) {
                    simWordList.addAll(singleLine);
                }
            }
            for (String str: simWordList) {
                String modifiedWord = word.replace(word.charAt(i) + "", str);
                if (HanLP.segment(modifiedWord).size() <= wordnum) result.add(modifiedWord);
            }
        }


        return result;
    }

    public ArrayList<String> getShapeSimilarWord(String word, int wordnum) {
        return getSpecialSimilarWord(word, wordnum, shapeSimilarWordList);
    }

    private ArrayList<String> getSpecialSimilarWord(String word, int wordnum, ArrayList<ArrayList<String>> data) {
        ArrayList<String> result = new ArrayList<>();

        for (int i = 0 ; i < word.length(); i++) {
            String singleWord = word.charAt(i) + "";
            ArrayList<String> simWordList = new ArrayList<>();
            for (ArrayList<String> singleLine : data) {
                if (singleLine.contains(singleWord)) {
                    simWordList.addAll(singleLine);
                }
            }
            for (String str: simWordList) {
                String modifiedWord = word.replace(word.charAt(i) + "", str);
                if (HanLP.segment(modifiedWord).size() <= wordnum) result.add(modifiedWord);
            }
        }


        return result;
    }

}
