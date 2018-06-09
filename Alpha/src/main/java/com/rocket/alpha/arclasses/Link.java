/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rocket.alpha.arclasses;

/**
 * This class is intended to support conversion from PostgreSQL to ArangoDB
 * Describes Links
 * @author Elena
 */
public class Link{
    //String _key;
    String _from;
    String _to;
    LinkType type;
    
    private static enum LinkType {
        USED_IN,            //Lemma --> Lemma|Collocation
        HAS_SENSE,          //Lemma --> Token
        HAS_MEANING,        //Lemma --> Concept | Token -> Concept
        LEX_LINK,           //Token --> Token
        USED_IN_DEF,        //Concept --> Lemma
        REALIZED_THROUGH,   //Concept --> Token       
        SEM_LINK;           //Concept --> Concept    
    
        private static final String LEMMA = Lemma.class.getSimpleName();
        private static final String TOKEN = Token.class.getSimpleName();
        private static final String CONCEPT = Concept.class.getSimpleName();
        
        static LinkType getType (String from, String to){
            if (from.equals(LEMMA)){
                if (to.equals(LEMMA)) return USED_IN;
                if (to.equals(TOKEN)) return HAS_SENSE;
                if (to.equals(CONCEPT)) return HAS_MEANING;
            }
            if (from.equals(TOKEN)){
                if (to.equals(TOKEN)) return LEX_LINK;
                if (to.equals(CONCEPT)) return HAS_MEANING;
            }
            if (from.equals(CONCEPT)){
                if (to.equals(LEMMA)) return USED_IN_DEF;
                if (to.equals(TOKEN)) return REALIZED_THROUGH;
                if (to.equals(CONCEPT)) return SEM_LINK;
            }
            throw new AssertionError(String.format("Unknown link from %s to %s",
                    from, to));
        }
    }
    
    public Link (String from, String to, Class fromClass, Class toClass){
        _from = from;
        _to = to;
        type = LinkType.getType(fromClass.getSimpleName(), toClass.getSimpleName());
    }
}