package com.pr.nlp.method;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.pr.nlp.data.*;
import com.pr.nlp.manager.LanguageModelManager;
import com.pr.nlp.manager.WordSimilarCalculator2;
import com.pr.nlp.model.MSMOReg;
import com.pr.nlp.util.FileUtil;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.*;

public class BasicMethod2 {


    private String outputRoot = "";
    private final String model_name = "smoreg.model";

    private LanguageModelManager lmManager;
    private WordSimilarCalculator2 similarCalculator;
    private MSMOReg model;

    private int winsize = 5;
    private int maxChangeInOneWin = 5;
    private int maxChange = 100;

    private double pmiThre = 0.3;
    private double lmThre = 0.1;

    public BasicMethod2(String outputRootn) {
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
        similarCalculator = new WordSimilarCalculator2(root);
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
        ArrayList<SighanDataBean> trainData = readFileData(trainFile);

        // create more sample
        createCandidate(trainData);

        // get feature
        HashMap<String, ArrayList<FeatureData>> data = getFeature(trainData);

        // train model (is need correct)
        Instances instances = formatFeature(data);
        this.model = new MSMOReg();
        model.trainModel(instances);
    }

    public void test(String testFile) {
        // read data
        ArrayList<SighanDataBean> testData = readFileData(testFile);

        // create more sample
        createCandidate(testData);

        // get feature
        HashMap<String, ArrayList<FeatureData>> data = getFeature(testData);

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

        ArrayList<String> output = new ArrayList<>();
        for (SighanDataBean sighanData : testData) {
            String idStr = sighanData.getIdStr();
            ArrayList<ChangeData> changeList = sighanData.getChangeList().get(result.get(idStr));
            output.add(sighanData.calCandidateContent(changeList));
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
        ArrayList<SighanDataBean> sighanList = new ArrayList<>();
        sighanList.add(new SighanDataBean("-1", input));

        // create more sample
        createCandidate(sighanList);

        // get feature
        HashMap<String, ArrayList<FeatureData>> data = getFeature(sighanList);
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

        return sighanList.get(0).calCandidateContent(sighanList.get(0).getChangeList().get(maxPreInd));
    }

    private ArrayList<SighanDataBean> readFileData(String path) {
        ArrayList<String> lines = FileUtil.getFiles(path);
        ArrayList<SighanDataBean> dataList = new ArrayList<>();
        for (String line : lines) {
            SighanDataBean dataBean = SighanDataBean.parseData(line);
            dataList.add(dataBean);
        }
        return dataList;
    }

    // create candidate by pmi and lm
    private void createCandidate(ArrayList<SighanDataBean> trainData) {
        for (SighanDataBean sighanData: trainData) {
            List<Term> termList = HanLP.segment(sighanData.getContent());
            int start = 0;
            int end = Math.min(termList.size(), start + winsize);
            int curStartPos = 0;
            int curChangePos = 0;
            ArrayList<ArrayList<ChangeData>> changeAllList = new ArrayList<>();
            do {
                ArrayList<Term> list = new ArrayList<>();
                for (int i = start; i < end; i++) {
                    list.add(termList.get(i));
                }

                // only one change in a single win
                ArrayList<ChangeData> changeList = new ArrayList<>();
                curChangePos = getChangeDataInSingleWin(list, changeList, curStartPos, curChangePos);
                if (!changeList.isEmpty()) changeAllList.add(changeList);
                if (curChangePos > 1) {
                    start += curChangePos - 2;
                    curChangePos = 2;
                }
                else {
                    start++;
                    if (curChangePos == 1) curChangePos = 0;
                    else curChangePos = -1;
                }
                curStartPos = start;
                end = Math.min(termList.size(), start + winsize);
            } while (end < termList.size());

            // basic choose
            ArrayList<ArrayList<ChangeData>> candidates = mergeAndBasicChoose(sighanData, changeAllList);
            sighanData.setChangeList(candidates);
        }
    }

    // using pmi
    private int getChangeDataInSingleWin(ArrayList<Term> termList, ArrayList<ChangeData> result, int winStart, int lastWinChgPos) {
        if (termList.size() <= 1) return 0;

        ArrayList<Pair<ChangeData, Double>> tmpResult = new ArrayList<>();

        String content = "";
        for (Term term : termList) content += term.word;

        int curStrPos = 0;
        int lastChgPos = 0;
        float lmScore = lmManager.calLM(termList);
        double originScore = lmScore + 1 / termList.size();
        for (int i = lastWinChgPos + 1 ; i < termList.size(); i++) {
            if (i != 0) curStrPos += termList.get(i-1).length();
            if (termList.get(i).nature.startsWith("w")) continue;
            if (i == 0 && termList.get(i+1).nature.startsWith("w")) continue;
            if (i == termList.size() - 1 && termList.get(i -1).nature.startsWith("w")) continue;
            if (i > 0 && i < termList.size() - 1 && termList.get(i-1).nature.startsWith("w") && termList.get(i+1).nature.startsWith("w")) continue;

            float pmi = 0;
            if (i == 0) pmi = lmManager.calPMI(termList.get(i).word, termList.get(i+1).word);
            if (i == termList.size() - 1) pmi = lmManager.calPMI(termList.get(-1).word, termList.get(i).word);
            else pmi = (lmManager.calPMI(termList.get(i).word, termList.get(i+1).word) +
                    lmManager.calPMI(termList.get(-1).word, termList.get(i).word)) / 2;

            if (pmi < pmiThre) {
                lastChgPos = i;
                int len = termList.get(i).length();
                HashSet<String> simWordList = similarCalculator.getSimilarWord(termList.get(i).word);
                for (String simWord : simWordList) {
                    String modifiedContent = content.substring(0, curStrPos) + simWord + content.substring(curStrPos + len, content.length());
                    List<Term> modifiedTermList = HanLP.segment(modifiedContent);
                    float modifiedLmScore = lmManager.calLM(modifiedTermList);
                    double modifiedScore = modifiedLmScore + 1 / modifiedTermList.size();
                    if (modifiedScore > originScore) {
                        boolean isNeedSort = false;
                        if (tmpResult.size() >= maxChangeInOneWin) {
                            if (tmpResult.get(tmpResult.size() - 1).getRight() < modifiedScore) {
                                isNeedSort = true;
                                tmpResult.set(tmpResult.size() - 1, new ImmutablePair<>(new ChangeData(winStart + curStrPos, winStart + curStrPos + len, simWord), modifiedScore));
                            }
                        } else {
                            tmpResult.add(new ImmutablePair<>(new ChangeData(winStart + curStrPos, winStart + curStrPos + len, simWord), modifiedScore));
                            if (tmpResult.size() >= maxChangeInOneWin) {
                                isNeedSort = true;
                            }
                        }

                        if (isNeedSort) {
                            tmpResult.sort(new Comparator<Pair<ChangeData, Double>>() {
                                @Override
                                public int compare(Pair<ChangeData, Double> o1, Pair<ChangeData, Double> o2) {
                                    if (o1.getRight() > o2.getRight()) return  -1;
                                    if (o1.getRight() < o2.getRight()) return 1;
                                    else return 0;
                                }
                            });
                        }
                    }
                }
            }
        }

        for (Pair<ChangeData, Double> rst : tmpResult) result.add(rst.getKey());
        return lastChgPos;
    }

    // lm model get top 20
    private ArrayList<ArrayList<ChangeData>> mergeAndBasicChoose(SighanDataBean data, ArrayList<ArrayList<ChangeData>> changeWinList) {
        ArrayList<Pair<ArrayList<ChangeData>, Double>> mergeResult = new ArrayList<>();
        for (int i = 0 ; i < changeWinList.size(); i++) {

        }

        ArrayList<ArrayList<ChangeData>> result = new ArrayList<>();
        for (int i = 0 ; i < mergeResult.size(); i++) result.add(mergeResult.get(i).getLeft());
        return result;
    }

    private void dfsChangeDataList(SighanDataBean data,
                                   ArrayList<Pair<ArrayList<ChangeData>, Double>> result,
                                   ArrayList<ArrayList<ChangeData>> changeWinList,
                                   ArrayList<ChangeData> curList, int depth) {
        if (depth >= changeWinList.size()) return;
        dfsChangeDataList(data, result, changeWinList, curList, depth+1);
        ArrayList<ChangeData> curDepthList = changeWinList.get(depth);
        for (int i = 0 ; i < curDepthList.size(); i++) {
            ArrayList<ChangeData> cloneList = (ArrayList<ChangeData>)curList.clone();
            if (!cloneList.isEmpty()) {
                String content = data.calCandidateContent(cloneList);
                double lmScore = lmManager.calLM(content);
                boolean isNeedSort = false;
                if (result.size() < maxChange) {
                    result.add(new ImmutablePair<>(cloneList, lmScore));
                    isNeedSort = true;
                } else if (result.get(result.size() - 1).getRight() < lmScore) {
                    result.set(result.size() - 1, new ImmutablePair<>(cloneList, lmScore));
                    if (result.size() >= maxChange) {
                        isNeedSort = true;
                    }
                }

                if (isNeedSort) {
                    result.sort(new Comparator<Pair<ArrayList<ChangeData>, Double>>() {
                        @Override
                        public int compare(Pair<ArrayList<ChangeData>, Double> o1, Pair<ArrayList<ChangeData>, Double> o2) {
                            if (o1.getRight() > o2.getRight()) return -1;
                            if (o1.getRight() < o2.getRight()) return 1;
                            else return 0;
                        }
                    });
                }
            }

            dfsChangeDataList(data, result, changeWinList, cloneList, depth+1);
        }
    }

    private HashMap<String, ArrayList<FeatureData>> getFeature(ArrayList<SighanDataBean> candidates) {
        HashMap<String, ArrayList<FeatureData>> data = new HashMap<>();
        for (SighanDataBean candidate : candidates) {
            ArrayList<FeatureData> featureList = new ArrayList<>();
            for (int ind = 0 ; ind < candidate.getChangeList().size(); ind++) {
                FeatureData feature = new FeatureData();
                feature.calFeature(candidate, ind, lmManager);
                featureList.add(feature);
            }

            data.put(candidate.getIdStr(), featureList);
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

    private void showTestResult(ArrayList<String> predictList, ArrayList<SighanDataBean> inputData) {
        double correctNum = 0;
        double totalNum = predictList.size();
        for (int i = 0 ; i < predictList.size(); i++) {
            if (predictList.get(i) == inputData.get(i).getCorrectStr()) {
                correctNum++;
            }

            System.out.println("result " + i);
            System.out.println("correct ==> " + inputData.get(i).getCorrectStr());
            System.out.println("predict ==> " + predictList.get(i));
        }

        System.out.println("accuracy : " +  correctNum / totalNum);
    }

}
