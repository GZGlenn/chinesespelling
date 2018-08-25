package com.pr.nlp.method;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.utility.CharacterHelper;
import com.pr.nlp.data.ChangeData;
import com.pr.nlp.data.FeatureData;
import com.pr.nlp.data.SighanDataBean2;
import com.pr.nlp.manager.LanguageModelManager2;
import com.pr.nlp.manager.TrieLMTree;
import com.pr.nlp.manager.WordSimilarCalculator;
import com.pr.nlp.model.MSMOReg;
import com.pr.nlp.util.FileUtil;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.*;

public class BasicMethod3 {


    private String outputRoot = "";
    private final String model_name = "smoreg.model";

    private LanguageModelManager2 lmManager;
    private WordSimilarCalculator similarCalculator;
    private MSMOReg model;

    private int winsize = 5;
    private int maxChange = 1000;

    private double pmiThre = -10000;
    private double lmThre = 0.1;

    public BasicMethod3(String outputRoot) {
        this.outputRoot = outputRoot;

        if (!this.outputRoot.endsWith(System.getProperty("file.separator"))) {
            this.outputRoot += System.getProperty("file.separator");
        }

        System.out.println(this.outputRoot);
    }

    public void initLMManager(String root, String time) {
        lmManager = new LanguageModelManager2(root);
        lmManager.loadModel(time);
    }

    public void initSimCalculator(String root, String word2vecPath) {
        similarCalculator = new WordSimilarCalculator(root);
//        similarCalculator.setWord2vecpath(word2vecPath);
    }

    public void setOutputRoot(String outputRoot) {
        this.outputRoot = outputRoot;
    }

    public String getOutputRoot() {
        return outputRoot;
    }

    public int getWinsize() {
        return winsize;
    }

    public void setWinsize(int winsize) {
        this.winsize = winsize;
    }

    public void train(String trainFile) {

        // read data
        HashMap<String, SighanDataBean2> trainData = readFileData(trainFile);

        // create more sample
        HashMap<String, ArrayList<String>> candidateMap = createCandidate(trainData, maxChange);

        // get feature
        HashMap<String, ArrayList<FeatureData>> data = getFeature(trainData, candidateMap);

        // train model (is need correct)
        Instances instances = formatFeature(data);
        this.model = new MSMOReg();
        model.trainModel(instances);
        model.saveModel(this.outputRoot + model_name);
    }

    public void test(String testFile) {
        // read data
        HashMap<String, SighanDataBean2> testData = readFileData(testFile);

        // create more sample
        HashMap<String, ArrayList<String>> candidateMap = createCandidate(testData, maxChange);

        // get feature
        HashMap<String, ArrayList<FeatureData>> data = getFeature(testData, candidateMap);

        HashMap<String, Integer> result = new HashMap<>();
        // test model (is need correct)
        for (HashMap.Entry<String, ArrayList<FeatureData>> entry : data.entrySet()) {
            HashMap<String, ArrayList<FeatureData>> map = new HashMap<>();
            map.put(entry.getKey(), entry.getValue());
            Instances instances = formatFeature(map);

            if (model == null) model.loadModel(outputRoot + model_name);
            ArrayList<Double> predictScore = model.predict(instances);

            double maxScore = 0;
            int maxPreInd = 0;
            for (int i = 0 ; i < predictScore.size(); i++) {
                if (predictScore.get(i) > maxScore) {
                    maxPreInd = i;
                    maxScore = predictScore.get(i);
                }
            }

            result.put(entry.getKey(), maxPreInd);

        }

        HashMap<String, String> output = new HashMap<>();
        for (HashMap.Entry<String, SighanDataBean2> sighanData : testData.entrySet()) {
            String idStr = sighanData.getKey();
            String bestResult = candidateMap.get(idStr).get(result.get(idStr));
            output.put(idStr, bestResult);
        }

        showTestResult(output, testData);
    }

    public HashMap<String, String> correctSpelling(ArrayList<String> inputList) {
        HashMap<String, String> result = new HashMap<>();
        for (String input : inputList) {
            result.put(input, correctSpelling(input));
        }

        return result;
    }

