package com.pr.nlp.data;

import com.alibaba.fastjson.JSONObject;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SighanDataBean implements Serializable {

    private String idStr;
    private String content;
    private String correctContent;
    private int location;
    private String errorStr;
    private String correctStr;

    private ArrayList<ArrayList<ChangeData>> changeList;

    public SighanDataBean(String idStr, String content, int location, String errorStr, String correctStr) {
        this.idStr = idStr;
        this.content = content;
        this.location = location;
        this.errorStr = errorStr;
        this.correctStr = correctStr;
        this.changeList = new ArrayList<>();
        this.correctContent = calCorrectContent();
    }

    public SighanDataBean(String idStr, String content) {
        this(idStr, content, 0, "", "");
    }

    public String getIdStr() {
        return idStr;
    }

    public String getContent() { return content; }

    public int getLocation() {
        return location;
    }

    public String getErrorStr() {
        return errorStr;
    }

    public String getCorrectStr() {
        return correctStr;
    }

    public String getCorrectContent() {
        return correctContent;
    }

    public ArrayList<ArrayList<ChangeData>> getChangeList() {
        return changeList;
    }

    public void setIdStr(String idStr) {
        this.idStr = idStr;
    }

    public void setLocation(int location) { this.location = location; }

    public void setErrorStr(String errorStr) {
        this.errorStr = errorStr;
    }

    public void setCorrectStr(String correctStr) {
        this.correctStr = correctStr;
    }

    public void setChangeList(ArrayList<ArrayList<ChangeData>> changeList) {
        this.changeList = changeList;
        for (int i = 0 ; i < this.changeList.size(); i++) {
            this.changeList.get(i).sort(new Comparator<ChangeData>() {
                @Override
                public int compare(ChangeData o1, ChangeData o2) {
                    return o1.getStartInd() - o2.getStartInd();
                }
            });
        }
    }

    public void addChangeDataArray(ArrayList<ChangeData> dataList) {
        changeList.add(dataList);
    }

    public String calCorrectContent() {
        if (errorStr.isEmpty()) return "";
        String prefix = content.substring(0, Math.max(0, location - 1));
        String posfix = content.substring(location + errorStr.length() - 1, content.length());
        return prefix + this.correctStr + posfix;
    }

    public String calCandidateContent(ArrayList<ChangeData> list) {

        String changeContent = content;
        int deltaLen = 0;
        for (ChangeData changeData : list) {
            String prefix = changeContent.substring(0, changeData.getStartInd() + deltaLen);
            String posfix = changeContent.substring(changeData.getEndInd() + deltaLen, changeContent.length());
            changeContent = prefix + changeData.getChangeStr() + posfix;
            deltaLen = changeContent.length() - content.length();
        }

        return changeContent;
    }

    public static SighanDataBean parseData(String line) {
        try {
            SighanDataBean sighanDataBean = new SighanDataBean("", "");
            JSONObject jsonObj = JSONObject.parseObject(line);
            sighanDataBean.idStr = (String) jsonObj.getOrDefault("idStr", "");
            sighanDataBean.content = (String) jsonObj.getOrDefault("content", "");
            sighanDataBean.location = (int) jsonObj.getOrDefault("location", -1);
            sighanDataBean.correctStr = (String) jsonObj.getOrDefault("correctStr", "");
            sighanDataBean.errorStr = (String) jsonObj.getOrDefault("errorStr", "");

            char locChar = sighanDataBean.content.charAt(sighanDataBean.location);
            for (int i = 0 ; i < sighanDataBean.correctStr.length() ; i++) {
                if (sighanDataBean.correctStr.charAt(i) == locChar) {
                    sighanDataBean.location -= i;
                    break;
                }
            }

            int start = sighanDataBean.location - 1;
            int end = sighanDataBean.errorStr.length() + start;
            ChangeData changeData = new ChangeData(start, end, sighanDataBean.correctStr);
            ArrayList<ChangeData> dataList = new ArrayList<>();
            dataList.add(changeData);
            sighanDataBean.addChangeDataArray(dataList);
            System.out.println(line);
            sighanDataBean.correctContent = sighanDataBean.calCorrectContent();
            return sighanDataBean;
        } catch (Exception e) {
            System.out.println("parser sighanData error:" + e.getMessage());
            System.out.println(line);
            return null;
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
                ", location=" + location +
                ", errorStr='" + errorStr + '\'' +
                ", correctStr='" + correctStr + '\'' +
                ", correctContent='" + correctContent + '\'' +
                '}';

    }
}
