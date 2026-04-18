"""
Task GH: Fused Reduce -> Expand

Action: Download large bin -> Reduce (G) -> Expand (H) -> Upload large bin
Purpose: Represents the fused execution of functions G and H. The intermediate 
         tiny 16-byte seed transfer between G and H in cloud storage is entirely
         eliminated, reducing latency and avoiding two distinct containers.
"""

import os
import time
import struct
import numpy as np

# Cold start detection
_is_cold_start = True

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
        task="gh_reduce_expand",
        payload_size=payload_size,
        memory_mb=memory_mb,
        provider=provider,
        region=region
    )
    metrics.set_cold_start(cold_start)

    try:
        storage = get_storage_provider()

        # --- G's Download Phase ---
        data, download_time_ns, input_size = storage.download_bytes(input_bucket, input_key)
        metrics.record_download(download_time_ns, input_size)

        # --- Fused Compute Phase (G + H) ---
        compute_start = time.perf_counter_ns()

        # Step 1: G's Compute (Reduce)
        n = struct.unpack("I", data[:4])[0]
        input_matrix = np.frombuffer(data, dtype=np.float64, offset=4).reshape(n, n)
        checksum = float(np.sum(input_matrix))
        seed = hash(tuple(input_matrix.flat[:min(100, n*n)])) & 0xFFFFFFFF
        
        # Virtual intermediate data (16 bytes)
        intermediate_data = struct.pack("IId", n, seed, checksum)

        # Step 2: H's Compute (Expand)
        n_out, seed_out, _checksum_out = struct.unpack("IId", intermediate_data)
        rng = np.random.default_rng(seed_out)
        output_matrix = rng.random((n_out, n_out), dtype=np.float64)
        
        output_data = struct.pack("I", n_out) + output_matrix.tobytes()

        compute_time_ns = time.perf_counter_ns() - compute_start
        metrics.record_compute(compute_time_ns)

        # --- H's Upload Phase ---
        upload_time_ns, output_size = storage.upload_bytes(output_data, output_bucket, output_key)
        metrics.record_upload(upload_time_ns, output_size)

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
