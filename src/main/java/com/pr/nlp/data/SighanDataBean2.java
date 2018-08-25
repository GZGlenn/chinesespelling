package com.pr.nlp.data;

import com.alibaba.fastjson.JSONObject;
import com.hankcs.hanlp.HanLP;
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

    public void addCorrectTriplet(ImmutableTriple<Integer, String, String> triple) {
        this.correctTriplet.add(triple);
    }

    public void addCorrectTriplet(ArrayList<ImmutableTriple<Integer, String, String>> tripleList) {
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

        System.out.println(this.getIdStr() + " ==> " + this.content);

        String correctContent = this.content;
        int deltaLen = 0;
        for (Triple<Integer, String, String> triple : correctTriplet) {
            System.out.println(triple.getLeft() + " ==> " + triple.getMiddle() + " ==> " + triple.getRight());
            String prefix = correctContent.substring(0, Math.max(0, triple.getLeft() - 1 - deltaLen));
            String posfix = correctContent.substring(triple.getLeft() + triple.getMiddle().length() - 1 - deltaLen, correctContent.length());
            deltaLen += triple.getMiddle().length() - triple.getRight().length();
            correctContent = prefix + triple.getRight() + posfix;
        }

        this.correctContent = correctContent;
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

        List<String> sentenceList = SentencesUtil.toSentenceList(data.content);
        if (sentenceList.size() == 1) {
            result.add(data);
            return result;
        }

        else {
            int starLen = 0;
            for (int i = 0 ; i < sentenceList.size(); i++) {
                SighanDataBean2 tmpData = new SighanDataBean2(data.idStr + "#" + i, sentenceList.get(i));
                ArrayList<Triple<Integer, String, String>> tripleList = new ArrayList<>();
                for (Triple<Integer, String, String> triple : data.getCorrectTriplet()) {
                    if (triple.getLeft() < starLen || triple.getLeft() > starLen + sentenceList.get(i).length()) continue;
                    Triple<Integer, String, String> newTriplet = new ImmutableTriple<>(triple.getLeft() - starLen, triple.getMiddle(), triple.getRight());
                    tripleList.add(newTriplet);
                }

                tmpData.setCorrectTriplet(tripleList);
                result.add(tmpData);
            }
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
