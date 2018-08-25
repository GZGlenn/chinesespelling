package com.pr.nlp.manager;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TrieLMTree {

    private class Node {
        public String word;
        public Float score;
        public HashMap<String, Node> children;

        public Node() {
            word = "";
            score = Float.valueOf("1.0");
            children = new HashMap<>();
        }
    }

    public Node root;

    public TrieLMTree() {
        this.root = new Node();
    }

    public double calLMScore(String string, LanguageModelManager2 lmManager) {
        List<Term> termList = HanLP.segment(string);
        double score = root.score;
        String lastWord = root.word;
        Node curNode = root;
        for (Term term : termList) {
            if (!curNode.children.containsKey(term.word)) {
                Node node = new Node();
                node.word = term.word;
                if (lastWord.isEmpty()) {
                    node.score = lmManager.getUnigram(term.word);
                }
                else {
                    node.score = lmManager.getLM(lastWord, term.word);
                }
                score += node.score;
                curNode.children.put(node.word, node);
                curNode = node;
            }
            else {
                curNode = curNode.children.get(term.word);
                score += curNode.score;
            }
            lastWord = term.word;
        }

        return score;
    }
}
