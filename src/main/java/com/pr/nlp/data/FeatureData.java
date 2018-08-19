package com.pr.nlp.data;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.pr.nlp.manager.LanguageModelManager;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
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

    public void calFeature(SighanDataBean data, int candidateIdx, LanguageModelManager lmManager) {

        String changeContent = data.getContent();
        ArrayList<Pair<Integer, Integer>> modifiedPos = new ArrayList<>();
        int deltaLen = 0;
        for (ChangeData changeData : data.getChangeList().get(candidateIdx)) {
            String prefix = changeContent.substring(0, changeData.getStartInd() + deltaLen);
            String posfix = changeContent.substring(changeData.getEndInd() + deltaLen, changeContent.length());
            changeContent = prefix + changeData.getChangeStr() + posfix;
            modifiedPos.add(new ImmutablePair<>(prefix.length(), prefix.length() + changeData.getChangeStr().length()));
            deltaLen = changeContent.length() - data.getContent().length();
        }


        List<Term> modifiedTermList = HanLP.segment(changeContent);

        this.lmfeat = lmManager.calLM(data.getContent()) / lmManager.calLM(changeContent);
        this.pmifeat = calPMIFeat(changeContent, modifiedPos, lmManager);
        this.wordNum = modifiedTermList.size();

        if (data.getCorrectContent() == changeContent) this.label = 1;
        else this.label = -1;

    }

    private double calPMIFeat(String content, ArrayList<Pair<Integer, Integer>> modifiedPos,
                       LanguageModelManager manager) {

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
                pmiVal += manager.calPMI(changeTermList.get(termIndex).word, changeTermList.get(j).word);
            }
        }

        pmiVal = pmiVal / (modifiedPos.size() * changeTermList.size() * 1.0);
        return pmiVal;
    }
}
