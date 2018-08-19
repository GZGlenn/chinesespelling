package com.pr.nlp.method;

import com.pr.nlp.data.*;
import com.pr.nlp.manager.LanguageModelManager;
import com.pr.nlp.manager.WordSimilarCalculator2;
import com.pr.nlp.model.MSMOReg;
import com.pr.nlp.util.FileUtil;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.HashMap;

public class BasicMethod2 {


    private String outputRoot = "";

    private LanguageModelManager lmManager;
    private WordSimilarCalculator2 similarCalculator;

    private MSMOReg model;


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


    public void train(String trainFile) {

        // read data
        ArrayList<SighanDataBean> trainData = readTrainData(trainFile);

        // create more sample
//        createCandidate(trainData);
//
//        // get feature
//        HashMap<String, ArrayList<FeatureData>> data = getFeature(trainData);
//
//        // train model (is need correct)
//        Instances instances = formatFeature(data);
//        this.model = new MSMOReg();
//        model.trainModel(instances);
    }

    private ArrayList<SighanDataBean> readTrainData(String path) {
        ArrayList<String> lines = FileUtil.getFiles(path);
        ArrayList<SighanDataBean> dataList = new ArrayList<>();
        for (String line : lines) {
            SighanDataBean dataBean = SighanDataBean.parseData(line);
            dataList.add(dataBean);



        }
        return dataList;
    }

    // create candidate by pmi and lm
//    private void createCandidate(ArrayList<SighanDataBean> trainData,
//                                 HashMap<String, LMStatisticWordBean> lmModel,
//                                 HashMap<String, PMIWordPairBean> pmiModel) {
//        for (SighanDataBean sighanData: trainData) {
//            List<Term> termList = HanLP.segment(sighanData.getContent());
//            for (Term term : termList) {
//                ArrayList<String> similarWord = getSimilarWord(term.word);
//
//            }
//
//            // basic choose
//        }
//    }
//
//    private HashMap<String, ArrayList<FeatureData>> getFeature(ArrayList<SighanDataBean> candidates) {
//        HashMap<String, ArrayList<FeatureData>> data = new HashMap<>();
//        for (SighanDataBean candidate : candidates) {
//            ArrayList<FeatureData> featureList = new ArrayList<>();
//            for (int ind = 0 ; ind < candidate.getChangeList().size(); ind++) {
//                FeatureData feature = new FeatureData();
//                feature.calFeature(candidate, ind, lmManager);
//                featureList.add(feature);
//            }
//
//            ArrayList<ChangeData> gt = new ArrayList<>();
//            ChangeData cdata = new ChangeData(candidate.getLocation(), candidate.getLocation() + candidate.getErrorStr().length(), candidate.getCorrectStr());
//            gt.add(cdata);
//            FeatureData feature = new FeatureData();
//            feature.calFeature(candidate.getContent(), candidate.getCorrectContent(), gt, pmiModel, lmModel, wordModel);
//            featureList.add(feature);
//            data.put(candidate.getIdStr(), featureList);
//        }
//        return data;
//    }

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



}
