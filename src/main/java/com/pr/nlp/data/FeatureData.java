package com.pr.nlp.data;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FeatureData {

    private double lmfeat;
    private double pmifeat;
    private double wordNum;

    private double label;

    public FeatureData() {
    }

    public FeatureData(double lmfeat, double pmifeat, double wordNum, double label) {
        this.lmfeat = lmfeat;
        this.pmifeat = pmifeat;
        this.wordNum = wordNum;
    }

    public double getLmfeat() {
        return lmfeat;
    }

    public double getPmifeat() {
        return pmifeat;
    }

    public double getWordNum() {
        return wordNum;
    }

    public double getLabel() {
        return label;
    }

    public void setLmfeat(double lmfeat) {
        this.lmfeat = lmfeat;
    }

    public void setPmifeat(double pmifeat) {
        this.pmifeat = pmifeat;
    }

    public void setWordNum(double wordNum) {
        this.wordNum = wordNum;
    }

    public void setLabel(double label) {
        this.label = label;
    }

    public void calFeature(String content, String correctStr,
                           ArrayList<ChangeData> changeList,
                           HashMap<String, PMIWordPairBean> pmiModel,
                           HashMap<String, LMStatisticWordBean> lmmodel,
                           HashMap<String, WordStatisticBean> unigram) {

        String changeContent = content;
        ArrayList<Pair<Integer, Integer>> modifiedPos = new ArrayList<>();
        int deltaLen = 0;
        for (ChangeData changeData : changeList) {
            String prefix = changeContent.substring(0, changeData.getStartInd() + deltaLen);
            String posfix = changeContent.substring(changeData.getEndInd() + deltaLen, changeContent.length());
            changeContent = prefix + changeData.getChangeStr() + posfix;
            modifiedPos.add(new Pair<>(prefix.length(), prefix.length() + changeData.getChangeStr().length()));
            deltaLen = changeContent.length() - content.length();
        }


        List<Term> originTermList = HanLP.segment(content);
        List<Term> modifiedTermList = HanLP.segment(changeContent);

        this.lmfeat = calLMFeat(originTermList, modifiedTermList, lmmodel, unigram);
        this.pmifeat = calPMIFeat(changeContent, modifiedPos, pmiModel);
        this.wordNum = modifiedTermList.size();

        if (correctStr == changeContent) this.label = 1;
        else this.label = -1;

    }


    private double calLMFeat(List<Term> originTermList, List<Term> modifiedTermList,
                          HashMap<String, LMStatisticWordBean> lmmodel,
                          HashMap<String, WordStatisticBean> unigram) {
        double originLMScore = calLM(originTermList, lmmodel, unigram);
        double candidateLMScore = calLM(modifiedTermList, lmmodel, unigram);
        return candidateLMScore / originLMScore;
    }

    private double calLM(List<Term> termList, HashMap<String, LMStatisticWordBean> lmmodel,
                      HashMap<String, WordStatisticBean> unigram) {
        if (termList.size() == 0) return  0;
        else {
            double lmFeat = unigram.getOrDefault(termList.get(0).word, new WordStatisticBean("")).frequency;
            for (int i = 1; i < termList.size(); i++) {
                String firstWord = termList.get(i - 1).word;
                String secondWord = termList.get(i).word;
                lmFeat *= lmmodel.getOrDefault(firstWord + "#" + secondWord, new LMStatisticWordBean("", "", 0)).getConditionalProb();
            }
            return lmFeat;
        }
    }

    private double calPMIFeat(String content, ArrayList<Pair<Integer, Integer>> modifiedPos,
                       HashMap<String, PMIWordPairBean> pmiModel) {

        double pmiVal = 0;
        List<Term> changeTermList = HanLP.segment(content);
        int termIndex = 0;
        int termPreLen = 0;
        for (Pair<Integer, Integer> pair : modifiedPos) {
            while(termPreLen < pair.getKey() && termIndex < changeTermList.size()) {
                termPreLen += changeTermList.get(termIndex++).length();
            }
            for (int j = 0; j < changeTermList.size(); j++) {
                if (j == termIndex) continue;
                if (changeTermList.get(termIndex).word.compareTo(changeTermList.get(j).word) > 0) {
                    pmiVal += pmiModel.getOrDefault(changeTermList.get(termIndex).word + "#" + changeTermList.get(j).word, new PMIWordPairBean("", "", 0.0)).getConditionalProb();
                } else {
                    pmiVal += pmiModel.getOrDefault(changeTermList.get(j).word + "#" + changeTermList.get(termIndex).word, new PMIWordPairBean("", "", 0.0)).getConditionalProb();
                }
            }
        }

        pmiVal = pmiVal / (modifiedPos.size() * changeTermList.size() * 1.0);
        return pmiVal;
    }
}
