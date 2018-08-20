package com.pr.nlp.driver;

import com.pr.nlp.manager.WordSimilarCalculator2;

import java.util.HashSet;

public class TestWordSimilar {

    public static void main(String[] args) {
        WordSimilarCalculator2 calculator = new WordSimilarCalculator2("/home/public/code/chinese_spelling/chinesespelling/src/main/resource/");
        HashSet<String> words = calculator.getSimilarWord("注");
        for (String str : words) {
            if (str.equals("住")) System.out.println("afdssdfasfsaf");
            else System.out.println(str);
        }

//        System.out.println("---------------------------");
//
//        HashSet<String> words2 = calculator.getSimilarWord("苹果");
//        for (String str : words2) System.out.println(str);
    }

}
