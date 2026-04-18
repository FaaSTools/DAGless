"""
fire_cost_analysis_v3.py
========================
Computes two output CSVs for FIRE profiling accuracy validation.

1. individual_costs.csv
   Per task x provider x payload x memory: measured times and computed costs.
   Formula: total_cost_per_M = (inv_cost + total_time_s * (mem_mb/1024) * rate) * 1e6

2. fused_cost_estimates.csv
   For each pair (A, B) x provider x payload x (mem_A, mem_B):
   - Looks up individual costs from individual_costs.csv (no recomputation)
   - mem_fused = max(mem_A, mem_B)
   - Looks up fused time components from individual_costs.csv at mem_fused
   - FIRE estimate: time_fused = t_A(mem_fused) + t_B(mem_fused)
                                 - upload_A(mem_fused) - download_B(mem_fused)
   - cost_fused = inv_cost + time_fused * (mem_fused/1024) * rate
   Only rows where all four lookups (A@mem_A, B@mem_B, A@mem_fused, B@mem_fused)
   exist in individual_costs are included.

Pricing:
  AWS Lambda eu-central-1:
    inv_cost = $0.0000002/invocation
    rate     = $0.0000166667/GB-second
  GCP Cloud Run functions v2 europe-west1 (proportional CPU):
    inv_cost = $0.0000004/invocation
    rate     = $0.000026/GB-second
"""

import pandas as pd
import numpy as np
from pathlib import Path

# ── paths ──────────────────────────────────────────────────────────────────
DATA_DIR = Path(".")
OUT_DIR  = Path(".")

# ── pricing ────────────────────────────────────────────────────────────────
PRICING = {
    "AWS": {"rate": 0.0000166667 / 1024, "inv_cost": 0.0000002},
    "GCP": {"rate": 0.000026      / 1024, "inv_cost": 0.0000004},
}
M = 1e6

MEMORIES = [128, 256, 512, 768, 1024, 1536, 2048]
PAYLOADS = ["micro", "small", "medium", "large"]
TASKS = [
    "a_passthrough", "b_compress", "c_decompress", "d_csv_to_matrix",
    "e_matrix_multiply", "f_matrix_to_csv", "g_reduce", "h_expand",
    "k_latency_bound",
]

# ── load raw data ──────────────────────────────────────────────────────────
aws_warm = pd.read_csv(DATA_DIR / "aws_warm_aggregated.csv")
gcp_warm = pd.read_csv(DATA_DIR / "gcp_warm_aggregated.csv")

def safe_col(wm, col, fallback):
    """Return col if it exists and is not NaN, else fallback."""
    if col in wm.index and not pd.isna(wm[col]):
        return wm[col]
    return wm[fallback]

# ══════════════════════════════════════════════════════════════════════════
# STEP 1: build individual_costs — one row per (provider, task, payload, mem)
# ══════════════════════════════════════════════════════════════════════════
ind_rows = []
for provider, df in [("AWS", aws_warm), ("GCP", gcp_warm)]:
    p = PRICING[provider]
    rate, inv_cost = p["rate"], p["inv_cost"]
    for task in TASKS:
        for payload in PAYLOADS:
            for mem in MEMORIES:
                sub = df[
                    (df['task'] == task) &
                    (df['payload_size'] == payload) &
                    (df['memory_mb'] == mem)
                ]
                if sub.empty:
                    continue
                wm = sub.median(numeric_only=True)
                total_s    = wm['total_time_ms'] / 1000
                download_s = safe_col(wm, 'primary_download_time_ms', 'download_time_ms') / 1000
                upload_s   = safe_col(wm, 'primary_upload_time_ms',   'upload_time_ms')   / 1000
                compute_s  = wm['compute_time_ms'] / 1000

                ind_rows.append({
                    "provider":           provider,
                    "task":               task,
                    "payload_size":       payload,
                    "memory_mb":          mem,
                    "n_invocations":      len(sub),
                    "total_time_s":       round(total_s, 3),
                    "download_time_s":    round(download_s, 3),
                    "compute_time_s":     round(compute_s, 3),
                    "upload_time_s":      round(upload_s, 3),
                    "input_size_bytes":   int(wm['input_size_bytes']),
                    "output_size_bytes":  int(wm['output_size_bytes']),
                    # cost components (per million invocations)
                    "inv_cost_per_M":     round(inv_cost * M, 6),
                    "compute_cost_per_M": round(total_s * (mem/1024) * rate * M, 4),
                    "total_cost_per_M":   round((inv_cost + total_s * (mem/1024) * rate) * M, 4),
                    # for manual verification
                    "rate_per_GBs":       round(rate * 1024, 8),
                    "f_m_GBs":            round(total_s * mem/1024, 4),
                })

ind = pd.DataFrame(ind_rows)
ind.to_csv(OUT_DIR / "individual_costs.csv", index=False)
print(f"individual_costs.csv: {len(ind)} rows")

