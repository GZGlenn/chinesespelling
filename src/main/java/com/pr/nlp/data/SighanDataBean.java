package com.pr.nlp.data;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import javafx.util.Pair;
import jdk.nashorn.internal.parser.JSONParser;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

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
        this(idStr, content, -1, "", "");
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

    public String calCorrectContent() {
        String prefix = content.substring(0, location);
        String posfix = content.substring(location + errorStr.length(), content.length());
        return prefix + this.correctStr + posfix;
    }

    public static SighanDataBean parseData(String line) {
        try {
            SighanDataBean sighanDataBean = new SighanDataBean("", "");
            JSONObject jsonObj = JSONObject.fromObject(line);
            sighanDataBean.idStr = (String) jsonObj.getOrDefault("idStr", "");
            sighanDataBean.content = (String) jsonObj.getOrDefault("content", "");
            sighanDataBean.location = (int) jsonObj.getOrDefault("location", -1);
            sighanDataBean.correctStr = (String) jsonObj.getOrDefault("correctStr", "");
            sighanDataBean.errorStr = (String) jsonObj.getOrDefault("errorStr", "");
            return sighanDataBean;
        } catch (Exception e) {
            System.out.println("parser sighanData error:" + e.getMessage());
            System.out.println(line);
            return null;
        }
    }

    @Override
    public String toString() {
        JSONObject jsonObject = JSONObject.fromObject(this);
        return jsonObject.toString();
    }

    public String show() {
        return "SighanDataBean{" +
                "idStr='" + idStr + '\'' +
                ", content='" + content + '\'' +
                ", location=" + location +
                ", errorStr='" + errorStr + '\'' +
                ", correctStr='" + correctStr + '\'' +
                '}';

    }
}
