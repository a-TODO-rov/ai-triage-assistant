# Semantic Cache Testing Guide

This document explains how to test the semantic caching functionality that was implemented to optimize label generation.

## Overview

The semantic caching system works as follows:
1. **CACHE_MISS**: First time an issue is processed, it uses LLM to generate labels
2. **Store**: The issue and its labels are stored in Redis with vector embeddings
3. **CACHE_HIT**: Similar future issues reuse labels from cached issues (≥92% similarity)

## Test Files

### 1. Integration Test: `SemanticCacheIT.java`
**Location**: `src/test/java/com/redis/triage/SemanticCacheIT.java`

**Purpose**: Tests the complete cache flow with real Redis and LLM calls

**Test Methods**:
- `shouldDemonstrateCacheMissToHitFlow()`: Full workflow test
- `shouldDemonstrateControlledCacheHit()`: Controlled test with pre-stored data
- `shouldLogCacheMissAndHitBehavior()`: Verifies logging behavior

**Requirements**:
- Docker (for Redis Testcontainers)
- Environment variables:
  - `LITELLM_BASE_URL`: URL of LiteLLM service
  - `LITELLM_API_KEY`: API key for LiteLLM

### 2. Unit Test: `LabelingServiceCacheTest.java`
**Location**: `src/test/java/com/redis/triage/service/LabelingServiceCacheTest.java`

**Purpose**: Tests cache logic without external dependencies (mocked)

**Test Methods**:
- `shouldReturnCachedLabelsOnCacheHit()`: Verifies cache hit behavior
- `shouldUseLLMOnCacheMiss()`: Verifies fallback to LLM
- `shouldHandleEmptyLabelsInCachedIssue()`: Edge case handling
- `shouldFallbackToLLMOnSemanticSearchException()`: Error handling

## Running the Tests

### Prerequisites
```bash
# Start LiteLLM service (if testing integration tests)
docker run -p 4000:4000 ghcr.io/berriai/litellm:main-latest

# Set environment variables
export LITELLM_BASE_URL=http://localhost:4000
export LITELLM_API_KEY=your-api-key
```

### Run Unit Tests Only
```bash
./mvnw test -Dtest=LabelingServiceCacheTest
```

### Run Integration Tests
```bash
./mvnw test -Dtest=SemanticCacheIT
```

### Run All Tests
```bash
./mvnw test
```

## Expected Log Output

### Cache Miss
```
INFO  - CACHE_MISS: No high-similarity match found, using LLM
INFO  - LLM generated 3 labels for issue 'Redis timeout': [bug, redis, timeout]
```

### Cache Hit
```
INFO  - CACHE_HIT: Reusing labels from issue 1001 (similarity: 0.94)
INFO  - Cached labels for issue 'Redis timeout issues': [bug, redis, timeout]
```

## Test Scenarios Covered

### 1. Basic Cache Flow
- Issue A processed → CACHE_MISS → LLM generates labels → Store in Redis
- Similar Issue B processed → CACHE_HIT → Reuse labels from Issue A

### 2. Similarity Threshold
- Issues with ≥92% similarity → CACHE_HIT
- Issues with <92% similarity → CACHE_MISS

### 3. Edge Cases
- Empty labels in cached issue
- Null labels in cached issue
- Redis connection failures
- LLM API failures

### 4. Error Handling
- Semantic search exceptions → Fallback to LLM
- Invalid similarity scores → Fallback to LLM
- Missing embeddings → Fallback to LLM

## Monitoring Cache Effectiveness

### Key Metrics to Watch
1. **Cache Hit Rate**: Percentage of issues that hit cache vs. use LLM
2. **Similarity Scores**: Distribution of similarity scores for matches
3. **Response Times**: Cache hits should be significantly faster
4. **Cost Savings**: Reduced LLM API calls

### Log Analysis
```bash
# Count cache hits vs misses
grep "CACHE_HIT" application.log | wc -l
grep "CACHE_MISS" application.log | wc -l

# Analyze similarity scores
grep "CACHE_HIT.*similarity:" application.log | sed 's/.*similarity: \([0-9.]*\).*/\1/'
```

## Troubleshooting

### Common Issues

1. **No Cache Hits Despite Similar Issues**
   - Check similarity threshold (currently 92%)
   - Verify embeddings are being generated correctly
   - Check Redis connectivity

2. **Tests Failing with Redis Errors**
   - Ensure Docker is running for Testcontainers
   - Check Redis container startup logs

3. **LLM API Errors in Integration Tests**
   - Verify `LITELLM_BASE_URL` and `LITELLM_API_KEY`
   - Check LiteLLM service is running and accessible

### Debug Mode
Add to `application-test.properties`:
```properties
logging.level.com.redis.triage.service.LabelingService=DEBUG
logging.level.com.redis.triage.service.SemanticSearchService=DEBUG
```

This will show detailed logs of the caching decisions and similarity calculations.
