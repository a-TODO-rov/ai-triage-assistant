package com.redis.triage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.SearchResult;
import redis.clients.jedis.search.schemafields.VectorField;
import redis.clients.jedis.search.schemafields.TextField;
import redis.clients.jedis.search.FTCreateParams;

import jakarta.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing vector embeddings in Redis using RediSearch
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisVectorStoreService {

    private final JedisPooled jedis;
    private static final String INDEX_NAME = "issue_index";
    private static final String KEY_PREFIX = "issue:";
    private static final int VECTOR_DIMENSION = 1536; // Common dimension for OpenAI embeddings

    /**
     * Initialize the Redis vector index if it doesn't exist
     */
    @PostConstruct
    public void initializeIndex() {
        try {
            // Check if index already exists
            try {
                jedis.ftInfo(INDEX_NAME);
                log.info("Redis vector index '{}' already exists", INDEX_NAME);
                return;
            } catch (Exception e) {
                // Index doesn't exist, create it
                log.info("Creating Redis vector index '{}'", INDEX_NAME);
            }

            // Create schema fields using the basic constructors
            TextField titleField = new TextField("title");
            TextField bodyField = new TextField("body");
            TextField labelsField = new TextField("labels");

            // Create vector field with HNSW algorithm
            Map<String, Object> vectorAttrs = new HashMap<>();
            vectorAttrs.put("TYPE", "FLOAT32");
            vectorAttrs.put("DIM", VECTOR_DIMENSION);
            vectorAttrs.put("DISTANCE_METRIC", "COSINE");
            VectorField embeddingField = new VectorField("embedding",
                VectorField.VectorAlgorithm.HNSW, vectorAttrs);

            // Create the index with prefix
            FTCreateParams createParams = new FTCreateParams();
            createParams.prefix(KEY_PREFIX);

            jedis.ftCreate(INDEX_NAME, createParams, titleField, bodyField, labelsField, embeddingField);
            log.info("Successfully created Redis vector index '{}'", INDEX_NAME);

        } catch (Exception e) {
            log.error("Failed to initialize Redis vector index: {}", e.getMessage(), e);
        }
    }

    /**
     * Stores an embedding vector for an issue in Redis
     *
     * @param issueId The unique identifier for the issue
     * @param embedding The vector embedding as float array
     * @param title The issue title
     * @param body The issue body
     * @param labels The issue labels
     */
    public void storeEmbedding(String issueId, float[] embedding, String title, String body, List<String> labels) {
        log.info("Storing embedding for issue: {}", issueId);

        try {
            String key = KEY_PREFIX + issueId;

            // Convert float array to byte array
            byte[] embeddingBytes = floatArrayToByteArray(embedding);

            // Prepare the hash fields
            Map<String, String> hash = new HashMap<>();
            hash.put("title", title != null ? title : "");
            hash.put("body", body != null ? body : "");
            hash.put("labels", labels != null ? String.join(",", labels) : "");

            // Store the hash fields
            jedis.hset(key, hash);

            // Store the embedding as binary data
            jedis.hset(key.getBytes(), "embedding".getBytes(), embeddingBytes);

            log.info("Successfully stored embedding for issue: {}", issueId);

        } catch (Exception e) {
            log.error("Failed to store embedding for issue '{}': {}", issueId, e.getMessage(), e);
        }
    }

    /**
     * Searches for similar issues based on a query vector
     *
     * @param queryVector The query vector for similarity search
     * @param k The number of similar results to return
     * @return List of similar issue IDs
     */
    public List<String> searchSimilar(float[] queryVector, int k) {
        log.info("Searching for {} similar issues", k);

        try {
            // Convert query vector to byte array
            byte[] queryBytes = floatArrayToByteArray(queryVector);

            // Build the KNN query
            String knnQuery = String.format("*=>[KNN %d @embedding $BLOB]", k);

            Query query = new Query(knnQuery)
                .addParam("BLOB", queryBytes)
                .returnFields("title", "labels")
                .setSortBy("__embedding_score", true)
                .limit(0, k);

            SearchResult result = jedis.ftSearch(INDEX_NAME, query);

            List<String> issueIds = new ArrayList<>();
            for (Document doc : result.getDocuments()) {
                // Extract issue ID from the key (remove "issue:" prefix)
                String issueId = doc.getId().substring(KEY_PREFIX.length());
                issueIds.add(issueId);
            }

            log.info("Found {} similar issues", issueIds.size());
            return issueIds;

        } catch (Exception e) {
            log.error("Failed to search for similar issues: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Converts a float array to byte array for Redis storage
     *
     * @param floats The float array to convert
     * @return The byte array representation
     */
    private byte[] floatArrayToByteArray(float[] floats) {
        ByteBuffer buffer = ByteBuffer.allocate(floats.length * 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (float f : floats) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }

    /**
     * Legacy method for backward compatibility
     *
     * @param issueId The unique identifier for the issue
     * @param embedding The vector embedding as float array
     * @param text The original text that was embedded
     */
    public void storeEmbedding(String issueId, float[] embedding, String text) {
        storeEmbedding(issueId, embedding, text, "", List.of());
    }

    /**
     * Searches for similar issues using KNN vector search with proper FT.SEARCH syntax
     *
     * @param queryVector The query vector for similarity search
     * @param k The number of top matches to return
     * @return List of matching Redis keys (issue IDs)
     */
    public List<String> searchSimilarIssues(float[] queryVector, int k) {
        log.info("Searching for {} similar issues using KNN vector search", k);

        try {
            // Convert query vector to byte array
            byte[] queryBytes = floatArrayToByteArray(queryVector);

            // Build the FT.SEARCH query with proper KNN syntax
            String searchQuery = String.format("*=>[KNN %d @embedding $vec_param]", k);

            // Execute the search with PARAMS and DIALECT 2
            SearchResult result = jedis.ftSearch(INDEX_NAME, searchQuery,
                redis.clients.jedis.search.FTSearchParams.searchParams()
                    .addParam("vec_param", queryBytes)
                    .dialect(2)
                    .limit(0, k));

            List<String> issueKeys = new ArrayList<>();
            for (Document doc : result.getDocuments()) {
                // Return the full Redis key (e.g., "issue:123")
                issueKeys.add(doc.getId());
            }

            log.info("Found {} similar issues: {}", issueKeys.size(), issueKeys);
            return issueKeys;

        } catch (Exception e) {
            log.error("Failed to search for similar issues using KNN: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Retrieves issue metadata from Redis by issue key
     *
     * @param issueKey The Redis key for the issue (e.g., "issue:123")
     * @return Map containing issue metadata (title, labels) or empty map if not found
     */
    public Map<String, String> getIssueMetadata(String issueKey) {
        log.debug("Retrieving metadata for issue key: {}", issueKey);

        try {
            Map<String, String> metadata = jedis.hgetAll(issueKey);
            if (metadata.isEmpty()) {
                log.warn("No metadata found for issue key: {}", issueKey);
            }
            return metadata;
        } catch (Exception e) {
            log.error("Failed to retrieve metadata for issue key '{}': {}", issueKey, e.getMessage(), e);
            return Map.of();
        }
    }

    /**
     * Legacy method for backward compatibility
     *
     * @param query The search query
     * @param k The number of similar results to return
     * @return List of similar issue IDs
     */
    public List<String> searchSimilar(String query, int k) {
        log.warn("String-based search not implemented, use vector-based search instead");
        return List.of();
    }
}