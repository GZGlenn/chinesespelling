package com.pr.nlp.driver;

import com.pr.nlp.manager.LanguageModelManager2;

public class TestLanguageModel {
//    unigram count : 339916
//    lm count : 339916
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
//        7 = {ImmutableTriple@963} "(18,1,[郿, 浼, 摹, 摸, 酶, 嫫, 摩, 嗨, 磨, 蓦, 煤, 秣, 楣, 无, 糜])"
//        8 = {ImmutableTriple@964} "(19,1,[鱿, 囿, 幽, 幼, 陶, 杳, 右, 诱, 铫, 湫, 忧, 呦, 卣, 黝, 曜])"
//        9 = {ImmutableTriple@965} "(20,1,[衽, 任, 亻, 人, 纫, 轫, 饪, 您, 韧, 认, 稔, 荏, 忍, 妊, 刃])"
        String[] strs = "郿, 浼, 摹, 摸, 酶, 嫫, 摩, 嗨, 磨, 蓦, 煤, 秣, 楣, 无, 糜".split(",");
        for (String str : strs) {
            System.out.println(str.trim() + " ==> " + lmmodel.isContain(str.trim()) + " : " + lmmodel.getUnigram(str.trim()));
        }
    }
}
