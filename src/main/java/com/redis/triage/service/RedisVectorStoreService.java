package com.redis.triage.service;

import com.redis.triage.model.SimilarIssueResult;
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
    public void storeEmbedding(String issueId, float[] embedding, String title, String body, List<String> labels, String url) {
        log.info("Storing embedding for issue: {}", issueId);

        try {
            String key = KEY_PREFIX + issueId;

            // Convert float array to byte array
            byte[] embeddingBytes = floatArrayToByteArray(embedding);

            // Prepare the hash fields
            Map<String, String> hash = new HashMap<>();
            hash.put("title", title != null ? title : "");
            hash.put("body", body != null ? body : "");
            hash.put("url", url != null ? url : "");
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
     * Searches for similar issues using KNN vector search with proper FT.SEARCH syntax
     *
     * @param queryVector The query vector for similarity search
     * @param k The number of top matches to return
     * @return List of matching Redis keys (issue IDs)
     */
    public List<SimilarIssueResult> searchSimilarIssues(float[] queryVector, int k) {
        log.info("Searching for {} similar issues using KNN vector search", k);

        try {
            byte[] queryBytes = floatArrayToByteArray(queryVector);
            String searchQuery = String.format("*=>[KNN %d @embedding $vec_param AS score]", k);

            SearchResult result = jedis.ftSearch(INDEX_NAME, searchQuery,
                    redis.clients.jedis.search.FTSearchParams.searchParams()
                            .addParam("vec_param", queryBytes)
                            .dialect(2)
                            .limit(0, k));

            List<SimilarIssueResult> similar = new ArrayList<>();
            for (Document doc : result.getDocuments()) {
                String redisKey = doc.getId();
                double distance = Double.parseDouble(doc.getString("score"));
                int score = (int) Math.round((1.0 - distance) * 100.0);

                similar.add(new SimilarIssueResult(redisKey, distance, score));
            }

            log.info("Found {} similar issues", similar.size());
            return similar;

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
}