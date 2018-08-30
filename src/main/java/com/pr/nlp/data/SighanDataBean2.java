package com.pr.nlp.data;

import com.alibaba.fastjson.JSONObject;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;
import com.hankcs.hanlp.utility.SentencesUtil;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SighanDataBean2 implements Serializable {

    private String idStr;
    private String content;
    private String correctContent;
    private ArrayList<Triple<Integer, String, String>> correctTriplet;


    public SighanDataBean2(String idStr, String content, ArrayList<Triple<Integer, String, String>> correctTriplet) {
        this.idStr = idStr;
        this.content = content;
        this.correctTriplet = correctTriplet;
        calCorrectContent();
    }

    public SighanDataBean2(String idStr, String content) {
        this(idStr, content, new ArrayList<>(0));
    }

    public String getIdStr() {
        return idStr;
    }

    public String getContent() { return content; }

    public String getCorrectContent() {
        return correctContent;
    }

    public ArrayList<Triple<Integer, String, String>> getCorrectTriplet() {
        return correctTriplet;
    }

    public void setIdStr(String idStr) {
        this.idStr = idStr;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCorrectContent(String correctContent) {
        this.correctContent = correctContent;
    }

    public void setCorrectTriplet(ArrayList<Triple<Integer, String, String>> correctTriplet) {
        this.correctTriplet = correctTriplet;
    }

    public void addCorrectTriplet(Triple<Integer, String, String> triple) {
        this.correctTriplet.add(triple);
    }

    public void addCorrectTriplet(ArrayList<Triple<Integer, String, String>> tripleList) {
        this.correctTriplet.addAll(tripleList);
    }

    public void sortCorrectTriplet() {
        Comparator<Triple<Integer, String, String>> comparator = new Comparator<Triple<Integer, String, String>>() {
            @Override
            public int compare(Triple<Integer, String, String> o1, Triple<Integer, String, String> o2) {
                if (o2.getLeft() < o1.getLeft()) return 1;
                else if (o2.getLeft() == o1.getLeft()) return 0;
                else return -1;
            }
        };

        this.correctTriplet.sort(comparator);
    }

    public String calCorrectContent() {
        if (correctTriplet.isEmpty()) {
            this.correctContent = this.content;
            return "";
        }


        String correctContent = this.content;
        System.out.println(content);
        int deltaLen = 0;
        for (Triple<Integer, String, String> triple : correctTriplet) {
            System.out.println(triple.getLeft() + " : " + triple.getMiddle() + " : " + triple.getRight());
            System.out.println(Math.max(0, triple.getLeft() - deltaLen) + " : " + Math.max(0, triple.getLeft() + triple.getMiddle().length() - deltaLen) + " : " + correctContent.length());
            String prefix = correctContent.substring(0, Math.max(0, triple.getLeft() - deltaLen));
            String posfix = correctContent.substring(Math.max(0, triple.getLeft() + triple.getMiddle().length() - deltaLen), correctContent.length());
            deltaLen += triple.getMiddle().length() - triple.getRight().length();

            correctContent = prefix + triple.getRight() + posfix;
        }

        this.correctContent = correctContent;

//        System.out.println(content + " ==> " + correctContent);

        return correctContent;
    }

    public static SighanDataBean2 parseData(String line) {
        try {
            SighanDataBean2 sighanDataBean = new SighanDataBean2("", "");
            JSONObject jsonObj = JSONObject.parseObject(line);
            sighanDataBean.idStr = (String) jsonObj.getOrDefault("idStr", "");
            sighanDataBean.content = (String) jsonObj.getOrDefault("content", "");
            Integer location = (int) jsonObj.getOrDefault("location", -1);
            String correctStr = (String) jsonObj.getOrDefault("correctStr", "");
            String errorStr = (String) jsonObj.getOrDefault("errorStr", "");

            int deltaLoc = 0;
            while (location + errorStr.length() >= sighanDataBean.content.length()) location--;
            while (!errorStr.equals(sighanDataBean.content.substring(location + deltaLoc, location + deltaLoc + errorStr.length()))) {
                if (deltaLoc <= errorStr.length() && location + deltaLoc + errorStr.length() < sighanDataBean.content.length()) deltaLoc++;
                else break;
            }

            if (location + deltaLoc + errorStr.length() > sighanDataBean.content.length() ||
                    !errorStr.equals(sighanDataBean.content.substring(location + deltaLoc, location + deltaLoc + errorStr.length()))) {
                deltaLoc = 0;
                while (!errorStr.equals(sighanDataBean.content.substring(location - deltaLoc, location - deltaLoc + errorStr.length()))) {
                    if (deltaLoc <= errorStr.length() && location - deltaLoc > 0) deltaLoc++;
                    else break;
                }
            }

            location = location - deltaLoc;

            ImmutableTriple<Integer, String, String> triplet = new ImmutableTriple<>(location, errorStr, correctStr);
            sighanDataBean.addCorrectTriplet(triplet);
            return sighanDataBean;

        } catch (Exception e) {
            System.out.println("parser sighanData error:" + e.getMessage());
            System.out.println(line);
            return null;
        }
    }

    public static ArrayList<SighanDataBean2> parseData2(String line) {
        SighanDataBean2 data = parseData(line);
        ArrayList<SighanDataBean2> result = new ArrayList<>();

        if (data.getIdStr().equals("B2-3764-1")) {
//            System.out.println(12313);
        }

        List<String> sentenceList = getSmallSentence(data.content);
        if (sentenceList.size() == 1) {
            result.add(data);
            return result;
        }

        else {
            int starLen = 0;
            for (int i = 0 ; i < sentenceList.size(); i++) {
                int preLen = 0;
                while(starLen + preLen < data.content.length() &&
                        HanLP.segment("" + data.content.charAt(starLen + preLen)).get(0).nature.startsWith("w")) {
                    preLen++;
                }
                starLen += preLen;
                SighanDataBean2 tmpData = new SighanDataBean2(data.idStr + "#" + i, sentenceList.get(i));
                if (preLen >= sentenceList.get(i).length()) {
                    result.add(tmpData);
                    continue;
                }
                ArrayList<Triple<Integer, String, String>> tripleList = new ArrayList<>();
                for (Triple<Integer, String, String> triple : data.getCorrectTriplet()) {
                    if (triple.getLeft() < starLen || triple.getLeft() >= starLen + sentenceList.get(i).length()) continue;
                    Triple<Integer, String, String> newTriplet = new ImmutableTriple<>(triple.getLeft() - starLen, triple.getMiddle(), triple.getRight());
                    tripleList.add(newTriplet);
                }

                tmpData.setCorrectTriplet(tripleList);
                result.add(tmpData);
                starLen += tmpData.content.length();
            }

            return result;
        }
    }

    private static ArrayList<String> getSmallSentence(String sentence) {
        List<String> sentenceList = SentencesUtil.toSentenceList(sentence);
        ArrayList<String> result = new ArrayList<>();
        for (String str : sentenceList) {
            List<Term> termList= HanLP.segment(str);
            ArrayList<List<Term>> subSentence = new ArrayList<>();
            RecursiveDivide(termList, subSentence);
            for (List<Term> terms : subSentence) {
                String subSen = "";
                for (Term term : terms) {
                    subSen += term.word;
                }
                result.add(subSen);
            }
        }

        return result;
    }

    private static void RecursiveDivide(List<Term> data, ArrayList<List<Term>> result) {
        if (data.size() < 10000) result.add(data);
        else {
            int leftLen = data.size() / 2;
            List<Term> left = new ArrayList<>();
            for (int i = 0 ; i < leftLen ; i++) {
                left.add(data.get(i));
            }
            RecursiveDivide(left, result);

            List<Term> right = new ArrayList<>();
            for (int i = leftLen ; i < data.size() ; i++) {
                right.add(data.get(i));
            }
            RecursiveDivide(right, result);
        }
    }


    @Override
    public String toString() {
        JSONObject jsonObject = JSONObject.parseObject(this.show());
        return jsonObject.toString();
    }

    public String show() {
        return "SighanDataBean{" +
                "idStr='" + idStr + '\'' +
                ", content='" + content + '\'' +
                ", correctTriplet=" + correctTriplet.toString() +
                ", correctContent='" + correctContent + '\'' +
                '}';

    }
}
