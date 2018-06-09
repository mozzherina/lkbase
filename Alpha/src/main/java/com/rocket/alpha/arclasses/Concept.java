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
 * Describes Concepts
 * @author Elena
 */
public class Concept extends Node{
    String pos;
    String definition;
    List<String> samples;
    
    public Concept(String key, String pos, String definition){
        super(key);
        this.pos = pos;
        this.definition = definition;
    }
    
    /**
     * Returns short description of Concept object
     * including an id in ArangoDB and definition
     * Format is NOT concretized and can be changed
     * @return "Concept element: id = Concepts/123, def = Fruit of apple tree"
     */
    @Override public String toString() {
        return String.format("Concept element: id = Concepts/%s, def = %s", 
                             super._key, definition);
    }
    
    /**
     * Returns an id in ArangoDB's collection for a given key
     * @param key
     * @return id
     */
    public static String getArangoKey(String key){
        return String.format("Concepts/%s", key);
    }
    
    public void addSample(String sample){
        if (samples == null) samples = new ArrayList<>();
        samples.add(sample);
    }
}