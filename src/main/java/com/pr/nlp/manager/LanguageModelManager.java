package com.pr.nlp.manager;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.pr.nlp.util.LogUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;

public class LanguageModelManager {


    private final static String staticUnigramName = "_1gram_clean.count";
    private final static String lmModelPath = "_cn_clean.lm";
//    private final static String CONNECTMODE = "_";

    private String mlRootPath;

    private HashMap<String, HashMap<String, Float>> lmModel;
    private HashMap<String, Float> unigram;

    public LanguageModelManager(String path) {
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

        String unigramModelAbPath = mlRootPath + time + staticUnigramName;
        loadUnigram(unigramModelAbPath);

        String lmModelAbPath = mlRootPath + time + lmModelPath;
        loadLMModel(lmModelAbPath);
    }

    private void loadUnigram(String path) {
        float total = 0;
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
                    String word = info[0].trim();
                    if (!isWordNeedConsider(word)) continue;
                    float num = Float.valueOf(info[1].trim());
                    unigram.put(word, num);
                    total += num;
                    System.out.println("load unigram : " + word + "\t" + ++index);
//                    if (index > 1000) break;
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
        }

        for (HashMap.Entry<String, Float> entry : unigram.entrySet()) {
            entry.setValue(entry.getValue() / total);
        }

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
                    String[] words = info[1].trim().split(" ");
                    String wordA = words[0].trim();
                    String wordB = words[1].trim();
                    if (!isWordNeedConsider(wordA) || !isWordNeedConsider(wordB)) continue;

                    float frequency = Float.valueOf(info[0].trim());
                    if (!lmModel.containsKey(wordA)) lmModel.put(wordA, new HashMap<>());
                    lmModel.get(wordA).put(wordB, frequency);
                    System.out.println("load lmmodel : " + wordA + "\t" + wordB + "\t" + ++index);
//                    if (index > 1000) break;
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
        }

    }

    private boolean isWordNeedConsider(String word) {
        for (int i = 0 ; i < word.length(); i++) {
            char c = word.charAt(i);
            if (isChinese(c) || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) return true;
//            if (!isChinese(c)) return false;
        }
        return true;
    }

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
            float lmFeat = unigram.getOrDefault(termList.get(index).word, Float.valueOf(0));
            for (int i = index + 1; i < termList.size(); i++) {
                String firstWord = termList.get(i - 1).word;
                String secondWord = termList.get(i).word;
                lmFeat *= lmModel.getOrDefault(firstWord, new HashMap<>()).getOrDefault(secondWord, Float.valueOf(0));
            }
            return lmFeat;
        }
    }

    public float calPMI(String wordA, String wordB) {
        if (!unigram.containsKey(wordB) && !unigram.containsKey(wordA)) return 0;
        else if (!unigram.containsKey(wordA)) return lmModel.getOrDefault(wordA, new HashMap<>()).getOrDefault(wordB, Float.valueOf(0)) / unigram.get(wordB);
        else if (!unigram.containsKey(wordB)) return lmModel.getOrDefault(wordB, new HashMap<>()).getOrDefault(wordA, Float.valueOf(0)) / unigram.get(wordA);
        else return (lmModel.getOrDefault(wordA, new HashMap<>()).getOrDefault(wordB, Float.valueOf(0)) * unigram.get(wordA) +
                    lmModel.getOrDefault(wordB, new HashMap<>()).getOrDefault(wordA, Float.valueOf(0)) * unigram.get(wordB)) / (unigram.get(wordA) * unigram.get(wordB));
    }

    public float getUnigram(String word) {
        return unigram.getOrDefault(word, Float.valueOf(0));
    }

    public float getLM(String wordA, String wordB) {
        return lmModel.getOrDefault(wordA, new HashMap<>()).getOrDefault(wordB, Float.valueOf(0));
    }

}
