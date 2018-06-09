/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rocket.alpha.arclasses;

/**
 * Base class for Lemma, Token, and Concept
 * _key is the same _key field in ArangoDB
 * @author Elena
 */
public abstract class Node {
    String _key;
    
    public Node(String key){
        _key = key;
    }
    
    @Override public abstract String toString();
    
    public static String getArangoKey(String key){
        throw new UnsupportedOperationException("Method is not implemented");
    };
    
    public String getKey(){
        return _key;
    }
}
