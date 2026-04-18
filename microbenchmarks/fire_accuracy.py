"""
fire_accuracy.py
================
Computes FIRE profiling accuracy by comparing estimated fused costs
against measured fused costs.

Inputs:
  - fused_cost_estimates.csv  (from fire_cost_analysis_v3.py)
  - fused_aws.csv             (measured fused function results, AWS)
  - fused_gcp.csv             (measured fused function results, GCP)

Output:
  - fire_accuracy.csv         (per configuration: measured vs estimated cost/time + error %)

Chain mapping (fused task name -> individual task A, task B):
  ae_passthrough_matrix_multiply -> a_passthrough   + e_matrix_multiply
  ce_decompress_matrix_multiply  -> c_decompress    + e_matrix_multiply
  gh_reduce_expand               -> g_reduce        + h_expand
  kb_latency_compress            -> k_latency_bound + b_compress

Accuracy is computed for each (provider, chain, payload, memory) configuration
where both a measured fused result and a FIRE estimate exist.
The estimate is looked up at mem_A_mb == mem_B_mb == memory_mb (symmetric config).

Pricing:
  AWS Lambda eu-central-1:
    inv_cost = $0.0000002/invocation
    rate     = $0.0000166667/GB-second
  GCP Cloud Run functions v2 europe-west1:
    inv_cost = $0.0000004/invocation
    rate     = $0.000026/GB-second
"""

import pandas as pd
from pathlib import Path

# ── paths ──────────────────────────────────────────────────────────────────
EST_DIR    = Path(".")
FUSED_DIR  = Path(".")
OUT_DIR    = Path(".")

# ── pricing ────────────────────────────────────────────────────────────────
PRICING = {
    "AWS": {"rate": 0.0000166667 / 1024, "inv_cost": 0.0000002},
    "GCP": {"rate": 0.000026      / 1024, "inv_cost": 0.0000004},
}
M = 1e6

# ── chain mapping ──────────────────────────────────────────────────────────
TASK_MAP = {
    "ae_passthrough_matrix_multiply": ("a_passthrough",   "e_matrix_multiply"),
    "ce_decompress_matrix_multiply":  ("c_decompress",    "e_matrix_multiply"),
    "gh_reduce_expand":               ("g_reduce",        "h_expand"),
    "kb_latency_compress":            ("k_latency_bound", "b_compress"),
}

# ── load inputs ────────────────────────────────────────────────────────────
fus_est = pd.read_csv(EST_DIR / "fused_cost_estimates.csv")

aws = pd.read_csv(FUSED_DIR / "fused_aws.csv")
gcp = pd.read_csv(FUSED_DIR / "fused_gcp.csv")

# unify task names (strip leading "task_" prefix if present)
for df in [aws, gcp]:
    df["task"] = df["task"].str.replace("^task_", "", regex=True)

aws_warm = aws[aws["cold_start"] == False]
gcp_warm = gcp[gcp["cold_start"] == False]

# ── compute accuracy ───────────────────────────────────────────────────────
rows = []
for provider, df, p in [
    ("AWS", aws_warm, PRICING["AWS"]),
    ("GCP", gcp_warm, PRICING["GCP"]),
]:
    rate     = p["rate"]
    inv_cost = p["inv_cost"]

    for fused_task, (A, B) in TASK_MAP.items():
        sub = df[df["task"] == fused_task]
        if sub.empty:
            continue

        for payload in ["micro", "small", "medium", "large"]:
            for mem in [128, 256, 512, 768, 1024, 1536, 2048]:

                # measured fused
                s = sub[(sub["payload_size"] == payload) & (sub["memory_mb"] == mem)]
                if s.empty:
                    continue
                meas_t = s["total_time_ms"].median() / 1000
                meas_c = (inv_cost + meas_t * (mem / 1024) * rate) * M

                # FIRE estimate — symmetric config: mem_A == mem_B == mem
                est = fus_est[
                    (fus_est["provider"]    == provider) &
                    (fus_est["task_A"]      == A) &
                    (fus_est["task_B"]      == B) &
                    (fus_est["payload_size"]== payload) &
                    (fus_est["mem_A_mb"]    == mem) &
                    (fus_est["mem_B_mb"]    == mem)
                ]
                if est.empty:
                    continue

                est_t = est["time_fused_est_s"].values[0]
                est_c = est["cost_fused_est_per_M"].values[0]

                rows.append({
                    "provider":          provider,
                    "fused_task":        fused_task,
                    "task_A":            A,
                    "task_B":            B,
                    "payload_size":      payload,
                    "memory_mb":         mem,
                    "n_invocations":     len(s),
                    "measured_time_s":   round(meas_t, 3),
                    "estimated_time_s":  round(est_t, 3),
                    "err_time_pct":      round(abs(est_t - meas_t) / meas_t * 100, 2),
                    "measured_cost_M":   round(meas_c, 4),
                    "estimated_cost_M":  round(est_c, 4),
                    "err_cost_pct":      round(abs(est_c - meas_c) / meas_c * 100, 2),
                })

acc = pd.DataFrame(rows)
acc.to_csv(OUT_DIR / "fire_accuracy.csv", index=False)

# ── summary ────────────────────────────────────────────────────────────────
print(f"fire_accuracy.csv: {len(acc)} configurations")
print(f"\nMean cost error: {acc['err_cost_pct'].mean():.2f}%")
print(f"Max  cost error: {acc['err_cost_pct'].max():.2f}%")
print(f"Rows > 10%: {(acc['err_cost_pct'] > 10).sum()}")
print(f"Rows >  5%: {(acc['err_cost_pct'] >  5).sum()}")

print("\nMean cost error per chain:")
for fused_task in TASK_MAP:
    for provider in ["AWS", "GCP"]:
        sub = acc[(acc["fused_task"] == fused_task) & (acc["provider"] == provider)]
        if sub.empty:
            continue
        print(f"  {provider} {fused_task}: {sub['err_cost_pct'].mean():.2f}% "
              f"(max {sub['err_cost_pct'].max():.2f}%)")
