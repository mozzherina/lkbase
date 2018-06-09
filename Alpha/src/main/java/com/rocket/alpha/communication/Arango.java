/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rocket.alpha.communication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDB.Builder;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionType;
import com.arangodb.model.CollectionCreateOptions;
import com.rocket.alpha.arclasses.Concept;
import com.rocket.alpha.arclasses.Lemma;
import com.rocket.alpha.arclasses.Link;
import com.rocket.alpha.arclasses.Node;
import com.rocket.alpha.arclasses.Token;
import com.rocket.alpha.arclasses.WNLink;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

/**
 * This class contains functions for connecting and querying ArrangoDB database
 * @author Elena
 */
public class Arango {
    private static final Logger LOG = LogManager.getLogger(Arango.class); 
    private Properties arangoProp = null;
    private Builder arangoBuilder;
    private ArangoDB arangoDB;
    private ArangoDatabase db;
    private ArangoCollection collection;   

    
    public Arango(){
        try{
            LOG.debug("Get properties file for connecting ArrangoDB");
            arangoProp = new Properties();
            arangoProp.load(getClass().getClassLoader()
                    .getResource("arango.properties")
                    .openStream());
            arangoBuilder = new ArangoDB.Builder()
                    .host(arangoProp.getProperty("host"), 
                          Integer.parseInt(arangoProp.getProperty("port")));
        } catch (IOException ex){
            LOG.error("Failed finding properties: {}", ex.getMessage());
        }
    }
   
    //<editor-fold defaultstate="collapsed" desc="Connection functions">  
    /**
     * Connect as a root user
     * @param db database name
     * @return 
     */
    public boolean connectAsRoot(String db){
        return connect(arangoProp.getProperty("user"),
                       arangoProp.getProperty("password"),
                       arangoProp.getProperty(db), true);        
    }
    
    /**
     * Establish connection to WordNet database
     * @return if succeeded
     */
    /*
    public boolean connectWordNet(){
        return connect(arangoProp.getProperty("wordnet.user"),
                       arangoProp.getProperty("wordnet.password"),
                       arangoProp.getProperty("wordnet"), false);
    }*/
    
    private boolean connect(String dbUser, String dbPassword, String dbName, boolean createNew){
        try{
            LOG.info("Connecting to database {}...", dbName);                  
            arangoBuilder.user(dbUser);
            arangoBuilder.password(dbPassword);
            arangoDB = arangoBuilder.build();    
            if (createNew){
                LOG.debug("Drop database {}", dbName);   
                arangoDB.db(dbName).drop();
                arangoDB.createDatabase(dbName);
            }
	    db = arangoDB.db(dbName);
            LOG.debug("Successfully connected to {}", db.name());
            return true;
        } catch (ArangoDBException ex){
            LOG.error("Connection failed: {}", ex.getMessage());
            return false;
        }
    }
    
    public void disconnect(){
        arangoDB.shutdown();
        LOG.info("Successfully disconnected from database");
    }
    //</editor-fold>  
    
    private void insert(String collectionName, Collection<?> nodesOrLinks, 
            boolean isNodeDescendant){
        try{
            LOG.debug("Creating collection {}", collectionName);        
            if (isNodeDescendant){
                db.createCollection(collectionName);
            } else {
                db.createCollection(collectionName, 
                    new CollectionCreateOptions().type(CollectionType.EDGES));
            }
            collection = db.collection(collectionName);            
            LOG.info("Inserting into {}", collectionName);
            long startTime = System.currentTimeMillis();
            collection.insertDocuments(nodesOrLinks);
            long endTime = System.currentTimeMillis();
            LOG.debug("Inserted finished in {} ms", (endTime - startTime));
        } catch (ArangoDBException ex){
            LOG.error("Insertion failed: {}", ex.getMessage());
        }
    }
       
    //TODO: add indexes on table
    public void insertLemmas(Collection<Lemma> lemmas){
        insert(arangoProp.getProperty("collection.lemmas"), lemmas, true);
    }
    
    //TODO: add indexes on table
    public void insertConcepts(Collection<Concept> concepts){
        insert(arangoProp.getProperty("collection.concepts"), concepts, true);
    }
    
    //TODO: add indexes on table
    public void insertTokens(Collection<Token> tokens){
        insert(arangoProp.getProperty("collection.tokens"), tokens, true);
    }
    
    public void insertUsedInLinks(Collection<Link> links){
        insert(arangoProp.getProperty("collection.usedin"), links, false);
    }

    public void insertSemLinks(Collection<WNLink> links){
        insert(arangoProp.getProperty("collection.semlinks"), links, false);
    }
    
    public void insertLexLinks(Collection<WNLink> links){
        insert(arangoProp.getProperty("collection.lexlinks"), links, false);
    }
    
    public void insertLinks(Collection<Link> links){
        insert(arangoProp.getProperty("collection.links"), links, false);
    }
}