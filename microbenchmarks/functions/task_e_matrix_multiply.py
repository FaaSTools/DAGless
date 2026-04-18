"""
Task E: Matrix Multiplication (Heavy Compute)

Action: Download matrix from storage -> Perform matrix multiplication -> Upload result to storage
Purpose: Maxes out CPU compute to test how FaaS platforms handle CPU-intensive tasks
         and whether they throttle background network connections while processor is pinned.
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
        task="e_matrix_multiply",
        payload_size=payload_size,
        memory_mb=memory_mb,
        provider=provider,
        region=region
    )
    metrics.set_cold_start(cold_start)

    try:
        storage = get_storage_provider()

        # Download matrix data
        data, download_time_ns, input_size = storage.download_bytes(input_bucket, input_key)
        metrics.record_download(download_time_ns, input_size)

        # Compute: Matrix multiplication
        compute_start = time.perf_counter_ns()

        # Deserialize matrix from binary format
        # Format: [n (4 bytes)] [n*n doubles (8 bytes each)]
        n = struct.unpack("I", data[:4])[0]

        # Use numpy for fast deserialization and multiplication
        matrix = np.frombuffer(data, dtype=np.float64, offset=4).reshape(n, n)

        # Matrix multiply (A * A)
        result = np.matmul(matrix, matrix)

        # Serialize result back to binary
        output_data = struct.pack("I", n) + result.tobytes()

        compute_time_ns = time.perf_counter_ns() - compute_start
        metrics.record_compute(compute_time_ns)

        # Upload result
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
