# ADR 002: Use Solr behind a search service

**Status:** Accepted

## Context

QBReader's query API is effective for literal occurrence search, but QBAlways also needs relevance ranking, field boosts, phrase proximity, typo-tolerant evolution, highlighted snippets, and facets.

## Decision

Index normalized question documents in Apache Solr. Keep Solr private and expose a small Java API that owns validation and query construction.

## Consequences

Solr supplies Lucene-based ranking and search features without requiring QBAlways to implement index lifecycle mechanics directly. The Java boundary keeps Solr query syntax and administration away from untrusted browser clients. The cost is an additional JVM service and an index that must be synchronized with QBReader.
