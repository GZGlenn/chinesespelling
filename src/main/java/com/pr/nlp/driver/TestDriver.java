package com.pr.nlp.driver;

import com.pr.nlp.data.SighanDataBean;
import com.pr.nlp.method.BasicMethod2;
import com.pr.nlp.util.FileUtil;

import java.util.ArrayList;
import java.util.stream.StreamSupport;

public class TestDriver {

    public static void main(String[] args) {
        String trainPath = "/Users/glenn/nlp/wordspelling/basic_method/data/sighan_train_2015.txt";
        String lmPath = "/home/public/dataset/nlp/GNLP/ngram/output_clean/";
        String lmTime = "20180818-111108";
        String simPath = "/home/public/code/chinese_spelling/chinesespelling/src/main/resource/";
        String word2vecPath = "/home/glenn/IdeaProjects/wordEmbedding_model/20180803-002633_chinese_vectors.txt";
        String outputPath = "";

        BasicMethod2 method2 = new BasicMethod2(outputPath);
        method2.initLMManager(lmPath, lmTime);
        method2.initSimCalculator(simPath, word2vecPath);
        method2.train(trainPath);

    }
}
