package com.pr.nlp.data;

public class Global {

    public enum SpellCheckMethod {
        BASIC("basic",0);

        private String name;
        private int index;

        SpellCheckMethod(String name, int index) {
            this.name = name;
            this.index = index;
        }
    }
}
