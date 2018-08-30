package com.pr.nlp.driver;

import com.hankcs.hanlp.utility.SentencesUtil;
import com.pr.nlp.data.SighanDataBean;
import com.pr.nlp.data.SighanDataBean2;
import com.pr.nlp.util.FileUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TestSentenceSegment {

    public static void main(String[] args) {
        String str = "因为我好想证明自己，我对他说：\"所以拼命去找很大的交易机会\"，结果检了合同后，才发现自己被骗，总价值和痛快到新台币五亿元。";
        List<String> sentenceList = SentencesUtil.toSentenceList(str);
        for(String ss : sentenceList) {
            System.out.println(ss);
        }

        String path = "/home/public/code/chinese_spelling/chinesespelling/data/";
        ArrayList<String> filePathes = FileUtil.getFiles(path);
        HashMap<String, SighanDataBean2> dataList = new HashMap<>();
        for (String filePath : filePathes) {
            ArrayList<String> lines = FileUtil.readFileByLine(filePath);
            for (String line : lines) {
                ArrayList<SighanDataBean2> dataBeanList = SighanDataBean2.parseData2(line);
                for (SighanDataBean2 dataBean : dataBeanList) {
                    if (!dataList.containsKey(dataBean.getIdStr())) {
                        dataList.put(dataBean.getIdStr(), dataBean);
                    } else {
                        SighanDataBean2 oldData = dataList.get(dataBean.getIdStr());
                        oldData.addCorrectTriplet(dataBean.getCorrectTriplet());
                        dataList.put(dataBean.getIdStr(), oldData);
                    }
                }
            }
        }

        for (HashMap.Entry<String, SighanDataBean2> entry : dataList.entrySet()) {
            entry.getValue().sortCorrectTriplet();
            entry.getValue().calCorrectContent();
        }

        System.out.println("finish");
    }
}
