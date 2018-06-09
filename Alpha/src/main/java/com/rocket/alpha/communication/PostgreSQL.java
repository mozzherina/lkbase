/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rocket.alpha.communication;

import com.rocket.alpha.arclasses.Concept;
import com.rocket.alpha.arclasses.DefLink;
import com.rocket.alpha.arclasses.LTLink;
import com.rocket.alpha.arclasses.Lemma;
import com.rocket.alpha.arclasses.Link;
import com.rocket.alpha.arclasses.Token;
import com.rocket.alpha.arclasses.WNLink;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This class contains functions for connecting and querying PostgreSQL database
 * @author Elena
 */
public class PostgreSQL {
    private static final Logger LOG = LogManager.getLogger(PostgreSQL.class); 
    private Connection connection = null;
    private Properties postgresProp = null;
    private HashMap<String, Lemma> lemmas = null; //lemma -> Lemma
    private HashMap<String, String> defs = null; //definition -> concept id  
    
    public PostgreSQL(){
        try{
            LOG.debug("Get properties file for connecting PostgreSQL");
            postgresProp = new Properties();
            postgresProp.load(getClass().getClassLoader()
                    .getResource("postgresql.properties")
                    .openStream()); 
        } catch (IOException ex){
            LOG.error("Failed finding properties: {}", ex.getMessage());
        }
    }
    
    //<editor-fold defaultstate="collapsed" desc="Useful functions">    
    private boolean tableExists(String tableName){
        try{
            DatabaseMetaData dbm = connection.getMetaData();
            ResultSet tables = dbm.getTables(null, null, tableName, null);
            return (tables.first());
        } catch (SQLException ex){
            return false;
        }
    }
    
    /**
     * @param query
     * @param tables used in query that need to be test for existence
     * @return ResultSet object
     * @throws SQLException if not enough tables or problems with query
     */
    private ResultSet executeQuery(String query, String ... tables) 
            throws SQLException{
        //check if all required tables exist in the database
        boolean checkTables = true;
        for (String t:tables){
            checkTables = checkTables && tableExists(t);
        }
        if (!checkTables){
            throw new SQLException("Not all tables exist");
        }  
        //run query
        LOG.debug("Run query: {}", query);
        PreparedStatement statement = connection.prepareStatement(query); 
        return statement.executeQuery();
    }
    //</editor-fold>  
    
    //<editor-fold defaultstate="collapsed" desc="Connection functions">  
    public boolean connectWordNet(){
        return connect(postgresProp.getProperty("wordnet"));
    }
    
    private boolean connect(String dbname){
        try{
            LOG.info("Connecting to database {}...", dbname);                 
            connection = DriverManager.getConnection(
                    postgresProp.getProperty("postgesql") + dbname, 
                    postgresProp.getProperty("user"),
                    postgresProp.getProperty("password"));
            LOG.debug("Successfully connected to {}", dbname);
            return true;
        } catch (SQLException ex){
            LOG.error("Connection failed: {}", ex.getMessage());
            return false;
        }
    }
    
    public boolean disconnect(){
        try{
            if (connection != null){
                connection.close();
                connection = null;
            }
            LOG.info("Successfully disconnected from database");
            return true;
        } catch (SQLException ex) {
            LOG.error("Disconnection failed: {}", ex.getMessage());
            return false;
        }
    }
    //</editor-fold>   
       
    //<editor-fold defaultstate="collapsed" desc="Form Lemmas and USED_IN links">  
    /**
     * Map table Words to HashMap of Lemmas
     * As a side-effect form HashMap lemmas : lemma -> Lemma
     * @return HahsMap words : id -> lemma
     * @throws SQLException 
     */
    private Map<String, Lemma> getAllWords() throws SQLException{
        String query = postgresProp.getProperty("words.select.all");
        lemmas = new HashMap<>();
        Map<String, Lemma> words = new HashMap<>();
        ResultSet results = executeQuery(query, "words");
        while (results.next()){
            String id = results.getString(1);
            String word = results.getString(2);
            Lemma lemma = new Lemma(id, word);
            words.put(id, lemma);
            lemmas.put(word, lemma);
        }
        return words;
    }
    
