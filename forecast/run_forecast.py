# -*- coding: utf-8 -*-
"""
PHANTOM STOCK — GitHub Actions용 실행 스크립트
Colab 셀 2(연결)~5(저장)를 하나로 합친 버전.
환경변수(GitHub Secrets)로 키를 받는다는 점만 Colab과 다름.
"""
import os, sys, importlib.util
import pandas as pd

# ── 1. Supabase 연결 (Secrets에서 키 로드) ──────────────────────
from supabase import create_client

SB_URL = os.environ["SB_URL"]
SB_KEY = os.environ["SB_SECRET"]     # service_role 키 — 반드시 Secrets로만
sb = create_client(SB_URL, SB_KEY)

probe = sb.table("v_customer_product").select("customer_id").limit(1).execute()
if not probe.data:
    print("⚠️ v_customer_product 가 비어 있습니다. 실행을 중단합니다.")
    sys.exit(1)
print("Supabase 연결 성공")

# ── 2. 데이터 읽기 (페이지네이션) ──────────────────────────────
def fetch_all(table, page=1000):
    out, start = [], 0
    while True:
        r = sb.table(table).select("*").range(start, start + page - 1).execute().data
        out += r
        if len(r) < page:
            break
        start += page
    return pd.DataFrame(out)

df = fetch_all("v_customer_product")
print(f"불러온 행 {len(df)} / 고객 {df.customer_id.nunique()}명")

for c in ["view_sec","zoom_max","detail_sec","tryon_sec","price","age","height","weight",
          "view_cnt","color_change_cnt","tryon_cnt","prereg","dummy_height","product_id"]:
    if c in df:
        df[c] = pd.to_numeric(df[c], errors="coerce").fillna(0)

A = {r["key"]: float(r["value"]) for r in sb.table("forecast_assumptions").select("*").execute().data}
print("가정 파라미터:", A)

# ── 3. 모델 실행 (같은 폴더의 pipeline.py) ─────────────────────
spec = importlib.util.spec_from_file_location(
    "ps_pipeline", os.path.join(os.path.dirname(__file__), "pipeline.py"))
ps = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ps)

ps.CFG.update(
    KAPPA_BASE     = A.get("kappa", 0.4),
    P_WEEKLY       = A.get("p_weekly", 0.06),
    Q_WEEKLY       = A.get("q_weekly", 0.50),
    SEASON_WEEKS   = int(A.get("season_weeks", 16)),
    V_EXPECTED     = int(A.get("v_expected", 3000)),
    PREV_PRODUCED  = int(A.get("prev_produced", 3000)),
    PREV_SOLD      = int(A.get("prev_sold", 1950)),
    DEMAND_GROWTH  = A.get("demand_growth", 0.0),
    C_UNDER        = A.get("c_under", 550000),
    C_OVER         = A.get("c_over", 380000),
    C_OVER_PRE_REG = A.get("c_over_pre_reg", 250000),
    NB_DISPERSION  = A.get("nb_dispersion", 8.0),
)

tmp_csv = os.path.join(os.path.dirname(__file__), "_supabase_snapshot.csv")
df.to_csv(tmp_csv, index=False, encoding="utf-8-sig")
blob = ps.main(tmp_csv)

if blob is None:
    print("⚠️ 모델이 결과를 반환하지 않았습니다 (blob is None). 중단합니다.")
    sys.exit(1)

# ── 4. 결과를 Supabase에 저장 ───────────────────────────────
t = blob["totals"]
run = sb.table("forecast_runs").insert({
    "n_rows": int(len(df)),
    "n_customers": int(df.customer_id.nunique()),
    "recommended_total": t["recommended"],
    "prev_produced": t["prev_produced"],
    "prev_sold": t["prev_sold"],
    "expected_sold": t["expected_sold"],
    "expected_leftover": t["expected_leftover"],
    "pre_regulation_total": t["pre_regulation_total"],
    "critical_ratio": blob["critical_ratio"],
    "lambda_shadow": blob["lambda_shadow_price"],
    "gate": blob["gate"],
    "unmeasured_share": blob["unmeasured_market_share"],
    "assumptions": {k: v for k, v in ps.CFG.items() if isinstance(v, (int, float))},
    "note": "GitHub Actions 자동 실행",
}).execute().data[0]
rid = run["run_id"]

sb.table("forecast_sku").insert([
    {"run_id": rid, "product": k, "share": v["share"], "confirmed": v["confirmed"],
     "d_mean": v["D_mean"], "d_p10": v["D_p10"], "d_p90": v["D_p90"],
     "q_star": v["Q_star"], "q_capped": v["Q_capped"]}
    for k, v in blob["result"].items()]).execute()

sb.table("forecast_curves").insert(
    [{"run_id": rid, "product": k, "weekly": v} for k, v in blob["weekly_curves"].items()]).execute()

sb.table("forecast_colors").insert(
    [{"run_id": rid, "product": k, "split": v} for k, v in blob["color_split"].items()]).execute()

os.remove(tmp_csv)
print(f"\n저장 완료 run_id = {rid}")
print(f"권장 총 생산량 {t['recommended']:.0f}개 "
      f"(전 시즌 {t['prev_produced']}개 대비 {t['recommended']/t['prev_produced']-1:+.1%})")
