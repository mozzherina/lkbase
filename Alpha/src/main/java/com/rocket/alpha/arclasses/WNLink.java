/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rocket.alpha.arclasses;

/**
 * This class is intended to support conversion from PostgreSQL to ArangoDB
 * Describes Links from WordNet
 * @author Elena
 */
public class WNLink extends Link {
    String wnType; //'link' field from 'linktypes' relation 
    boolean recurses; //'recurses' field from 'linktypes' relation 
    
    public WNLink(String fromId, String toId, Class cl, String wnType, boolean recurses) {
        super(fromId, toId, cl, cl); 
        this.wnType = wnType;
        this.recurses = recurses;
    }
}
