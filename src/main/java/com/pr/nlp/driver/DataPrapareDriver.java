package com.pr.nlp.driver;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import com.pr.nlp.data.SighanDataBean;
import com.pr.nlp.util.FileUtil;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class DataPrapareDriver {

    public static void main(String[] args) {
        String inputRoot = "/Users/glenn/nlp/wordspelling/sighan8csc_release1.0/Training/";
        String savePath = "/Users/glenn/nlp/wordspelling/basic_method/data/sighan_train_2015.txt";

        HashMap<String, SighanDataBean> sighanDataMap = new HashMap<>();
        HashMap<String, String> id2ContentMap = new HashMap<>();

        ArrayList<String> filePathes = FileUtil.getFiles(inputRoot, ".sgml");
        for (String filePath : filePathes) {
            ArrayList<String> lines = FileUtil.readFileByLine(filePath);
            for (int i = 0 ; i < lines.size() ; i++) {
                String line = ZhConverterUtil.convertToSimple(lines.get(i));
                if (line.startsWith("<PASSAGE ")) {
                    String id = line.substring(line.indexOf("id=") + 4, line.indexOf(">") - 1);
                    if (!id2ContentMap.containsKey(id)) {
                        String content = line.substring(line.indexOf(">") + 1, line.substring(1).indexOf("<") + 1);
                        id2ContentMap.put(id, content);
//                        System.out.println(id + " ==> " + content);
                    }
                }

                else if (line.startsWith("<MISTAKE ")) {
                    String id = line.substring(line.indexOf("id=") + 4, line.indexOf("\" "));
                    String location = line.substring(line.indexOf("location=") + 10, line.indexOf("\">"));
                    String errorStr = "";
                    String correctStr = "";

                    line = ZhConverterUtil.convertToSimple(lines.get(++i));
                    if (line.startsWith("<WRONG>")) errorStr = line.substring(7, line.length() - 8);
                    else if (line.startsWith("<CORRECTION>")) correctStr = line.substring(12, line.length() - 13);

                    line = ZhConverterUtil.convertToSimple(lines.get(++i));
                    if (line.startsWith("<WRONG>")) errorStr = line.substring(7, line.length() - 8);
                    else if (line.startsWith("<CORRECTION>")) correctStr = line.substring(12, line.length() - 13);

                    String key = id + "#" + errorStr;
                    if (!sighanDataMap.containsKey(key)) {
                        String content = id2ContentMap.getOrDefault(id, "");
                        if (content.isEmpty()) continue;
                        SighanDataBean sighanDataBean = new SighanDataBean(id, content, Integer.valueOf(location), errorStr, correctStr);
                        sighanDataMap.put(key, sighanDataBean);
                    }
                    else {
                        SighanDataBean sighanDataBean = sighanDataMap.get(key);
                        if (sighanDataBean.getLocation() > Integer.valueOf(location)) {
                            sighanDataBean.setLocation(Integer.valueOf(location));
                            sighanDataMap.put(key, sighanDataBean);
                        }
                    }
                }
            }
        }

        FileUtil.deleteFile(savePath);
        FileWriter fw = FileUtil.createFileWriter(savePath);
        for (HashMap.Entry<String, SighanDataBean> entry : sighanDataMap.entrySet()) {
            FileUtil.append(fw, entry.getValue().toString() + "\n");
        }
        FileUtil.close(fw);

    }
}