    public String correctSpelling(String input) {
        HashMap<String, SighanDataBean2> sighanList = new HashMap<>();
        sighanList.put("-1", new SighanDataBean2("-1", input));

        // create more sample
        HashMap<String, ArrayList<String>> candidateMap = createCandidate(sighanList, maxChange);

        // get feature
        HashMap<String, ArrayList<FeatureData>> data = getFeature(sighanList, candidateMap);
        Instances instances = formatFeature(data);

        if (model == null) model.loadModel(outputRoot + model_name);
        ArrayList<Double> predicts = model.predict(instances);

        double maxScore = 0;
        int maxPreInd = 0;
        for (int i = 0 ; i < predicts.size(); i++) {
            if (predicts.get(i) > maxScore) {
                maxPreInd = i;
                maxScore = predicts.get(i);
            }
        }

        return candidateMap.get(-1).get(maxPreInd);
    }

    private HashMap<String, SighanDataBean2> readFileData(String path) {
        ArrayList<String> filePathes = FileUtil.getFiles(path);
        HashMap<String, SighanDataBean2> dataList = new HashMap<>();
        for (String filePath : filePathes) {
            ArrayList<String> lines = FileUtil.readFileByLine(filePath);
            for (String line : lines) {
                SighanDataBean2 dataBean = SighanDataBean2.parseData(line);
                if (!dataList.containsKey(dataBean.getIdStr())) {
                    dataList.put(dataBean.getIdStr(), dataBean);
                } else {
                    SighanDataBean2 oldData = dataList.get(dataBean.getIdStr());
                    oldData.addCorrectTriplet(dataBean.getCorrectTriplet());
                    dataList.put(dataBean.getIdStr(), oldData);
                }
            }
        }

        for (HashMap.Entry<String, SighanDataBean2> entry : dataList.entrySet()) {
            entry.getValue().sortCorrectTriplet();
            entry.getValue().calCorrectContent();
        }

        return dataList;
    }

    // create candidate by pmi and lm
    private HashMap<String, ArrayList<String>> createCandidate(HashMap<String, SighanDataBean2> trainData, int limit) {
        HashMap<String, ArrayList<String>> candidateList = new HashMap<>();
        for (HashMap.Entry<String, SighanDataBean2> sighanData: trainData.entrySet()) {
            List<Term> termList = HanLP.segment(sighanData.getValue().getContent());
            ArrayList<Triple<Integer, Integer, List<String>>> changeAllList = new ArrayList<>();
            int start = 0;
            for (int i = 0 ; i < termList.size() ; i++) {
                Term term = termList.get(i);
                if (term.nature.startsWith("w")) {
                    start += term.word.length();
                    continue;
                }
                else if (!similarCalculator.isContainWord(term.word)) {
                    char[] chars = term.word.toCharArray();
                    int tmpStart = start;
                    for (int j = 0 ; j < chars.length; j++) {
                        HashSet<String> similarWords = similarCalculator.getSimilarWord(chars[j] + "");
                        ArrayList<String> addWords = new ArrayList<>();
                        for (String str : similarWords) {
                            if (lmManager.isContain(str)) addWords.add(str);
                        }
                        Triple triple = new ImmutableTriple(tmpStart, 1, new ArrayList<>(addWords));
                        changeAllList.add(triple);
                    }
                    tmpStart++;
                }
                else {
                    String beforeWord = "", afterWord = "";
                    if (i == 0) beforeWord = "<s>";
                    if (i != 0 && !termList.get(i-1).nature.startsWith("w")) beforeWord = termList.get(i-1).word;
                    if (i != termList.size() - 1 && !termList.get(i+1).nature.startsWith("w")) afterWord = termList.get(i+1).word;
                    if (i == termList.size() - 1) afterWord = "</s>";

                    HashSet<String> similarWords = similarCalculator.geSimilarInfo(term.word);
                    double originScore = calPMIInThreeWin(beforeWord, term.word, afterWord);
                    ArrayList<String> addWords = new ArrayList<>();
                    for (String str : similarWords) {
                        if (str.equals(term.word)) continue;
                        double modifiedScore = calPMIInThreeWin(beforeWord, str, afterWord);
                        if (lmManager.isContain(str) && modifiedScore >= originScore) {
                            addWords.add(str);
                        }
                    }
                    if (addWords.isEmpty()) {
                        start += term.word.length();
                        continue;
                    }
                    Triple triple = new ImmutableTriple(start, term.word.length(), new ArrayList<>(addWords));
                    changeAllList.add(triple);
                }

                start += term.word.length();
            }

            changeAllList.sort(new Comparator<Triple<Integer, Integer, List<String>>>() {
                @Override
                public int compare(Triple<Integer, Integer, List<String>> o1, Triple<Integer, Integer, List<String>> o2) {
                    return o1.getLeft() - o2.getLeft();
                }
            });

            ArrayList<String> candidates = mergeAndBasicChoose(sighanData.getValue(), changeAllList, maxChange);

            for (String string : candidates) System.out.println("candidates :" + string);

//            candidates.add(sighanData.getValue().calCorrectContent());

            candidateList.put(sighanData.getKey(), candidates);
        }

        return candidateList;
    }

