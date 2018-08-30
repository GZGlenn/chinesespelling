package com.pr.nlp.driver;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.synonym.SynonymHelper;
import com.pr.nlp.data.SighanDataBean;
import com.pr.nlp.method.BasicMethod2;
import com.pr.nlp.method.BasicMethod3;
import com.pr.nlp.util.FileUtil;

import java.util.ArrayList;
import java.util.stream.StreamSupport;

public class TestDriver {

    public static void main(String[] args) {
        String trainPath = "/home/public/code/chinese_spelling/chinesespelling/data/";
        String lmPath = "/home/public/dataset/nlp/GNLP/ngram/output_clean/";
        String lmTime = "20180818-111108";
        String simPath = "/home/public/code/chinese_spelling/chinesespelling/src/main/resource/";
        String word2vecPath = "/home/glenn/IdeaProjects/wordEmbedding_model/20180803-002633_chinese_vectors.txt";
        String outputPath = "/home/public/code/chinese_spelling/chinesespelling/model/";

        BasicMethod3 method = new BasicMethod3(outputPath);
        method.initLMManager(lmPath, lmTime);
        method.initSimCalculator(simPath, word2vecPath);
        method.train(trainPath);

    }
}
