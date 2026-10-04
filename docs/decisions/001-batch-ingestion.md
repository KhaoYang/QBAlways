# ADR 001: Use scheduled batch ingestion

**Status:** Accepted

## Context

QBReader exposes request/response APIs for set metadata and packet contents. It does not expose an ordered event stream or change-data-capture feed. QBAlways needs a repeatable way to build and refresh a search index without exceeding the upstream API's 20-request-per-second limit.

## Decision

Use a rate-limited batch job that enumerates sets and packets, transforms questions, and upserts stable document IDs into Solr. Store run status and progress in PostgreSQL. Make scheduled execution optional and allow operators to trigger bounded runs for development.

## Consequences

The pipeline is easy to run locally, retry, observe, and reason about. It provides eventual rather than immediate consistency. A full production implementation should persist the last completed set and packet so a restarted job can resume precisely.

Spark would help if transformation volume required distributed computation. Kafka and Flink would help if the source emitted a durable event stream and required low-latency stateful processing. Neither condition applies yet, so adding them now would increase failure modes without improving the product.
