package com.pr.nlp.manager;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.collection.trie.DoubleArrayTrie;
import com.hankcs.hanlp.dictionary.py.PinyinUtil;
import com.pr.nlp.util.FileUtil;
import com.pr.nlp.util.Pinyin4jUtil;

import java.util.*;

public class WordSimilarCalculator {

    private String root_path = "/home/public/code/chinese_spelling/SimilarWordCalculator/src/main/resource/";
    private final String shape_similar_file_name = "shape_similar.txt";
    private final String pronounce_similar_file_name = "pronounce_similar.txt";
    private final String pinyin_word_similar_file_name = "pinyin_similar_word.txt";
    private final String pinyin_phrase_similar_file_name  = "pinyin_similar_phrase.txt";


    private ArrayList<ArrayList<String>> shapeSimilarWordList;
    private ArrayList<ArrayList<String>> pronunSimialrWordList;
//    private DoubleArrayTrie<List<String>> pinyinWordSimialrWordList;
//    private DoubleArrayTrie<List<String>> pinyinPhraseSimialrWordList;
    private DoubleArrayTrie<List<String>> pinyinWordSimialrWordList;
    private DoubleArrayTrie<List<String>> pinyinPhraseSimialrWordList;

    public WordSimilarCalculator(String root_path) {
        this.root_path = root_path;
        initShapeSimilarWordList();
        initPronunSimilarWordList();
        initPinyinWordSimilarWordList();
        initPinyinPhraseSimilarWordList();
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
        pinyinWordSimialrWordList = new DoubleArrayTrie<>();
        TreeMap<String, List<String>> pinyinWordTreeMap = new TreeMap<>();

        ArrayList<String> lines = FileUtil.readFileByLine(root_path + pinyin_word_similar_file_name);
        for (String line : lines) {
            String[] spInfo = line.split(":");
            if (!pinyinWordTreeMap.containsKey(spInfo[0])) {
                pinyinWordTreeMap.put(spInfo[0], new ArrayList<String>());
            }
            String[] words = spInfo[1].split(",");
            for (int i = 1; i < words.length; i++) {
                pinyinWordTreeMap.get(spInfo[0]).add(words[i]);
            }
        }
        pinyinWordSimialrWordList.build(pinyinWordTreeMap);
    }

    private void initPinyinPhraseSimilarWordList() {
        pinyinPhraseSimialrWordList = new DoubleArrayTrie<>();
        TreeMap<String, List<String>> pinyinPhraseTreeMap = new TreeMap<>();

        ArrayList<String> lines = FileUtil.readFileByLine(root_path + pinyin_phrase_similar_file_name);
        for (String line : lines) {
            String[] spInfo = line.split(":");
            if (!pinyinPhraseTreeMap.containsKey(spInfo[0])) {
                pinyinPhraseTreeMap.put(spInfo[0], new ArrayList<String>());
            }
            String[] words = spInfo[1].split(",");
            for (int i = 1; i < words.length; i++) {
                if (spInfo[0].length() == words[i].length()) {
                    pinyinPhraseTreeMap.get(spInfo[0]).add(words[i]);
                }
            }
        }
        pinyinPhraseSimialrWordList.build(pinyinPhraseTreeMap);
    }


    public HashSet<String> geSimilarInfo(String word) {
        if (word.length() == 1) return getSimilarWord(word);
        else return getSimilarPhrase(word);
    }

    public boolean isContainWord(String word) {
        String[] pinyinList = Pinyin4jUtil.converterToSpell(word).split(",");
        for (String pinyin : pinyinList) {
            if (word.length() == 1 && pinyinWordSimialrWordList.containsKey(pinyin)) return true;
            else if (pinyinPhraseSimialrWordList.containsKey(pinyin)) return true;
        }
        return false;
    }

    public HashSet<String> getSimilarWord(String word) {
        String[] pinyinList = Pinyin4jUtil.converterToSpell(word).split(",");
        HashSet<String> result = new HashSet<>();
//        result.addAll(getPronunSimilarWord(word, wordnum));
//        result.addAll(getShapeSimilarWord(word, wordnum));

        for (String py : pinyinList) {
            List<String> searchResult = pinyinWordSimialrWordList.get(py);
            if (searchResult != null && !searchResult.isEmpty()) result.addAll(searchResult);
        }
        return result;
    }

    public HashSet<String> getSimilarPhrase(String word) {
        String[] pinyinList = Pinyin4jUtil.converterToSpell(word).split(",");
        HashSet<String> result = new HashSet<>();
//        result.addAll(getPronunSimilarWord(word, wordnum));
//        result.addAll(getShapeSimilarWord(word, wordnum));

        for (String py : pinyinList) {
            List<String> searchResult = pinyinPhraseSimialrWordList.get(py);
            if (searchResult != null && !searchResult.isEmpty()) result.addAll(searchResult);
        }
        return result;
    }

    public ArrayList<String> getPronunSimilarWord(String word, int wordnum) {
        return getSpecialSimilarWord(word, wordnum, pronunSimialrWordList);
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
