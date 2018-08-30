package com.pr.nlp.manager;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.utility.SentencesUtil;
import com.pr.nlp.data.SighanDataBean2;
import com.pr.nlp.util.NLPUtil;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class CandidatgeGenerator {

    public static ArrayList<String> getCandidate(String input, int wordLimit, int sentenceLimit, LanguageModelManager2 lmmodel, WordSimilarCalculator simCalculator) {

        ArrayList<String> result = new ArrayList<>();

        Pair<Integer, Integer> headTailNoUseInfo = getHeadTailNoUsePos(input);

        if (headTailNoUseInfo.getLeft() < headTailNoUseInfo.getRight()) {

            String dealInput = input.substring(headTailNoUseInfo.getLeft(), headTailNoUseInfo.getRight() + 1);
            List<Term> termList = HanLP.segment(dealInput);
            ArrayList<List<Term>> smallSegmentList = getSmallSentence(termList);
            ArrayList<String> strBetweenSmallSeg = getStrBetweenSeg(termList);
            ArrayList<ArrayList<String>> modifiedSegmentList = new ArrayList<>();

            for (int i = 0; i < smallSegmentList.size(); i++) {
                ArrayList<String> candidateList = getSentenceCandidate(smallSegmentList.get(i), lmmodel, simCalculator, wordLimit, sentenceLimit);
                modifiedSegmentList.add(candidateList);
            }

            ArrayList<Pair<String, Double>> resultWithScore = new ArrayList<>();

            Comparator<Pair<String, Double>> comparator = new Comparator<Pair<String, Double>>() {
                @Override
                public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                    return -1 * o1.getRight().compareTo(o2.getRight());
                }
            };

            double originScore = lmmodel.calLM(termList);

            RecursiveReplace("", modifiedSegmentList, resultWithScore, strBetweenSmallSeg, comparator,
            originScore, lmmodel, 0, sentenceLimit);

            String head = "", tail = "";
            for (int h = 0 ; h < headTailNoUseInfo.getLeft(); h++) head += input.charAt(h);
            for (int t = input.length() - 1; t > headTailNoUseInfo.getRight(); t--) tail = input.charAt(t) + tail;

            for (Pair<String, Double> pair : resultWithScore) {
                String output = head + pair.getLeft() + tail;
                result.add(output);
            }
        }

        return result;
    }

    private static Pair<Integer, Integer> getHeadTailNoUsePos(String input) {
        char[] charList = input.toCharArray();
        int left = 0, right = input.length() - 1;
        while (left < input.length() && NLPUtil.isChinesePunctuation(charList[left])) left++;
        while (right >= 0 && NLPUtil.isChinesePunctuation(charList[right])) right--;
        return new ImmutablePair<Integer, Integer>(left, right);
    }

    private static ArrayList<List<Term>> getSmallSentence(List<Term> termList) {
        ArrayList<List<Term>> result = new ArrayList<>();

        List<Term> tmpStr = new ArrayList<>();
        for (int i = 0 ; i < termList.size() ; i++) {
            if (termList.get(i).nature.startsWith("w")) {
                if (!tmpStr.isEmpty()) result.add(new ArrayList<>(tmpStr));
                tmpStr = new ArrayList<>();
            }
            else {
                tmpStr.add(termList.get(i));
            }
        }

        if (!tmpStr.isEmpty()) result.add(tmpStr);

        return result;
    }


    private static ArrayList<String> getStrBetweenSeg(List<Term> termList) {
        ArrayList<String> result = new ArrayList<>();

        int index = 0;
        while (index < termList.size()) {
            while (index < termList.size() && !termList.get(index).nature.startsWith("w")) index++;
            String tmpStr = "";
            while(index < termList.size() && termList.get(index).nature.startsWith("w")) {
                tmpStr += termList.get(index).word;
                index++;
            }
            result.add(tmpStr);
        }

        if (result.size() == 0) result.add("");

        return result;
    }

    private static ArrayList<String> getSentenceCandidate(List<Term> termList, LanguageModelManager2 lmmodel,
                                                          WordSimilarCalculator calculator, int wordLimit, int sentenceLimit) {
        Comparator<Pair<String, Float>> wordScoreComparator = new Comparator<Pair<String, Float>>() {
            @Override
            public int compare(Pair<String, Float> o1, Pair<String, Float> o2) {
                if (o2.getValue() > o1.getValue()) return 1;
                else if (o2.getValue() == o1.getValue()) return 0;
                else return -1;
            }
        };

        int cutLen = termList.size();
        while (cutLen > 10) {
            wordLimit = wordLimit / 2;
            cutLen = cutLen / 2;
        }

        String originData = "";
        for (Term term : termList) originData += term.word;

        ArrayList<Triple<Integer, Integer, List<String>>> changeAllList = new ArrayList<>();
        int start = 0;
        for (int i = 0 ; i < termList.size() ; i++) {
            Term term = termList.get(i);
            if (term.nature.startsWith("w")) {
                start += term.word.length();
                continue;
            }
            else if (!calculator.isContainWord(term.word) && term.word.length() <= 3) {
                char[] chars = term.word.toCharArray();
                for (int j = 0 ; j < chars.length; j++) {
                    HashSet<String> similarWords = calculator.getSimilarWord(chars[j] + "");
                    ArrayList<String> addWords = new ArrayList<>();
                    for (String str : similarWords) {
                        if (lmmodel.isContain(str) && lmmodel.getUnigram(str) > -6) {
                            addWords.add(str);
                        }
                    }

                    if (addWords.size() != 0 && addWords.size() <= wordLimit) {
                        Triple triple = new ImmutableTriple(start + j, 1, new ArrayList<>(addWords));
                        changeAllList.add(triple);
                    } else if (addWords.size() != 0) {
                        ArrayList<Pair<String, Float>> scoreAddWord = new ArrayList<>();
                        for (String str : addWords) scoreAddWord.add(new ImmutablePair<>(str, lmmodel.getUnigram(str)));
                        scoreAddWord.sort(wordScoreComparator);
                        ArrayList<String> result = new ArrayList<>();
                        for (int k = 0 ; k < wordLimit ; k++) {
                            result.add(scoreAddWord.get(k).getKey());
                        }
                        Triple triple = new ImmutableTriple(start + j, 1, new ArrayList<>(result));
                        changeAllList.add(triple);
                    }
                }
            }
            else {
                String beforeWord = "", afterWord = "";
                if (i == 0) beforeWord = "<s>";
                if (i != 0 && !termList.get(i-1).nature.startsWith("w")) beforeWord = termList.get(i-1).word;
                if (i != termList.size() - 1 && !termList.get(i+1).nature.startsWith("w")) afterWord = termList.get(i+1).word;
                if (i == termList.size() - 1) afterWord = "</s>";

                HashSet<String> similarWords = calculator.geSimilarInfo(term.word);
                double originScore = calPMIInThreeWin(lmmodel, beforeWord, term.word, afterWord);
                ArrayList<String> addWords = new ArrayList<>();
                for (String str : similarWords) {
                    if (str.equals(term.word)) continue;
                    double modifiedScore = calPMIInThreeWin(lmmodel, beforeWord, str, afterWord);
                    if (lmmodel.isContain(str) && modifiedScore >= originScore) {
                        addWords.add(str);
                    }
                }
                if (addWords.isEmpty()) {
                    start += term.word.length();
                    continue;
                }
                else if (addWords.size() <= wordLimit) {
                    Triple triple = new ImmutableTriple(start, term.word.length(), new ArrayList<>(addWords));
                    changeAllList.add(triple);
                }
                else {
                    ArrayList<Pair<String, Float>> scoreAddWord = new ArrayList<>();
                    for (String str : addWords) scoreAddWord.add(new ImmutablePair<>(str, lmmodel.getUnigram(str)));
                    scoreAddWord.sort(wordScoreComparator);
                    ArrayList<String> result = new ArrayList<>();
                    for (int k = 0 ; k < wordLimit ; k++) {
                        result.add(scoreAddWord.get(k).getKey());
                    }
                    Triple triple = new ImmutableTriple(start, term.word.length(), new ArrayList<>(result));
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


        ArrayList<String> candidates = mergeAndBasicChoose(originData, changeAllList, lmmodel, sentenceLimit);

        return candidates;
    }

    private static double calPMIInThreeWin(LanguageModelManager2 lmmodel, String left, String middle, String right) {
        double pmi = 0, num = 0;
        if (!left.isEmpty()) {
            pmi += lmmodel.calPMI(left, middle);
            num++;
        }
        if (!right.isEmpty()) {
            pmi += lmmodel.calPMI(middle, right);
            num++;
        }

        if (num > 0) pmi = pmi / num;
        return pmi;
    }


    // lm model get top 20
    private static ArrayList<String> mergeAndBasicChoose(String content,
                                                         ArrayList<Triple<Integer, Integer, List<String>>> changeList,
                                                         LanguageModelManager2 lmmodel,
                                                         int limit) {

        ArrayList<Pair<String, Double>> mergeResult = new ArrayList<>();
        Comparator comparator = new Comparator<Pair<String, Double>>() {
            @Override
            public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                if (o2.getValue().floatValue() > o1.getValue().floatValue()) return 1;
                else if (o2.getValue().floatValue() == o1.getValue().floatValue()) return 0;
                else return -1;
            }
        };
        double originScore = lmmodel.calLM(content);
        RecursiveReplace(content, 0, changeList, 0, mergeResult, comparator, originScore, lmmodel, limit);
        ArrayList<String> result = new ArrayList<>();
        for (Pair<String, Double> pair : mergeResult) result.add(pair.getKey());
        return result;
    }

    private static void RecursiveReplace(String prevStr, int lastChange,
                                  ArrayList<Triple<Integer, Integer, List<String>>> replaceList,
                                  int curIndex,
                                  List<Pair<String, Double>> list,
                                  Comparator<Pair<String, Double>> comparator,
                                  double originScore,
                                  LanguageModelManager2 lmmodel,
                                  int limit) {
        if (curIndex >= replaceList.size()) return;
        Triple<Integer, Integer, List<String>> triplet = replaceList.get(curIndex);
        if (triplet.getLeft() <= lastChange) {
            RecursiveReplace(prevStr, lastChange, replaceList, curIndex + 1, list, comparator, originScore, lmmodel, limit);
        }
        else {
            RecursiveReplace(prevStr, lastChange, replaceList, curIndex + 1, list, comparator, originScore, lmmodel, limit);
            char[] charBuffer = prevStr.toCharArray();
            for (String target: triplet.getRight()) {
                for (int i = 0; i < triplet.getMiddle(); ++i) {
                    charBuffer[triplet.getLeft() + i] = target.charAt(i);
                }
                String curStr = String.valueOf(charBuffer);
                double score = lmmodel.calLM(curStr);
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
                RecursiveReplace(curStr, triplet.getLeft() + triplet.getMiddle() - 1, replaceList, curIndex + 1, list, comparator, originScore, lmmodel, limit);
            }
        }
    }



    private static void RecursiveReplace(String prevStr,
                                         ArrayList<ArrayList<String>> modifiedSegmentList,
                                         ArrayList<Pair<String, Double>> result,
                                         ArrayList<String> strBetweenSmallSeg,
                                         Comparator<Pair<String, Double>> comparator,
                                         double originScore,
                                         LanguageModelManager2 lmmodel,
                                         int curIdx,
                                         int limit) {

        if (curIdx == modifiedSegmentList.size()) {
            double score = lmmodel.calLM(prevStr);
            if (result.size() < limit) {
                result.add(new ImmutablePair<>(prevStr, score));
            }
            else if (score > result.get(result.size() - 1).getRight()) {
                result.set(result.size() - 1, new ImmutablePair<>(prevStr, score));
            }
            else if (score == result.get(result.size() - 1).getRight()) {
                result.add(new ImmutablePair<>(prevStr, score));
            }

            if (result.size() >= limit) result.sort(comparator);
            return;
        }

        ArrayList<String> strList = modifiedSegmentList.get(curIdx);
        if (curIdx > 0) prevStr += strBetweenSmallSeg.get(curIdx - 1);
        for (String str : strList) {
            RecursiveReplace(prevStr + str, modifiedSegmentList, result, strBetweenSmallSeg,
                    comparator, originScore, lmmodel, curIdx + 1, limit);
        }
    }
}