# ── build lookup index: (provider, task, payload, mem) -> row ──────────────
# This is the single source of truth — fused estimates look up from here
ind_idx = ind.set_index(["provider", "task", "payload_size", "memory_mb"])

def lookup(provider, task, payload, mem):
    """Return individual cost row, or None if not available."""
    key = (provider, task, payload, mem)
    if key in ind_idx.index:
        return ind_idx.loc[key]
    return None

# ══════════════════════════════════════════════════════════════════════════
# STEP 2: build fused_cost_estimates
# For each (A@mem_A, B@mem_B): look up from individual_costs, compute fused
# ══════════════════════════════════════════════════════════════════════════

# Transfer elimination config per chain (A, B):
# FIRE eliminates the intersection of A's output and B's input.
# For most chains: A uploads intermediate → B downloads it → both eliminated.
# For k_latency_bound → b_compress: k and b share the same input file.
#   k's output is a tiny 16-byte checksum (upload_A eliminated).
#   b's download is its own independent input, NOT k's output → not eliminated.
TRANSFER_ELIMINATION = {
    # (task_A, task_B): (eliminate_upload_A, eliminate_download_B)
    ("k_latency_bound", "b_compress"): (True, False),
    # all other pairs: standard — both eliminated
}

def get_elimination(A, B):
    return TRANSFER_ELIMINATION.get((A, B), (True, True))

fus_rows = []
for provider in ["AWS", "GCP"]:
    p = PRICING[provider]
    rate, inv_cost = p["rate"], p["inv_cost"]

    for payload in PAYLOADS:
        for A in TASKS:
            for B in TASKS:
                elim_upload_A, elim_download_B = get_elimination(A, B)

                for mem_A in MEMORIES:
                    for mem_B in MEMORIES:

                        # look up separate costs
                        rA = lookup(provider, A, payload, mem_A)
                        rB = lookup(provider, B, payload, mem_B)
                        if rA is None or rB is None:
                            continue

                        mem_fused = max(mem_A, mem_B)

                        # look up fused time components at mem_fused
                        rA_fus = lookup(provider, A, payload, mem_fused)
                        rB_fus = lookup(provider, B, payload, mem_fused)
                        if rA_fus is None or rB_fus is None:
                            continue

                        # separate cost
                        cost_A   = rA["total_cost_per_M"]
                        cost_B   = rB["total_cost_per_M"]
                        cost_sep = cost_A + cost_B

                        # FIRE fused estimate — only eliminate intersecting transfers
                        upload_eliminated   = rA_fus["upload_time_s"]   if elim_upload_A   else 0.0
                        download_eliminated = rB_fus["download_time_s"] if elim_download_B else 0.0
                        time_fused = (rA_fus["total_time_s"] + rB_fus["total_time_s"]
                                      - upload_eliminated
                                      - download_eliminated)
                        cost_fused = (inv_cost + time_fused * (mem_fused/1024) * rate) * M

                        delta = cost_fused - cost_sep

                        fus_rows.append({
                            "provider":               provider,
                            "payload_size":           payload,
                            "task_A":                 A,
                            "task_B":                 B,
                            "mem_A_mb":               mem_A,
                            "mem_B_mb":               mem_B,
                            "mem_fused_mb":           mem_fused,
                            # separate costs (looked up from individual_costs)
                            "cost_A_per_M":           round(cost_A, 4),
                            "cost_B_per_M":           round(cost_B, 4),
                            "cost_sep_per_M":         round(cost_sep, 4),
                            # fused time components at mem_fused
                            "t_A_at_fused_s":         round(rA_fus["total_time_s"], 3),
                            "t_B_at_fused_s":         round(rB_fus["total_time_s"], 3),
                            "upload_A_at_fused_s":    round(rA_fus["upload_time_s"], 3),
                            "download_B_at_fused_s":  round(rB_fus["download_time_s"], 3),
                            "upload_A_eliminated":    elim_upload_A,
                            "download_B_eliminated":  elim_download_B,
                            "time_fused_est_s":       round(time_fused, 3),
                            "cost_fused_est_per_M":   round(cost_fused, 4),
                            "delta_per_M":            round(delta, 4),
                            "decision":               "FUSE" if delta <= 0 else "SEP",
                        })

fus = pd.DataFrame(fus_rows)
fus.to_csv(OUT_DIR / "fused_cost_estimates.csv", index=False)
print(f"fused_cost_estimates.csv: {len(fus)} rows")

# sanity checks
print(f"\nNull counts:")
for col in ['time_fused_est_s', 'cost_fused_est_per_M', 'delta_per_M']:
    print(f"  {col}: {fus[col].isna().sum()}")
print(f"\nSEP: {(fus['decision']=='SEP').sum()}  FUSE: {(fus['decision']=='FUSE').sum()}")
