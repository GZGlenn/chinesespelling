package com.pr.nlp.driver;

import com.pr.nlp.method.BasicMethod;
import com.pr.nlp.util.FileUtil;

public class TrainDriver {

    private String inputRoot = "";
    private String outputRoot = "";
    private String method = "";

    private boolean checkInput(String[] args) {
        if (args.length < 2) {
            return false;
        }

        inputRoot = args[0];
        outputRoot = args[1];

        if (!FileUtil.bExistFile(inputRoot)) return false;
        if (!FileUtil.bExistFile(outputRoot) && FileUtil.createIfNotExist(outputRoot) == null) {
            return false;
        }

        if (args.length > 2) method = args[3];
        else method = "basic";

        return true;
    }

    public void showHelp() {
        System.out.println("Useage: 2 params");
        System.out.println("first param: inputRoot");
        System.out.println("secnond param: outputRoot");
    }


    public boolean run(String[] args) {
        if (!checkInput(args)) {
            showHelp();
            return false;
        }

        if (method.equals("basic")) {
            BasicMethod basicMethod = new BasicMethod(inputRoot, outputRoot);
//            basicMethod.train();
        }

        return true;
    }

    public static void main(String[] args) {
        TrainDriver driver = new TrainDriver();
        boolean isCorrectFinish = driver.run(args);
        System.out.println("is Correct finish : " + isCorrectFinish);
    }
}
