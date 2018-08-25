package com.pr.nlp.driver;

import com.pr.nlp.manager.LanguageModelManager2;

public class TestLanguageModel {

    public static void main(String[] args) {
        String lmPath = "/home/public/dataset/nlp/GNLP/ngram/output_clean/";
        String lmTime = "20180818-111108";
        LanguageModelManager2 lmmodel = new LanguageModelManager2(lmPath);
        lmmodel.loadModel(lmTime);

        System.out.println(lmmodel.getUnigram("印为"));
        System.out.println(lmmodel.getUnigram("我"));
        System.out.println(lmmodel.getLM("印为", "我"));
        System.out.println(lmmodel.getLM("我","印为"));

        double score = lmmodel.calPMI("印为", "我");
        double score2 = lmmodel.calPMI("印为", "<s>");
        System.out.println(score);
        System.out.println(score2);

        score = lmmodel.calPMI("因为", "我");
        score2 = lmmodel.calPMI("因为", "<s>");
        System.out.println(score);
        System.out.println(score2);
    }
}