    /**
     * Update Lemmas with information about all possible PoS of this Lemma
     * Get info from tables Senses and Synsets
     * @param words HahsMap words : id -> lemma
     * @throws SQLException 
     */
    private void updatePos(Map<String, Lemma> words) throws SQLException{
        String query = postgresProp.getProperty("words.select.pos");
        ResultSet results = executeQuery(query, "senses", "synsets");
        while (results.next()){
            words.get(results.getString(1)).addPos(results.getString(2));
        }
    }
    
    /**
     * Update Lemmas with information about Morphs
     * Get info from tables Morphs and Morphmaps
     * @param words HahsMap words : id -> lemma
     * @throws SQLException 
     */
    private void updateMorphs(Map<String, Lemma> words) throws SQLException{
        String query = postgresProp.getProperty("morphs.select.all");
        ResultSet results = executeQuery(query, "morphmaps", "morphs");
        while (results.next()){
            String wordid = results.getString(1);
            String pos = results.getString(2);
            String morph = results.getString(3);
            words.get(wordid).addMorph(pos, morph);
        }
    }
    
    /**
     * Form Lemmas collection with all required information
     * Save results in HashMap lemmas : lemma -> Lemma
     * @return Lemmas
     */
    public Collection<Lemma> getWordsWithMorphs(){
        try{
            Map<String, Lemma> words = getAllWords();
            updatePos(words);
            updateMorphs(words);
        } catch (SQLException ex){
            LOG.error("Failed execution query: {}", ex.getMessage());
        }
        return lemmas.values();
    }
    
    /**
     * Create USED_IN links from the first to all other lemmas in lList
     * @param lList contains at least 2 elements
     * @return list of links
     */
    private List<Link> formUsedInLinks(List<Lemma> lList){
        List<Link> result = new ArrayList<>();
        for (int i = 1; i < lList.size(); i++){
            result.add(new Link(lList.get(0).getArangoKey(), 
                                lList.get(i).getArangoKey(), 
                                Lemma.class, Lemma.class));
        }
        return result;
    }
    
    /**
     * Form Lemma -> Lemma links
     * @return list of all USED_IN links
     */
    public List<Link> getUsedInLinks(){
        Map <String, List<Lemma>> mapping = new HashMap<>(lemmas.size());
        //for each lemma in all database
        lemmas.forEach((l, lemma) -> {
            //split this lemma for all possible collocations
            String[] collocations = Lemma.getCollocations(l);
            //and for each collocation if it is another complete lemma
            //save link to this lemma
            for (String c:collocations){
                if (lemmas.containsKey(c)){
                    List<Lemma> lList = mapping.getOrDefault(c, new ArrayList<>());
                    //if lemma is the same as collocation
                    //place it in the head otherwise in the tail
                    if (l.equals(c)){
                        lList.add(0, lemma);
                    } else {
                        lList.add(lemma);
                    }
                    //save this new list
                    mapping.put(c, lList);
                }
            }
        });
        List<Link> usedIn = new ArrayList<>();
        //collocation -> list of lemmas with this collocation
        mapping.forEach((col, lList) -> {
            if ((lList.size() > 1) && (col.length() > 2)){
                usedIn.addAll(formUsedInLinks(lList));
            }});
        return usedIn;        
    }
    //</editor-fold>       
        
