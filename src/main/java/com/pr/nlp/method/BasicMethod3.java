package com.pr.nlp.method;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.utility.CharacterHelper;
import com.pr.nlp.data.ChangeData;
import com.pr.nlp.data.FeatureData;
import com.pr.nlp.data.SighanDataBean;
import com.pr.nlp.manager.LanguageModelManager;
import com.pr.nlp.manager.WordSimilarCalculator;
import com.pr.nlp.model.MSMOReg;
import com.pr.nlp.util.FileUtil;
import javafx.scene.control.TextFormatter;
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

    private LanguageModelManager lmManager;
    private WordSimilarCalculator similarCalculator;
    private MSMOReg model;

    private int winsize = 5;
    private int maxChangeInOneWin = 5;
    private int maxChange = 100;

    private double pmiThre = -10000;
    private double lmThre = 0.1;

    public BasicMethod3(String outputRootn) {
        this.outputRoot = outputRoot;

        if (!outputRoot.endsWith(System.getProperty("file.separator"))) {
            outputRoot += System.getProperty("file.separator");
        }
    }

    public void initLMManager(String root, String time) {
        lmManager = new LanguageModelManager(root);
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
        HashMap<String, SighanDataBean> trainData = readFileData(trainFile);

        // create more sample
        HashMap<String, ArrayList<String>> candidateMap = createCandidate(trainData, maxChange);

        // get feature
        HashMap<String, ArrayList<FeatureData>> data = getFeature(trainData, candidateMap);


        // train model (is need correct)
        Instances instances = formatFeature(data);
        this.model = new MSMOReg();
        model.trainModel(instances);
    }

    public void test(String testFile) {
        // read data
        HashMap<String, SighanDataBean> testData = readFileData(testFile);

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
        for (HashMap.Entry<String, SighanDataBean> sighanData : testData.entrySet()) {
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
        HashMap<String, SighanDataBean> sighanList = new HashMap<>();
        sighanList.put("-1", new SighanDataBean("-1", input));

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

    private HashMap<String, SighanDataBean> readFileData(String path) {
        ArrayList<String> filePathes = FileUtil.getFiles(path);
        HashMap<String, SighanDataBean> dataList = new HashMap<>();
        for (String filePath : filePathes) {
            ArrayList<String> lines = FileUtil.readFileByLine(filePath);
            for (String line : lines) {
                SighanDataBean dataBean = SighanDataBean.parseData(line);
                dataList.put(dataBean.getIdStr(), dataBean);
            }
        }
        return dataList;
    }

    // create candidate by pmi and lm
    private HashMap<String, ArrayList<String>> createCandidate(HashMap<String, SighanDataBean> trainData, int limit) {
        HashMap<String, ArrayList<String>> candidateList = new HashMap<>();
        for (HashMap.Entry<String, SighanDataBean> sighanData: trainData.entrySet()) {
            List<Term> termList = HanLP.segment(sighanData.getValue().getContent());
            ArrayList<Triple<Integer, Integer, List<String>>> changeAllList = new ArrayList<>();
            int start = 0;
            for (Term term : termList) {
                if (term.nature.startsWith("w")) continue;
                else if (term.word.length() == 1) {
                    HashSet<String> similarWords = similarCalculator.geSimilarInfo(term.word);
                    for (String modifiedWord : similarWords) {
                        Triple triple = new ImmutableTriple(start, term.word.length(), modifiedWord);
                        changeAllList.add(triple);
                    }
                }
                else if (!similarCalculator.isContainWord(term.word)) {
                    char[] chars = term.word.toCharArray();
                    int tmpStart = start;
                    for (int i = 0 ; i < chars.length; i++) {
                        HashSet<String> similarWords = similarCalculator.getSimilarWord(chars[i] + "");
                        for (String modifiedWord : similarWords) {
                            Triple triple = new ImmutableTriple(start, 1, modifiedWord);
                            changeAllList.add(triple);
                        }
                        tmpStart++;
                    }
                }
                else {
                    HashSet<String> similarWords = similarCalculator.geSimilarInfo(term.word);
                    for (String modifiedWord : similarWords) {
                        Triple triple = new ImmutableTriple(start, term.word.length(), modifiedWord);
                        changeAllList.add(triple);
                    }
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
            candidates.add(sighanData.getValue().calCorrectContent());

            candidateList.put(sighanData.getKey(), candidates);
        }

        return candidateList;
    }


    // lm model get top 20
    private ArrayList<String> mergeAndBasicChoose(SighanDataBean data, ArrayList<Triple<Integer, Integer, List<String>>> changeList, int limit) {

        ArrayList<Pair<String, Double>> mergeResult = new ArrayList<>();
        RecursiveReplace(data.getContent(), 0, changeList, 0, mergeResult, limit);

        ArrayList<String> result = new ArrayList<>();
        for (Pair<String, Double> pair : mergeResult) result.add(pair.getKey());
        return result;
    }


    private void RecursiveReplace(String prevStr, int lastChange,
                                  ArrayList<Triple<Integer, Integer, List<String>>> replaceList,
                                  int curIndex,
                                  ArrayList<Pair<String, Double>> list,
                                  int limit) {
        if (curIndex >= replaceList.size())
            return;
        Triple<Integer, Integer, List<String>> triplet = replaceList.get(curIndex);
        if (triplet.getLeft() <= lastChange) {
            RecursiveReplace(prevStr, lastChange, replaceList, curIndex + 1, list, limit);
        }
        else {
            RecursiveReplace(prevStr, lastChange, replaceList, curIndex + 1, list, limit);
            char[] charBuffer = prevStr.toCharArray();
            for (String target: triplet.getRight()) {
                for (int i = 0; i < triplet.getMiddle(); ++i) {
                    charBuffer[triplet.getLeft() + i] = target.charAt(i);
                }
                String curStr = String.valueOf(charBuffer);
                double score = lmManager.calLM(curStr);
                if (list.size() < limit) list.add(new ImmutablePair<>(curStr, score));
                else if (score >= list.get(list.size() - 1).getValue()) {
                    list.add(new ImmutablePair<>(curStr, score));
                    list.sort(new Comparator<Pair<String, Double>>() {
                        @Override
                        public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                            if (o1.getValue() - o2.getValue() > 0) return 1;
                            else return -1;
                        }
                    });
                }
                RecursiveReplace(curStr, triplet.getLeft() + triplet.getMiddle() - 1, replaceList, curIndex + 1, list, limit);
            }
        }
    }

    private HashMap<String, ArrayList<FeatureData>> getFeature(HashMap<String, SighanDataBean> sighanDataMap,
                                                               HashMap<String, ArrayList<String>> candidatesList) {
        HashMap<String, ArrayList<FeatureData>> data = new HashMap<>();
        for (HashMap.Entry<String, ArrayList<String>> entry : candidatesList.entrySet()) {
            SighanDataBean sighanData = sighanDataMap.get(entry.getKey());
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

    private void showTestResult(HashMap<String, String> predictList, HashMap<String, SighanDataBean> inputData) {
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
