/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rocket.alpha.arclasses;

/**
 * This class is intended to support conversion from PostgreSQL to ArangoDB
 * Describes Lemma-Token-Links
 * @author Elena
 */
public class LTLink extends Link {
    String pos; //part of speech 
    int num; //number of concept in current part of speech
    
    public LTLink(String fromId, String toId, String pos, int num) {
        super(fromId, toId, Lemma.class, Token.class); 
        this.pos = pos;
        this.num = num;
    }
}
