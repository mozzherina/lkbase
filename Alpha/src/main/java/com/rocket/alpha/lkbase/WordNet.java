/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rocket.alpha.lkbase;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.rocket.alpha.communication.PostgreSQL;
import com.rocket.alpha.communication.Arango;

/**
 * This class is intended to convert the original WordNet database from 
 * the relational form in PostgreSQL to graph representation in ArangoDB
 * Depends on classes in Communication Package
 * @author Elena
 */
public class WordNet {
    private static final Logger LOG = LogManager.getLogger(WordNet.class);
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args){
        LOG.debug("***Start WordNet migration***");
        PostgreSQL postgres = new PostgreSQL();
        Arango arango = new Arango();  
        //if successfully connected to both databases
        if (postgres.connectWordNet() && arango.connectAsRoot("wordnet")){
            //move data from PostgerSQL to ArrangoDB
            //get all words and create lemmas collection
            arango.insertLemmas(postgres.getWordsWithMorphs());
            //get used_in links and save them to ArangoDB
            arango.insertUsedInLinks(postgres.getUsedInLinks());
            //get all synsets and create concepts collection
            arango.insertConcepts(postgres.getSynsetsAndSamples());
            //TODO: not implemented
            postgres.getUsedInDefLinks();
            //get all semlinks and connect concepts
            arango.insertSemLinks(postgres.getSemLinks());
            //get all senses with corresponding info from other tables
            //and create tokens collection
            arango.insertTokens(postgres.getSenses());
            //get all lexlinks and connect tokens
            arango.insertLexLinks(postgres.getLexLinks());
            arango.insertLinks(postgres.getLemmaTokenConceptLinks());
            //disconnect from both databases
            postgres.disconnect();
            arango.disconnect();
        }
        LOG.debug("***Finish WordNet migration***");
    }       
}
