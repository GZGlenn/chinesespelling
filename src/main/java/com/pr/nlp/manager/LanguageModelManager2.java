package com.pr.nlp.manager;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.pr.nlp.util.LogUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;

public class LanguageModelManager2 {

    private final static Float DEFAULTMIN = Float.valueOf("-10");
    private final static String lmModelPath = "_cn_clean2.lm";
//    private final static String CONNECTMODE = "_";

    private String mlRootPath;

    private HashMap<String, HashMap<String, Float>> lmModel;
    private HashMap<String, Float> unigram;

    public LanguageModelManager2(String path) {
        mlRootPath = path;
    }

    public String getMlRootPath() {
        return mlRootPath;
    }

    public HashMap<String, HashMap<String, Float>> getLmModel() {
        return lmModel;
    }

    public HashMap<String, Float> getUnigram() {
        return unigram;
    }

    public void setMlRootPath(String mlRootPath) {
        this.mlRootPath = mlRootPath;
    }

    public void setLmModel(HashMap<String, HashMap<String, Float>> lmModel) {
        this.lmModel = lmModel;
    }

    public void setUnigram(HashMap<String, Float> unigram) {
        this.unigram = unigram;
    }

    public void loadModel(String time) {
        lmModel = new HashMap<>();
        unigram = new HashMap<>();

        if (mlRootPath.isEmpty()) {
            return;
        }

        String lmModelAbPath = mlRootPath + time + lmModelPath;
        loadLMModel(lmModelAbPath);
    }

    private void loadLMModel(String path) {
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileReader = new FileReader(path);
            bufferedReader = new BufferedReader(fileReader);
            String line = null;
            int index = 0;
            while((line = bufferedReader.readLine()) != null) {
                try {
                    String[] info = line.split("\t");

                    // unigram
                    if (info.length == 3) {
                        String word = info[1];
                        float score = Float.valueOf(info[0]);
                        unigram.put(word, score);
//                        System.out.println("load lmmodel : " + word + "\t" + score + "\t" + ++index);
                    }
                    else {
                        String[] words = info[1].trim().split(" ");
                        String wordA = words[0].trim();
                        String wordB = words[1].trim();
//                        if (!isWordNeedConsider(wordA) || !isWordNeedConsider(wordB)) continue;

                        float frequency = Float.valueOf(info[0].trim());
                        if (!lmModel.containsKey(wordA)) lmModel.put(wordA, new HashMap<>());
                        lmModel.get(wordA).put(wordB, frequency);
//                        System.out.println("load lmmodel : " + wordA + "\t" + wordB + "\t" + ++index);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        } catch(Exception e) {
            System.out.println("create statistic info error : " + e.getMessage());
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Exception e) {
                    LogUtil.getInstance().printLog(e.getMessage(), LogUtil.LEVEL.ERROR);
                }
            }
            if (fileReader != null)  {
                try {
                    fileReader.close();
                } catch (Exception e) {
                    LogUtil.getInstance().printLog(e.getMessage(), LogUtil.LEVEL.ERROR);
                }
            }

            System.out.println("unigram count : " + unigram.size());
            System.out.println("lm count : " + lmModel.size());
        }

    }

//    private boolean isWordNeedConsider(String word) {
//        for (int i = 0 ; i < word.length(); i++) {
//            char c = word.charAt(i);
//            if (isChinese(c) || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) return true;
////            if (!isChinese(c)) return false;
//        }
//        return true;
//    }

    private boolean isChinese(char c) {
        return c >= 0x4E00 && c <= 0x9FA5;
    }

    public float calLM(String content) {
        List<Term> termList = HanLP.segment(content);
        return calLM(termList);
    }

    public float calLM(List<Term> termList) {
        if (termList.size() == 0) return  0;
        else {
            int index = 0;
            while (termList.get(index).nature.startsWith("w")) index++;
            float lmFeat = unigram.getOrDefault(termList.get(index).word, DEFAULTMIN);
            for (int i = index + 1; i < termList.size(); i++) {
                String firstWord = termList.get(i - 1).word;
                String secondWord = termList.get(i).word;
                lmFeat += lmModel.getOrDefault(firstWord, new HashMap<>()).getOrDefault(secondWord, DEFAULTMIN);
            }
            return lmFeat;
        }
    }

    public float calPMI(String wordA, String wordB) {
        if (!unigram.containsKey(wordB) && !unigram.containsKey(wordA)) return DEFAULTMIN;
        else if (!unigram.containsKey(wordA)) return lmModel.getOrDefault(wordA, new HashMap<>()).getOrDefault(wordB, DEFAULTMIN) - unigram.get(wordB);
        else if (!unigram.containsKey(wordB)) return lmModel.getOrDefault(wordB, new HashMap<>()).getOrDefault(wordA, DEFAULTMIN) - unigram.get(wordA);
        else return (lmModel.getOrDefault(wordA, new HashMap<>()).getOrDefault(wordB, DEFAULTMIN) + unigram.get(wordA) +
                    lmModel.getOrDefault(wordB, new HashMap<>()).getOrDefault(wordA, DEFAULTMIN) + unigram.get(wordB)) - unigram.get(wordA) - unigram.get(wordB);
    }

    public boolean isContain(String word) {
        return unigram.containsKey(word) || lmModel.containsKey(word);
    }

    public float getUnigram(String word) {
        return unigram.getOrDefault(word, DEFAULTMIN);
    }

    public float getLM(String wordA, String wordB) {
        return lmModel.getOrDefault(wordA, new HashMap<>()).getOrDefault(wordB, DEFAULTMIN);
    }

}
