"""
Task KB: Fused Latency -> Compress

Action: Download large bin -> Run Latency Probes (K) -> Gzip Compress (B) -> Upload gz
Purpose: Represents the fused execution of functions K and B. 
         This models the "elevation penalty": K is latency bound and optimal at 128MB.
         B is CPU bound and optimal at 768MB. When fused, the long latency wait of K
         is forced to run at 768MB, wasting memory-seconds and directly increasing cost.
"""

import os
import time
import gzip
import struct
import numpy as np

# Cold start detection
_is_cold_start = True

ITERATIONS_PER_SIZE = {
    "micro": 500,
    "small": 1000,
    "medium": 1500,
    "large": 2000,
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
    compression_level = event.get("compression_level", 6)
    memory_mb = int(os.environ.get("AWS_LAMBDA_FUNCTION_MEMORY_SIZE",
                                    os.environ.get("MEMORY", "0")))
    provider = "aws" if os.environ.get("AWS_LAMBDA_FUNCTION_NAME") else "gcp"
    region = os.environ.get("AWS_REGION",
                            os.environ.get("FUNCTION_REGION", "unknown"))

    metrics = MetricsCollector(
        task="kb_latency_compress",
        payload_size=payload_size,
        memory_mb=memory_mb,
        provider=provider,
        region=region
    )
    metrics.set_cold_start(cold_start)

    try:
        storage = get_storage_provider()

        # --- Download Phase ---
        data, primary_download_ns, input_size = storage.download_bytes(input_bucket, input_key)
        metrics.record_primary_download(primary_download_ns)

        # --- Phase 1: K's Latency Loops ---
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
                time.sleep(0.05)
                compute_time_total += 1000

        loop_end = time.perf_counter_ns()
        total_loop_time_ns = loop_end - loop_start

        additional_download_ns = total_loop_time_ns - compute_time_total
        metrics.record_additional_downloads(additional_download_ns, iterations)

        total_download_ns = primary_download_ns + additional_download_ns
        metrics.record_download(total_download_ns, input_size + total_additional_bytes)

        # --- Phase 2: B's CPU Compression ---
        compute_start = time.perf_counter_ns()
        
        # We compress the entire original data array to ensure it's CPU intensive
        compressed_data = gzip.compress(data, compresslevel=compression_level)
        
        compute_time_ns = time.perf_counter_ns() - compute_start + compute_time_total
        metrics.record_compute(compute_time_ns)

        # --- Upload Phase ---
        upload_time_ns, output_size = storage.upload_bytes(compressed_data, output_bucket, output_key)
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
