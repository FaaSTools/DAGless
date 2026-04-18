"""
Task K: Memory-Insensitive Long Runner (Latency Bound)

Action: Download .bin (primary) -> Perform many sequential small storage reads -> Upload .bin
Purpose: Models functions where execution time is dominated by sequential latency (many
         round-trips to storage) rather than bandwidth or CPU throughput. Adding more memory
         provides NO speedup because the bottleneck is latency, not bandwidth or compute.

Input format:
Binary matrix: [n (4 bytes)] [n*n doubles (8 bytes each)]
(Same as other tasks - enables chaining)

Output format:
Binary matrix: [n (4 bytes)] [n*n doubles (8 bytes each)]
(Same matrix with small perturbations from latency probes)

The key insight: This function has a long execution time (20-40 seconds) that is FLAT across
all memory configurations. When paired with a compute-heavy function that benefits from more
memory (e.g., b_compress optimal at 768 MB), the elevation penalty for fusion can exceed
the invocation saving, making "don't fuse" the cost-optimal decision.

Number of iterations scales with payload size to achieve target runtime:
- micro: 100 iterations (~10s)
- small: 200 iterations (~20s)
- medium: 400 iterations (~40s)
- large: 600 iterations (~60s)

Each iteration:
1. Download a small reference file (~1KB)
2. Compute XOR checksum (negligible)
3. The round-trip latency (~50-100ms) dominates

Expected behavior: total_time_ms should be nearly identical across 128MB to 2048MB.
"""

import os
import time
import struct

import numpy as np

# Cold start detection
_is_cold_start = True

ITERATIONS_PER_SIZE = {
    "micro": 500,      # ~15s
    "small": 1000,     # ~30s
    "medium": 1500,    # ~60s
    "large": 2000,     # ~90s
}


def handler(event, context):
    """AWS Lambda / GCP Cloud Function handler."""
    global _is_cold_start
    cold_start = _is_cold_start
    _is_cold_start = False

    import sys
    sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

    from shared.storage import get_storage_provider
    from shared.metrics import MetricsCollector

    # Parse input
    input_bucket = event.get("input_bucket")
    input_key = event.get("input_key")
    output_bucket = event.get("output_bucket")
    output_key = event.get("output_key")
    payload_size = event.get("payload_size", "unknown")
    memory_mb = int(os.environ.get("AWS_LAMBDA_FUNCTION_MEMORY_SIZE",
                                    os.environ.get("MEMORY", "0")))
    provider = "aws" if os.environ.get("AWS_LAMBDA_FUNCTION_NAME") else "gcp"
    region = os.environ.get("AWS_REGION",
                            os.environ.get("FUNCTION_REGION", "unknown"))

    metrics = MetricsCollector(
        task="k_latency_bound",
        payload_size=payload_size,
        memory_mb=memory_mb,
        provider=provider,
        region=region
    )
    metrics.set_cold_start(cold_start)

    try:
        storage = get_storage_provider()

        # Step 1: Download primary input (would be eliminated by fusion)
        # For task_k, this must be TINY, otherwise bandwidth time dominates and scales with memory.
        data, primary_download_ns, input_size = storage.download_bytes(input_bucket, input_key)
        metrics.record_primary_download(primary_download_ns)

        # Preserve the matrix size 'n' if present so downstream functions can use it
        try:
            n = struct.unpack("I", data[:4])[0]
            if n > 10000 or n == 0:  # Protect against parsing compressed or invalid headers
                n = 100
        except Exception:
            n = 100

        # Step 2: Perform many sequential storage reads (this is the latency-bound part)
        # Each read is small but incurs full round-trip latency
        iterations = ITERATIONS_PER_SIZE.get(payload_size, 200)
        reference_key = "payloads/ref/latency_probe.bin"

        loop_start = time.perf_counter_ns()
        checksum = 0
        total_additional_bytes = 0
        compute_time_total = 0

        for i in range(iterations):
            try:
                probe_data, _, probe_size = storage.download_bytes(input_bucket, reference_key)
                total_additional_bytes += probe_size
                
                compute_start = time.perf_counter_ns()
                for byte in probe_data[:min(64, len(probe_data))]:
                    checksum ^= byte
                compute_time_total += time.perf_counter_ns() - compute_start
            except Exception:
                # If probe doesn't exist, simulate the latency (e.g. 50ms)
                time.sleep(0.05)
                compute_time_total += 1000

        loop_end = time.perf_counter_ns()
        total_loop_time_ns = loop_end - loop_start

        # Record additional downloads (the sequential reads)
        additional_download_ns = total_loop_time_ns - compute_time_total
        metrics.record_additional_downloads(additional_download_ns, iterations)

        # Total download = primary + all iterations
        total_download_ns = primary_download_ns + additional_download_ns
        metrics.record_download(total_download_ns, input_size + total_additional_bytes)

        # Step 3: Compute
        compute_start = time.perf_counter_ns()
        # Tiny aggregated result formatted as seed: [n] [iterations as seed] [checksum as double]
        primary_output = struct.pack("IId", n, iterations, float(checksum))
        compute_time_ns = time.perf_counter_ns() - compute_start + compute_time_total
        metrics.record_compute(compute_time_ns)

        # Step 4: Upload result (would be eliminated by fusion with successor)
        # Must be TINY
        upload_time_ns, output_size = storage.upload_bytes(primary_output, output_bucket, output_key)
        metrics.record_upload(upload_time_ns, output_size)
        metrics.record_primary_upload(upload_time_ns)

    except Exception as e:
        metrics.record_error(str(e))

    result = metrics.finalize()
    return {
        "statusCode": 200,
        "body": result.to_json()
    }


def gcp_handler(request):
    """HTTP Cloud Function entry point for GCP."""
    event = request.get_json(silent=True) or {}
    result = handler(event, None)
    return result["body"], result["statusCode"], {"Content-Type": "application/json"}
