/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rocket.alpha.arclasses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is intended to support conversion from PostgreSQL to ArangoDB
 * Describes Lemmas
 * @author Elena
 */
public class Lemma extends Node{
    String lemma;
    List<String> pos;
    Map<String, List<String>> morphs;
   
    public Lemma(String key, String lemma){
        super(key);
        this.lemma = lemma;
        //for 99% of the words that would be enough
        pos = new ArrayList<>(2);
    }
    
    public String getLemma(){
        return lemma;
    }
    
    public void addPos(String pos){
        this.pos.add(pos);
    }
    
    public void addMorph(String pos, String morph){
        if (morphs == null) morphs = new HashMap<>();
        List<String> morphList = morphs.getOrDefault(pos, new ArrayList<>());
        morphList.add(morph);
        morphs.put(pos, morphList);
    }
    
    /**
     * Returns short description of Lemmas object
     * including an id in ArangoDB and lemma
     * Format is NOT concretized and can be changed
     * @return "Lemma element: id = Lemmas/123, lemma = apple"
     */
    @Override public String toString() {
        return String.format("Lemma element: id = Lemmas/%s, lemma = %s", 
                             super._key, lemma);
    }
    
    /**
     * Returns an id in ArangoDB's collection for a given key
     * Attention! 2 variants
     * @return id
     */    
    public String getArangoKey(){
        return String.format("Lemmas/%s", super._key); 
    }
    
    public static String getArangoKey(String key){
        return String.format("Lemmas/%s", key); 
    }
       
    /**
     * Form all possible collocations out of lemma
     * Example: mama mila ramu
     * Result: mama, mila, ramu, mama mila, mama mila ramu, mila ramu
     * @param l is a String for parsing
     * @return array of collocations
     */
    public static String[] getCollocations(String l){
        String[] words = l.split(" ");
        List<String> collocations = new ArrayList<>();
        collocations.addAll(Arrays.asList(words));
        int k;
        StringBuilder collocation;
        for (int i = 0; i < words.length; i++){
            for (int j = i+1; j < words.length; j++){
                k = i;
                collocation = new StringBuilder(words[k]);
                while (k < j){
                    k++;
                    collocation.append(" ").append(words[k]);
                }
                collocations.add(collocation.toString());
            }
        }       
        return collocations.toArray(new String[0]);
    }
}

