package com.pr.nlp.method;

import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;
import com.pr.nlp.data.*;
import com.pr.nlp.model.MSMOReg;
import com.pr.nlp.util.FileUtil;
import com.pr.nlp.util.LogUtil;
import javafx.util.Pair;
import weka.core.Instances;

import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BasicMethod {

    private final static String seq = System.getProperty("file.separator");
    private final static String staticUnigramName = "static_unigram.txt";
    private final static String staicBigramName = "static_bigram.txt";
    private final static String smoothBigramName = "lm_model.txt";
    private final static String pmiName = "pmi_model.txt";
    private final double smoothValue = 0.1;

    private String inputRoot = "";
    private String outputRoot = "";
    private String pattern = "";

    private MSMOReg model;


    public BasicMethod(String inputRoot, String outputRoot) {
        this(inputRoot, outputRoot, ".txt$");
    }

    public BasicMethod(String inputRoot, String outputRoot, String pattern) {
        this.inputRoot = inputRoot;
        this.outputRoot = outputRoot;
        this.pattern = pattern;

        if (!outputRoot.endsWith(seq)) {
            outputRoot += seq;
        }

        if (isNeedStatistic()) {
            createStatisticInfo();
            createLMModel();
            createPMIModel();
        }
    }

    public void saveModel(String path) {
        if (this.model != null) this.model.saveModel(path);
    }

    public void loadModel(String path) {
        this.model.loadModel(path);
    }

    public void train(String trainFile) {

        // read data
        ArrayList<SighanDataBean> trainData = readTrainData(trainFile);

        // load data
        HashMap<String, LMStatisticWordBean> lmModel = loadLMModel();
        HashMap<String, PMIWordPairBean> pmiModel = loadPMIModel();
        HashMap<String, WordPairStisticBean> wordPairModel = loadStatisticBigram();
        HashMap<String, WordStatisticBean> wordModel = loadStatisticUnigram();

        // create more sample
        ArrayList<SighanDataBean> candidate = createCandidate(trainData, lmModel, pmiModel);

        // get feature
        ArrayList<Pair<FeatureData, Integer>> data = getFeature(candidate);

        // train model (is need correct)
        Instances instances = formatFeature(data);
        this.model = new MSMOReg();
        model.trainModel(instances);

    }

    private ArrayList<SighanDataBean> readTrainData(String path) {
        ArrayList<String> lines = FileUtil.getFiles(path);
        ArrayList<SighanDataBean> dataList = new ArrayList<>();
        for (String line : lines) {
            dataList.add(SighanDataBean.parseData(line));
        }
        return dataList;
    }

    // create candidate by pmi and lm
    private ArrayList<SighanDataBean> createCandidate(ArrayList<SighanDataBean> trainData,
                                                      HashMap<String, LMStatisticWordBean> lmModel,
                                                      HashMap<String, PMIWordPairBean> pmiModel) {
        ArrayList<SighanDataBean> result = new ArrayList<>();
        for (SighanDataBean sighanData: trainData) {

        }

        return result;
    }

    private ArrayList<Pair<FeatureData, Integer>> getFeature(ArrayList<SighanDataBean> candidate) {
        ArrayList<Pair<FeatureData, Integer>> data = new ArrayList<>();

        return data;
    }

    private Instances formatFeature(ArrayList<Pair<FeatureData, Integer>> features) {
        return null;
    }


    private double calSetenceLMScore(String content, HashMap<String, LMStatisticWordBean> lmModel,
                                     HashMap<String, WordStatisticBean> unigramModel) {
        return -1;
    }

    private boolean isNeedStatistic() {
        if (!FileUtil.bExistFile(outputRoot + staticUnigramName)) return true;
        if (!FileUtil.bExistFile(outputRoot + staicBigramName)) return true;
        if (!FileUtil.bExistFile(outputRoot + smoothBigramName)) return true;
        if (!FileUtil.bExistFile(outputRoot + pmiName)) return true;
        return false;
    }

    private void createStatisticInfo() {
        HashMap<String, WordStatisticBean> unigramStatisticMap = new HashMap<>();
        HashMap<String, WordPairStisticBean> bigramStatisticMap = new HashMap<>();

        ArrayList<String> filePathes = FileUtil.getFiles(inputRoot, pattern);
        for (String filepath : filePathes) {
            FileReader fileReader = null;
            BufferedReader bufferedReader = null;
            try {
                fileReader = new FileReader(filepath);
                bufferedReader = new BufferedReader(fileReader);
                String line = null;
                while((line = bufferedReader.readLine()) != null) {
                    List<List<Term>> sentenceList = StandardTokenizer.seg2sentence(line);
                    for (List<Term> wordList : sentenceList) {
                        ArrayList<Term> cleanWordList = getCleanWord(wordList);
                        getStatisticUnigram(cleanWordList, unigramStatisticMap);
                        getStatisticBigram(cleanWordList, bigramStatisticMap);
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

        calUnigramFreqency(unigramStatisticMap);
        calBigramFreqency(bigramStatisticMap);

        FileUtil.deleleFiles(outputRoot + staticUnigramName);
        FileWriter fw = FileUtil.createFileWriter(outputRoot + staticUnigramName);
        for (HashMap.Entry<String, WordStatisticBean> entry : unigramStatisticMap.entrySet()) {
            FileUtil.append(fw, entry.getValue().toString() + "\n");
        }
        FileUtil.close(fw);

        FileUtil.deleleFiles(outputRoot + staicBigramName);
        fw = FileUtil.createFileWriter(outputRoot + staicBigramName);
        for (HashMap.Entry<String, WordPairStisticBean> entry : bigramStatisticMap.entrySet()) {
            FileUtil.append(fw, entry.getValue().toString() + "\n");
        }
        FileUtil.close(fw);
    }

    private ArrayList<Term> getCleanWord(List<Term> wordList) {
        ArrayList<Term> termList = new ArrayList<>();

        return termList;
    }

    private void getStatisticUnigram(ArrayList<Term> wordList, HashMap<String, WordStatisticBean> statisticMap) {
        for (Term term : wordList) {
            if (!statisticMap.containsKey(term.word)) {
                statisticMap.put(term.word, new WordStatisticBean(term.word, 1 + smoothValue));
            }
            else {
                statisticMap.get(term.word).addOneFrequent();
            }
        }
    }

    private void getStatisticBigram(ArrayList<Term> wordList, HashMap<String, WordPairStisticBean> statisticMap) {
        for (int i = 0 ; i < wordList.size() - 1; i++) {
            String firstWord = wordList.get(i).word;
            String secondWord = wordList.get(i+1).word;
            String key = firstWord + "#" + secondWord;
            if (!statisticMap.containsKey(key)) {
                statisticMap.put(key, new WordPairStisticBean(firstWord, secondWord, 1 + smoothValue, 0));
            } else {
                statisticMap.get(key).addOneFrequent();
            }
        }
    }

    private void calUnigramFreqency(HashMap<String, WordStatisticBean> statisticMap) {
        double totalNum = 0;
        for (HashMap.Entry<String, WordStatisticBean> entry : statisticMap.entrySet()) {
            totalNum += entry.getValue().getFrequent();
        }

        for (HashMap.Entry<String, WordStatisticBean> entry : statisticMap.entrySet()) {
            double freqency = entry.getValue().getFrequent() / totalNum;
            entry.getValue().setFrequency(freqency);
        }
    }

    private void calBigramFreqency(HashMap<String, WordPairStisticBean> statisticMap) {
        double totalNum = 0;
        for (HashMap.Entry<String, WordPairStisticBean> entry : statisticMap.entrySet()) {
            totalNum += entry.getValue().getFrequent();
        }

        for (HashMap.Entry<String, WordPairStisticBean> entry : statisticMap.entrySet()) {
            double freqency = entry.getValue().getFrequent() / totalNum;
            entry.getValue().setFrequency(freqency);
        }
    }

    private HashMap<String, WordStatisticBean> loadStatisticUnigram() {
        String filePath = outputRoot + staticUnigramName;
        List<String> infoList = FileUtil.readFileByLine(filePath);

        HashMap<String, WordStatisticBean> result = new HashMap<>();
        for (String str : infoList) {
            String[] spInfo = str.split(",");
            String word = spInfo[0];
            long frequent = Long.valueOf(spInfo[1]);
            double frequency = Double.valueOf(spInfo[2]);
            result.put(word, new WordStatisticBean(word, frequent, frequency));
        }
        return result;
    }

    private HashMap<String, WordPairStisticBean> loadStatisticBigram() {
        String filePath = outputRoot + staicBigramName;
        List<String> infoList = FileUtil.readFileByLine(filePath);

        HashMap<String, WordPairStisticBean> result = new HashMap<>();
        for (String str : infoList) {
            String[] spInfo = str.split(",");
            String firstWord = spInfo[0];
            String secondWord = spInfo[1];
            long frequent = Long.valueOf(spInfo[2]);
            double frequency = Double.valueOf(spInfo[3]);
            result.put(firstWord + "#" + secondWord, new WordPairStisticBean(firstWord, secondWord, frequent, frequency));
        }
        return result;
    }

    private void createLMModel() {
        HashMap<String, WordPairStisticBean> bigramStatisticMap = loadStatisticBigram();
        HashMap<String, WordStatisticBean> unigramStatisticMap = loadStatisticUnigram();

        double totalUnigramNum = 0;
        for (HashMap.Entry<String, WordStatisticBean> wordBean : unigramStatisticMap.entrySet()) {
            totalUnigramNum += wordBean.getValue().getFrequent();
        }

        HashMap<String, LMStatisticWordBean> smoothLMModel = new HashMap<>();
        for (HashMap.Entry<String, WordStatisticBean> wordBeanA : unigramStatisticMap.entrySet()) {
            for (HashMap.Entry<String, WordStatisticBean> wordBeanB : unigramStatisticMap.entrySet()) {
                String firstWord = wordBeanA.getKey();
                String secondWord = wordBeanB.getKey();
                if (!smoothLMModel.containsKey(firstWord + "#" + secondWord)) {
                    double knesderNeySmoothValue = calKnesderNeySmoothing(firstWord, secondWord,
                            bigramStatisticMap, unigramStatisticMap, totalUnigramNum);
                    smoothLMModel.put(firstWord + "#" + secondWord,
                            new LMStatisticWordBean(firstWord, secondWord, knesderNeySmoothValue));
                }
            }
        }

        FileUtil.deleleFiles(outputRoot + smoothBigramName);
        FileWriter fw = FileUtil.createFileWriter(outputRoot + smoothBigramName);
        for (HashMap.Entry<String, LMStatisticWordBean> lmWord : smoothLMModel.entrySet()) {
            FileUtil.append(fw, lmWord.getValue().toString() + "\n");
        }
        FileUtil.close(fw);
    }

    private double calKnesderNeySmoothing(String firstWord, String secondWord,
                                          HashMap<String, WordPairStisticBean> bigramMap,
                                          HashMap<String, WordStatisticBean> unigramMap,
                                          double totalUnigramNum) {
        double dForBiggerNum = 0.75;
        double dForSingleNum = 0.5;

        if (unigramMap.getOrDefault(secondWord, new WordStatisticBean(secondWord)).getFrequent() == 0) {
            return smoothValue / totalUnigramNum;
        }
        else if (unigramMap.getOrDefault(firstWord, new WordStatisticBean(firstWord)).getFrequent() == 0) {
            return unigramMap.get(secondWord).getFrequency();
        }
        else {
            double pCommunication = 0;
            if (bigramMap.containsKey(firstWord + "#" + secondWord)) {
                double postfixTotalNum = 0;
                for (HashMap.Entry<String, WordPairStisticBean> entry : bigramMap.entrySet()) {
                    if (entry.getKey().endsWith("#" + secondWord)) {
                        postfixTotalNum += 1;
                    }
                }
                pCommunication = postfixTotalNum / bigramMap.size();
            }

            double lambda = 0;
            double d = 0;
            if (pCommunication != 0) {
                double wnum = 0;
                for (HashMap.Entry<String, WordPairStisticBean> entry : bigramMap.entrySet()) {
                    if (entry.getKey().startsWith(firstWord + "#")) {
                        wnum += 1;
                    }
                }
                d = bigramMap.getOrDefault(firstWord + "#" + secondWord,
                        new WordPairStisticBean("", "", smoothValue))
                        .getFrequent() > 1 ? dForBiggerNum : dForSingleNum;
                lambda = d / unigramMap.get(firstWord).getFrequent() * wnum;
            }

            double firstColumn = Math.max(0, bigramMap.getOrDefault(firstWord + "#" + secondWord,
                    new WordPairStisticBean("","", smoothValue)).getFrequent()
                    - d) / unigramMap.get(firstWord).getFrequent();

            return firstColumn + lambda * pCommunication;
        }
    }

    private void createPMIModel() {
        HashMap<String, WordPairStisticBean> bigramStatisticMap = loadStatisticBigram();
        HashMap<String, WordStatisticBean> unigramStatisticMap = loadStatisticUnigram();

        double totalUnigramNum = 0;
        for (HashMap.Entry<String, WordStatisticBean> wordBean : unigramStatisticMap.entrySet()) {
            totalUnigramNum += wordBean.getValue().getFrequent();
        }

        double totalBigramNum = 0;
        for (HashMap.Entry<String, WordPairStisticBean> wordPairBean : bigramStatisticMap.entrySet()) {
            totalBigramNum += wordPairBean.getValue().getFrequent();
        }

        HashMap<String, PMIWordPairBean> pmiMap = new HashMap<>();
        for (HashMap.Entry<String, WordStatisticBean> wordBeanA : unigramStatisticMap.entrySet()) {
            for (HashMap.Entry<String, WordStatisticBean> wordBeanB : unigramStatisticMap.entrySet()) {
                String firstWord = wordBeanA.getKey();
                String secondWord = wordBeanB.getKey();
                if (!pmiMap.containsKey(firstWord + "#" + secondWord)) {
                    double pmi = getPMI(firstWord, secondWord,
                            bigramStatisticMap, unigramStatisticMap,
                            totalUnigramNum, totalBigramNum);
                    pmiMap.put(firstWord + "#" + secondWord,
                            new PMIWordPairBean(firstWord, secondWord, pmi));
                }
            }
        }


        FileUtil.deleleFiles(outputRoot + pmiName);
        FileWriter fw = FileUtil.createFileWriter(outputRoot + pmiName);
        for (HashMap.Entry<String, PMIWordPairBean> pmiWordPair : pmiMap.entrySet()) {
            FileUtil.append(fw, pmiWordPair.getValue().toString() + "\n");
        }
        FileUtil.close(fw);
    }

    private double getPMI(String firstWord, String secondWord,
                        HashMap<String, WordPairStisticBean> bigramMap,
                        HashMap<String, WordStatisticBean> unigramMap,
                          double totalUnigramNum, double totalBigramNum) {

        double numerator = bigramMap.getOrDefault(firstWord + "#" + secondWord,
                new WordPairStisticBean("", "", smoothValue))
                .getFrequent() / totalBigramNum;

        double denominatorFirst = unigramMap.getOrDefault(firstWord,
                new WordStatisticBean("", smoothValue))
                .getFrequent() / totalUnigramNum;

        double denominatorSecond = unigramMap.getOrDefault(secondWord,
                new WordStatisticBean("", smoothValue))
                .getFrequent() / totalUnigramNum;

        return numerator / (denominatorFirst * denominatorSecond);
    }

    private HashMap<String, LMStatisticWordBean> loadLMModel() {
        String filePath = outputRoot + smoothBigramName;
        List<String> infoList = FileUtil.readFileByLine(filePath);

        HashMap<String, LMStatisticWordBean> result = new HashMap<>();
        for (String str : infoList) {
            String[] spInfo = str.split(",");
            String firstWord = spInfo[0];
            String secondWord = spInfo[1];
            double conditionalProb = Double.valueOf(spInfo[1]);
            result.put(firstWord + "#" + secondWord, new LMStatisticWordBean(firstWord, secondWord, conditionalProb));
        }
        return result;
    }

    private HashMap<String, PMIWordPairBean> loadPMIModel() {
        String filePath = outputRoot + pmiName;
        List<String> infoList = FileUtil.readFileByLine(filePath);

        HashMap<String, PMIWordPairBean> result = new HashMap<>();
        for (String str : infoList) {
            String[] spInfo = str.split(",");
            String firstWord = spInfo[0];
            String secondWord = spInfo[1];
            double entropy = Double.valueOf(spInfo[1]);
            result.put(firstWord + "#" + secondWord, new PMIWordPairBean(firstWord, secondWord, entropy));
        }
        return result;
    }
}