    //<editor-fold defaultstate="collapsed" desc="Form Concepts and supporting links">  
    /**
     * Form Concepts collection with all required information
     * As a side-effect save all definitions for further processing in defs variable
     * @param concepts is a HashMap id -> Concept
     * @throws SQLException 
     */
    private void getSynsets(Map<String, Concept> concepts) throws SQLException{
        String query = postgresProp.getProperty("synsets.select.all");
        defs = new HashMap<>();
        ResultSet results = executeQuery(query);
        while (results.next()){
            String def = results.getString(3);
            String id = results.getString(1);
            defs.put(def, id);
            concepts.put(id, new Concept(id, results.getString(2), def));
        }
    }
    
    /**
     * Update Concepts with information about Samples
     * Get info from table Samples
     * @param concepts is a HashMap id -> Concept
     * @throws SQLException 
     */
    private void updateConceptSamples(Map<String, Concept> concepts) throws SQLException{
        String query = postgresProp.getProperty("samples.select.all");
        ResultSet results = executeQuery(query, "samples");
        while (results.next()){
            concepts.get(results.getString(1)).addSample(results.getString(2));
        }
    }
    
    /**
     * Form Concepts collection with all required information
     * Side-effect: form defs : definition -> concept id
     * @return collection of Concepts
     */
    public Collection<Concept> getSynsetsAndSamples(){
        Map<String, Concept> concepts = new HashMap<>();
        try{
            getSynsets(concepts);
            updateConceptSamples(concepts);
        } catch (SQLException ex){
            LOG.error("Failed execution query: {}", ex.getMessage());
        }
        return concepts.values();
    }
    
    /**
     * TODO: NOT IMPLEMENTED
     * Find lemmas in definition string
     * Use lemmas
     * @param definition of Concept
     * @return HashMap lemma -> pos used in definition
     */
    private HashMap<String, String> findLemmas(String definition){
        HashMap<String, String> result = new HashMap<>();
        //find all lemmas in definition
        //return lemma -> pos
        //take in attention that lemma could be two- and three- words
        return result;
    }
    
    /**
     * Form USED_IN_DEF links from saved definitions of concepts
     * Use lemmas and defs
     * Side-effect: defs = null
     * @return list of USED_IN_DEF links
     */
    public List<DefLink> getUsedInDefLinks(){
        List<DefLink> links = new ArrayList<>();
        defs.forEach((def, id) -> { //definition -> concept id
            findLemmas(def).forEach((l, pos) -> { //lemma -> pos used in def
                links.add(new DefLink(Concept.getArangoKey(id),
                                      lemmas.get(l).getArangoKey(), 
                                      pos));
            });
        });
        defs = null;
        lemmas = null;
        return links;
    }
    
