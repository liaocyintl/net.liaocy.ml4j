/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.liaocy.ml4j.tfidf;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.GridFSUploadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import static java.lang.Math.log;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.liaocy.ml4j.db.Mongo;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;

/**
 *
 * @author liaocy
 */
public class tfidf {

    private HashMap<Integer, HashMap<Integer, Integer>> wordDocCount;
    private HashMap<Integer, Integer> docCount;
    private HashMap<Integer, Integer> wordCount;

    public tfidf() {
        wordCount = new HashMap<>();
        docCount = new HashMap<>();
        wordDocCount = new HashMap<>();
    }

    public void add(int wordId, int docId) {
        if (!docCount.containsKey(docId)) {
            docCount.put(docId, 0);
        }
        docCount.put(docId, docCount.get(docId) + 1);

        if (!wordCount.containsKey(wordId)) {
            wordCount.put(wordId, 0);
        }
        wordCount.put(wordId, wordCount.get(wordId) + 1);

        if (!this.wordDocCount.containsKey(wordId)) {
            this.wordDocCount.put(wordId, new HashMap<Integer, Integer>());
        }
        Map<Integer, Integer> doc = this.wordDocCount.get(wordId);
        if (!doc.containsKey("docId")) {
            doc.put(docId, 0);
        }
        doc.put(docId, doc.get(docId) + 1);
    }

    public double getIdf(int wordId) {
        double idf = docCount.size();
        if (this.wordDocCount.containsKey(wordId)) {
            idf = idf / (1 + this.wordDocCount.get(wordId).size());
            return log(idf);
        }
        return 0.;
    }

    public void save(String modelName) throws IOException {
        MongoDatabase db = Mongo.getDB();
        GridFSBucket gridFSBucket = GridFSBuckets.create(db, "tfidfmodels");

        GridFSFile gfsfi = gridFSBucket.find(new Document("filename", modelName)).first();
        if (gfsfi != null) {
            ObjectId id = gfsfi.getObjectId();
            gridFSBucket.delete(id);
        }

        try (GridFSUploadStream uploadStream = gridFSBucket.openUploadStream(modelName)) {
            try (ObjectOutputStream o = new ObjectOutputStream(uploadStream)) {
                o.writeObject(this.wordDocCount);
                o.writeObject(this.docCount);
                o.writeObject(this.wordCount);
            }
        }

        System.out.println("Save Model Successed!");
    }

    public void load(String modelName) throws IOException, ClassNotFoundException {
        MongoDatabase db = Mongo.getDB();
        GridFSBucket gridFSBucket = GridFSBuckets.create(db, "tfidfmodels");
        try (GridFSDownloadStream stream = gridFSBucket.openDownloadStreamByName(modelName)) {
            try (ObjectInputStream objIn = new ObjectInputStream(stream)) {
                this.wordDocCount = (HashMap) objIn.readObject();
                this.docCount = (HashMap) objIn.readObject();
                this.wordCount = (HashMap) objIn.readObject();
            }
        }

        System.out.println("Loaded TFIDF Model: " + modelName);
    }
}