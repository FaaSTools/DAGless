"""
Task C: Gzip Decompression (Medium Compute)

Action: Download gzipped data from storage -> Decompress -> Upload to storage
Purpose: Inverse of Task B, enables chains like: CSV → B (compress) → C (decompress) → CSV
         or starting from compressed data: Gzip → C → CSV → D → E → F → CSV

Input: Gzip compressed data
Output: Decompressed data (original format)
"""

import os
import time
import gzip

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
        task="c_decompress",
        payload_size=payload_size,
        memory_mb=memory_mb,
        provider=provider,
        region=region
    )
    metrics.set_cold_start(cold_start)

    try:
        storage = get_storage_provider()

        # Download compressed data
        data, download_time_ns, input_size = storage.download_bytes(input_bucket, input_key)
        metrics.record_download(download_time_ns, input_size)

        # Decompress
        compute_start = time.perf_counter_ns()
        decompressed_data = gzip.decompress(data)
        compute_time_ns = time.perf_counter_ns() - compute_start
        metrics.record_compute(compute_time_ns)

        # Upload decompressed data
        upload_time_ns, output_size = storage.upload_bytes(decompressed_data, output_bucket, output_key)
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