    /**
     * Export 'Semlinks' relation
     * @return list of WordNet links
     */
    public List<WNLink> getSemLinks(){
        String query = postgresProp.getProperty("semlinks.select.all");
        List<WNLink> links = new ArrayList<>();
        try (ResultSet results = executeQuery(query, "semlinks", "linktypes")){
            while (results.next()){
                links.add(new WNLink(
                        Concept.getArangoKey(results.getString(1)), //from
                        Concept.getArangoKey(results.getString(2)), //to
                        Concept.class, results.getString(3), //link
                        results.getBoolean(4))); //recurses
            }
        } catch (SQLException ex){
            LOG.error("Failed execution query: {}", ex.getMessage());
        }
        return links; 
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Form Tokens and supporting links">  
    /**
     * Map table Senses to HashMap of Tokens
     * @return token id -> Token
     * @throws SQLException 
     */
    private Map<String, Token> getAllSenses() throws SQLException{
        Map<String, Token> senses = new HashMap<>();
        String query = postgresProp.getProperty("senses.select.all");
        ResultSet results = executeQuery(query);
        while (results.next()){
            String senseId = results.getString(1);
            senses.put(senseId, new Token(senseId, results.getString(2),
                                results.getString(3), results.getInt(4)));
        }
        return senses;
    }
    
    /**
     * Update Tokens with information about Casedwords
     * Get info from table Casedwords
     * @param senses HashMap : token id -> Token
     * @throws SQLException 
     */
    private void updateCased(Map<String, Token> senses) throws SQLException{
        String query = postgresProp.getProperty("senses.select.cased");
        ResultSet results = executeQuery(query, "casedwords");
        while (results.next()){
            senses.get(results.getString(1)).setCased(results.getString(2));
        }
    }

    /**
     * Update Tokens with information about type of adjective
     * Get info from table Adjpositions
     * @param senses HashMap : token id -> Token
     * @throws SQLException 
     */
    private void updateAdjectives(Map<String, Token> senses) throws SQLException{
        String query = postgresProp.getProperty("adjectives.select.type");
        ResultSet results = executeQuery(query, "adjpositions");
        while (results.next()){
            senses.get(results.getString(1)).setAdjPos(results.getString(2));
        }
    }
    
    /**
     * Update Tokens with information about verb frames and sentences
     * Get info from tables Vframes, Vframemaps, Vframesentences, and Vframesentencemaps
     * @param senses HashMap : token id -> Token
     * @throws SQLException 
     */
    private void updateVerbs(Map<String, Token> senses) throws SQLException{
        String query = postgresProp.getProperty("verbs.select.frame");
        ResultSet results = executeQuery(query, "vframes", "vframemaps");
        while (results.next()){
            senses.get(results.getString(1)).addFrame(results.getString(2));
        }        
        query = postgresProp.getProperty("verbs.select.sentence");
        results = executeQuery(query, "vframesentences", "vframesentencemaps");
        while (results.next()){
            senses.get(results.getString(1)).addSentence(results.getString(2));
        }
    }
        
    /**
     * Form Tokens collection with all required information
     * Get information from tables Senses, Casedwords, Adjpositions, 
     * Vframes, Vframemaps, Vframesentences, and Vframesentencemaps
     * @return collection of Tokens
     */
    public Collection<Token> getSenses(){
        try{
            Map<String, Token> senses = getAllSenses();
            updateCased(senses);
            updateAdjectives(senses);
            updateVerbs(senses);
            return senses.values();
        } catch (SQLException ex){
            LOG.error("Failed execution query: {}", ex.getMessage());
            return new ArrayList<>();
        }
    }
       
    /**
     * Export 'lexlinks' relation
     * @return list of WordNet links
     */
    public List<WNLink> getLexLinks(){
        String query = postgresProp.getProperty("lexlinks.select.all");
        List<WNLink> links = new ArrayList<>();
        try (ResultSet results = executeQuery(query, "lexlinks")){
            while (results.next()){
                links.add(new WNLink(
                        Token.getArangoKey(results.getString(1)), //from
                        Token.getArangoKey(results.getString(2)), //to
                        Token.class, results.getString(3), //link
                        results.getBoolean(4))); //recurses
            }
        } catch (SQLException ex){
            LOG.error("Failed execution query: {}", ex.getMessage());
        }
        return links; 
    }
    
    public List<Link> getLemmaTokenConceptLinks(){
        List<Link> allLinks = new ArrayList<>();
        String query = postgresProp.getProperty("links.select.all");
        try (ResultSet results = executeQuery(query)){
            while (results.next()){
                String wordId = Lemma.getArangoKey(results.getString(1));
                String senseId = Token.getArangoKey(results.getString(2));
                String synsetId = Concept.getArangoKey(results.getString(3));
                allLinks.add(new LTLink(wordId, senseId, results.getString(4), //pos
                                        results.getInt(5))); //num
                allLinks.add(new Link(wordId, synsetId, Lemma.class, Concept.class));
                allLinks.add(new Link(senseId, synsetId, Token.class, Concept.class));
                allLinks.add(new Link(synsetId, senseId, Concept.class, Token.class));
            }
        } catch (SQLException ex){
            LOG.error("Failed execution query: {}", ex.getMessage());
        }
        return allLinks;
    }
    //</editor-fold>
}
