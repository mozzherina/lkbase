/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rocket.alpha.arclasses;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is intended to support conversion from PostgreSQL to ArangoDB
 * Describes Tokens
 * @author Elena
 */
public class Token extends Node{
    String lemma;
    String pos;
    int num;
    //the following fields could be absent
    String cased = null;
    String adjPos = null;
    List<String> vFrames = null;
    List<String> vSentences = null;
   
    public Token(String key, String lemma, String pos, int num){
        super(key);
        this.lemma = lemma;
        this.pos = pos;
        this.num = num;
    }
      
    /**
     * Returns short description of Concept object
     * including an id in ArangoDB and lemma
     * Format is NOT concretized and can be changed
     * @return "Token element: id = Tokens/123, lemma = apple"
     */
    @Override public String toString() {
        return String.format("Token element: id = Tokens/%s, lemma = %s", 
                             super._key, lemma);
    }
        
    /**
     * Returns an id in ArangoDB's collection for a given key
     * @param key
     * @return id
     */
    public static String getArangoKey(String key){
        return String.format("Tokens/%s", key);
    }

    public String getLemma(){
        return lemma;
    }
    
    public void setCased(String cased){
        this.cased = cased;
    }
    
    public void setAdjPos(String pos){
        this.adjPos = pos;
    }
    
    public void addFrame(String frame){
        if (vFrames == null) vFrames = new ArrayList<>();
        vFrames.add(frame);
    }
    
    public void addSentence(String sentence){
        if (vSentences == null) vSentences = new ArrayList<>();
        vSentences.add(sentence);
    }
}