    private double calPMIInThreeWin(String left, String middle, String right) {
        double pmi = 0, num = 0;
        if (!left.isEmpty()) {
            pmi += lmManager.calPMI(left, middle);
            num++;
        }
        if (!right.isEmpty()) {
            pmi += lmManager.calPMI(middle, right);
            num++;
        }

        if (num > 0) pmi = pmi / num;
        return pmi;
    }


    // lm model get top 20
    private ArrayList<String> mergeAndBasicChoose(SighanDataBean2 data, ArrayList<Triple<Integer, Integer, List<String>>> changeList, int limit) {
//
//        double originScore = lmManager.calLM(data.getCorrectContent());
//
//        ArrayList<String> mergeResult = new ArrayList<>();
//        RecursiveReplace2(data.getContent(), 0, changeList, 0, mergeResult);
//
//        // viterbi cal lmscore
//        TrieLMTree lmTree = new TrieLMTree();
//        ArrayList<Pair<String, Double>> candidateAndScoreList = new ArrayList<>();
//        Comparator comparator = new Comparator<Pair<String, Double>>() {
//            @Override
//            public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
//                if (o1.getValue() - o2.getValue() > 0) return 1;
//                else return -1;
//            }
//        };
//
//
//        for (String string : mergeResult) {
//            double score = lmTree.calLMScore(string, lmManager);
//            if (score < originScore) continue;
//            if (candidateAndScoreList.size() < limit) candidateAndScoreList.add(new ImmutablePair<>(string, score));
//            else if (candidateAndScoreList.get(candidateAndScoreList.size() - 1).getRight() == score) candidateAndScoreList.add(new ImmutablePair<>(string, score));
//            else if (candidateAndScoreList.get(candidateAndScoreList.size() - 1).getRight() < score) {
//                candidateAndScoreList.set(candidateAndScoreList.size() - 1, new ImmutablePair<>(string, score));
//            }
//            candidateAndScoreList.sort(comparator);
//        }

        ArrayList<Pair<String, Double>> mergeResult = new ArrayList<>();
        TrieLMTree lmTree = new TrieLMTree();
        Comparator comparator = new Comparator<Pair<String, Double>>() {
            @Override
            public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                if (o2.getValue().floatValue() > o1.getValue().floatValue()) return 1;
                else if (o2.getValue().floatValue() == o1.getValue().floatValue()) return 0;
                else return -1;
            }
        };
        double originScore = lmTree.calLMScore(data.getContent(), lmManager);
        double originScore2 = lmManager.calLM(data.getContent());
        double correctScore = lmManager.calLM(data.getCorrectContent());
        RecursiveReplace(data.getContent(), 0, changeList, 0, mergeResult, lmTree, comparator, originScore2, limit);
        ArrayList<String> result = new ArrayList<>();
        for (Pair<String, Double> pair : mergeResult) result.add(pair.getKey());
        return result;
    }


    private void RecursiveReplace2(String prevStr, int lastChange,
                                  ArrayList<Triple<Integer, Integer, List<String>>> replaceList,
                                  int curIndex,
                                  ArrayList<String> list) {
        if (curIndex >= replaceList.size())
            return;
        Triple<Integer, Integer, List<String>> triplet = replaceList.get(curIndex);
        if (triplet.getLeft() <= lastChange) {
            RecursiveReplace2(prevStr, lastChange, replaceList, curIndex + 1, list);
        }
        else {
            RecursiveReplace2(prevStr, lastChange, replaceList, curIndex + 1, list);
            char[] charBuffer = prevStr.toCharArray();
            for (String target: triplet.getRight()) {
                for (int i = 0; i < triplet.getMiddle(); ++i) {
                    charBuffer[triplet.getLeft() + i] = target.charAt(i);
                }
                String curStr = String.valueOf(charBuffer);
                list.add(curStr);
                RecursiveReplace2(curStr, triplet.getLeft() + triplet.getMiddle() - 1, replaceList, curIndex + 1, list);
            }
        }
    }


    private void RecursiveReplace(String prevStr, int lastChange,
                                  ArrayList<Triple<Integer, Integer, List<String>>> replaceList,
                                  int curIndex,
                                  List<Pair<String, Double>> list,
                                  TrieLMTree lmTree,
                                  Comparator<Pair<String, Double>> comparator,
                                  double originScore,
                                  int limit) {
        if (curIndex >= replaceList.size())
            return;
        Triple<Integer, Integer, List<String>> triplet = replaceList.get(curIndex);
        if (triplet.getLeft() <= lastChange) {
            RecursiveReplace(prevStr, lastChange, replaceList, curIndex + 1, list, lmTree, comparator, originScore, limit);
        }
        else {
            RecursiveReplace(prevStr, lastChange, replaceList, curIndex + 1, list, lmTree, comparator, originScore, limit);
            char[] charBuffer = prevStr.toCharArray();
            for (String target: triplet.getRight()) {
                for (int i = 0; i < triplet.getMiddle(); ++i) {
                    charBuffer[triplet.getLeft() + i] = target.charAt(i);
                }
                String curStr = String.valueOf(charBuffer);
                double score = lmManager.calLM(curStr);
                if (score >= originScore) {
                    if (list.size() < limit) list.add(new ImmutablePair<>(curStr, score));
                    else if (score > list.get(list.size() - 1).getValue()) {
                        list.set(list.size() - 1, new ImmutablePair<>(curStr, score));
                        if (list.size() > limit) {
                            list = list.subList(0, limit);
                        }
                        list.sort(comparator);
                    } else if (score == list.get(list.size() - 1).getValue()) {
                        list.add(new ImmutablePair<>(curStr, score));
                    }
                }
                RecursiveReplace(curStr, triplet.getLeft() + triplet.getMiddle() - 1, replaceList, curIndex + 1, list, lmTree, comparator, originScore, limit);
            }
        }
    }

    private HashMap<String, ArrayList<FeatureData>> getFeature(HashMap<String, SighanDataBean2> sighanDataMap,
                                                               HashMap<String, ArrayList<String>> candidatesList) {
        HashMap<String, ArrayList<FeatureData>> data = new HashMap<>();
        for (HashMap.Entry<String, ArrayList<String>> entry : candidatesList.entrySet()) {
            SighanDataBean2 sighanData = sighanDataMap.get(entry.getKey());
            ArrayList<FeatureData> featureList = new ArrayList<>();
            for (int ind = 0 ; ind < entry.getValue().size(); ind++) {
                FeatureData feature = new FeatureData();
                feature.calFeature(sighanData.getContent(), entry.getValue().get(ind), sighanData.getCorrectContent(), lmManager);
                featureList.add(feature);
            }

            data.put(entry.getKey(), featureList);
        }
        return data;
    }

    private Instances formatFeature(HashMap<String, ArrayList<FeatureData>> featureMap) {
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("lmfeat"));
        attributes.add(new Attribute("pmi"));
        attributes.add(new Attribute("wordnum"));
        attributes.add(new Attribute("label"));

        Instances instances = new Instances("train_data",attributes,0);
        instances.setClassIndex(instances.numAttributes() - 1);
        for (HashMap.Entry<String, ArrayList<FeatureData>> entry : featureMap.entrySet()) {
            for (FeatureData feature : entry.getValue()) {
                Instance instance = new DenseInstance(instances.numAttributes());
                instance.setValue(0, feature.getLmfeat());
                instance.setValue(1, feature.getPmifeat());
                instance.setValue(2, feature.getWordNum());
                instance.setValue(3, feature.getLabel());
                instances.add(instance);
            }
        }


        return instances;
    }

    private void showTestResult(HashMap<String, String> predictList, HashMap<String, SighanDataBean2> inputData) {
        double correctNum = 0;
        double totalNum = predictList.size();
        for (HashMap.Entry<String, String> preEntry : predictList.entrySet()) {
            String idStr = preEntry.getKey();
            String correctStr = inputData.get(idStr).getCorrectContent();
            String predictStr = preEntry.getValue();
            if (predictStr.equals(correctStr)) correctNum++;

            System.out.println("correct ==> " + correctStr);
            System.out.println("predict ==> " + predictStr);
        }

        System.out.println("accuracy : " +  correctNum / totalNum);
    }

}
