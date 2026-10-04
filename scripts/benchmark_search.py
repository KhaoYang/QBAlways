"""Small dependency-free load probe for the local QBAlways search API."""

from __future__ import annotations

import argparse
import concurrent.futures
import json
import statistics
import time
import urllib.parse
import urllib.request


def request_once(url: str, timeout: float) -> tuple[float, bool]:
    started = time.perf_counter()
    try:
        with urllib.request.urlopen(url, timeout=timeout) as response:
            payload = json.load(response)
            success = response.status == 200 and "results" in payload
    except Exception:
        success = False
    return (time.perf_counter() - started) * 1000, success


def percentile(values: list[float], percentage: float) -> float:
    index = max(0, min(len(values) - 1, round((len(values) - 1) * percentage)))
    return values[index]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--query", default="mitochondria")
    parser.add_argument("--requests", type=int, default=200)
    parser.add_argument("--concurrency", type=int, default=20)
    parser.add_argument("--base-url", default="http://localhost:8080/api/search")
    parser.add_argument("--timeout", type=float, default=5)
    args = parser.parse_args()

    if args.requests < 1 or args.concurrency < 1:
        parser.error("requests and concurrency must be positive")

    query = urllib.parse.urlencode({"q": args.query, "size": 10})
    url = f"{args.base_url}?{query}"
    started = time.perf_counter()

    with concurrent.futures.ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        samples = list(pool.map(
            lambda _: request_once(url, args.timeout),
            range(args.requests),
        ))

    elapsed = time.perf_counter() - started
    latencies = sorted(latency for latency, success in samples if success)
    failures = sum(1 for _, success in samples if not success)

    if not latencies:
        print(json.dumps({"requests": args.requests, "failures": failures}, indent=2))
        return 1

    report = {
        "query": args.query,
        "requests": args.requests,
        "concurrency": args.concurrency,
        "failures": failures,
        "throughput_rps": round(len(latencies) / elapsed, 2),
        "latency_ms": {
            "mean": round(statistics.fmean(latencies), 2),
            "p50": round(percentile(latencies, 0.50), 2),
            "p95": round(percentile(latencies, 0.95), 2),
            "p99": round(percentile(latencies, 0.99), 2),
            "max": round(max(latencies), 2),
        },
    }
    print(json.dumps(report, indent=2))
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
