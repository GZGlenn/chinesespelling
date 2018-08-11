package com.pr.nlp.driver;

import com.pr.nlp.data.SighanDataBean;
import com.pr.nlp.util.FileUtil;

import java.util.ArrayList;
import java.util.stream.StreamSupport;

public class TestDriver {

    public static void main(String[] args) {
        String filePath = "/Users/glenn/nlp/wordspelling/basic_method/data/sighan_train_2015.txt";

        ArrayList<String> infoList = FileUtil.readFileByLine(filePath);

        for (String info : infoList) {
            SighanDataBean dataBean = SighanDataBean.parseData(info);
            String errorStr = dataBean.getErrorStr();

            System.out.println(dataBean.getLocation() - 1);

            String checkStr = dataBean.getContent().substring(Math.max(0, dataBean.getLocation() - 1));
            System.out.println(dataBean.getIdStr() + " : " + errorStr + " == " + checkStr + " : " + errorStr.equals(checkStr));
        }
    }
}
