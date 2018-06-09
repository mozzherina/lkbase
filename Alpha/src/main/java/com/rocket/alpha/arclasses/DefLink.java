/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rocket.alpha.arclasses;

/**
 * This class is intended to support conversion from PostgreSQL to ArangoDB
 * Describes Concept-Lemma-Links
 * @author Elena
 */
public class DefLink extends Link {
    String pos; //part of speech 
    
    public DefLink(String fromId, String toId, String pos) {
        super(fromId, toId, Concept.class, Lemma.class); 
        this.pos = pos;
    }
}
