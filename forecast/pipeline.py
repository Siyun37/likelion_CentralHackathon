# -*- coding: utf-8 -*-
"""
PHANTOM STOCK 수요 예측 파이프라인 v2.0
수식 명세(phantom_stock_수요예측_수식_최종.md) 구현 — 800행 더미데이터 부하 테스트용

단계: 0 위생 → 1 행동 스코어(L2 로지스틱 + 고객 클러스터 부트스트랩)
     → 2 품질 게이트 + 사후층화 가중 → 3 확정층 + 확산층(Bass 시나리오)
     → 4 컬러 분해(Dirichlet) → 5 뉴스벤더(추정 불확실성 ⊗ 수요 변동성)

주의: 더미데이터 테스트 = 파이프라인 검증용. 모델 성능 검증 아님.

실행법
  - Colab/Jupyter:  main(DATA_PATH)   또는 그냥 셀 전체 실행
  - 터미널:          python3 pipeline.py "Phantom stock dummy 800.csv"
"""
import json, sys
import numpy as np
import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.preprocessing import StandardScaler

RNG = np.random.default_rng(20260819)

# ───────────────────────── 경로 설정 (환경 자동 감지) ─────────────────────────
import os, glob

DATA_FILENAME = "Phantom stock dummy 800.csv"

def _resolve_data_path(fname=DATA_FILENAME):
    """작업디렉터리 → /content → /mnt/data → /mnt/user-data/outputs 순으로 탐색.
    공백 포함 파일명, 대소문자·언더바 변형(phantom_stock_dummy_800.csv)도 함께 시도."""
    cands = [fname, fname.replace(" ", "_"), fname.lower(), fname.lower().replace(" ", "_")]
    roots = [".", "/content", "/content/drive/MyDrive", "/mnt/data", "/mnt/user-data/uploads",
             "/mnt/user-data/outputs"]
    for r in roots:
        for c in cands:
            p = os.path.join(r, c)
            if os.path.exists(p):
                return p
    hits = []
    for r in roots:
        hits += glob.glob(os.path.join(r, "*dummy*800*.csv"))
    if hits:
        return hits[0]
    raise FileNotFoundError(
        f"'{fname}' 를 찾지 못했습니다. 현재 위치: {os.getcwd()}\n"
        f"  → main('전체/경로/파일.csv') 처럼 직접 지정하세요.")

def _resolve_out_dir():
    for d in ["/mnt/user-data/outputs", "/content", "."]:
        if os.path.isdir(d) and os.access(d, os.W_OK):
            return d
    return "."

OUT_DIR = _resolve_out_dir()

# ───────────────────────── 설정 (미확정 파라미터 대장 §7) ─────────────────────────
CFG = dict(
    # [가정] 시즌·확산 prior — MCM 과거 유사 SKU 주차 실적 확보 시 역적합으로 교체
    SEASON_WEEKS=16,
    P_WEEKLY=0.06, Q_WEEKLY=0.50,      # t* = ln(q/p)/(p+q) ≈ 3.8주 (시즌 초반 1/3 제약 충족)
    # [외부 입력] 전 시즌 실적 — MD 보유 수치로 교체
    PREV_PRODUCED=3000, PREV_SOLD=1950, DEMAND_GROWTH=0.0,
    # [미확인] 전시 예상 총 관람객 — 팝업 기간 × 일 방문객
    V_EXPECTED=3000,
    # [미확인] 등록→구매 전환율 — 시나리오 병기
    KAPPA_SCENARIOS=[0.2, 0.4, 0.6],
    KAPPA_BASE=0.4,
    # 수축: gamma = n_eff/(n_eff+n0)
    N0_SHRINK=30,
    # 품질 게이트 (§2.1)
    GATE_IDLE_MAX=0.15, GATE_SKIP_MAX=0.50, GATE_N_MIN=15,
    DEMO_MODE=True,          # True: 표본수(n_min) 조건 무시 — 소규모 실데이터 데모용
    DEMO_N_MIN=3,            # DEMO_MODE일 때 최소 표본(0에 가까우면 노이즈만 있는 세그먼트까지 통과)
    W_MAX=3.0,
    # [가정] 시장 고객 구성비 (연령대) — MCM 제공 시 교체
    MARKET_MIX={"20대": 0.30, "30대": 0.35, "40대": 0.22, "50대": 0.13},
    # 부트스트랩·시뮬레이션
    B_BOOT=500, NB_DISPERSION=8.0,      # [가정] 음이항 산포 φ
    # [외부 입력] 뉴스벤더 비용 — 단위 마진/처분 실적으로 교체
    C_UNDER=550_000,                     # 품절 기회비용(단위 마진 가정)
    C_OVER=380_000,                      # 과잉 비용(할인손실+보관+폐기불가 처리·보고)
    C_OVER_PRE_REG=250_000,              # [비교용] 규제 이전 과잉비용
    DIRICHLET_PRIOR=2.0,
    L2_C=1.0,
)

PRODUCT_NAMES = {1:"Stark",2:"Ella",3:"Aren",4:"Dessau",5:"Tracy",6:"Pina"}
MULTI_COLOR_PIDS = [1, 4]   # §4: 멀티컬러 SKU만 분해

# ───────────────────────── 0단. 데이터 위생 ─────────────────────────
def stage0_load(path_csv):
    df = pd.read_csv(path_csv)
    n_all = len(df)
    df = df[df["excluded"] == "N"].copy()          # 방치(>600s) 제외
    # 비중첩 시간 근사: 순수응시 = view - detail - tryon (음수 클램프)
    # ⚠️ 더미 한계: 실빌드에서는 이벤트 구간 교집합으로 정확 분리할 것
    df["pure_view"] = (df["view_sec"] - df["detail_sec"].fillna(0)
                       - df["tryon_sec"].fillna(0)).clip(lower=0.0)
    df["revisit"] = (df["view_cnt"] > 1).astype(int)
    df["zoomed"]  = (df["zoom_max"] > 0).astype(int)
    df["seg"] = df["age_group"]                    # 게이트 세그먼트 = 연령대 (성별 결합 가능)
    print(f"[0단] 로드 {n_all}행 → 유효 {len(df)}행 / 고객 {df.customer_id.nunique()}명 "
          f"(방치 제외 {n_all-len(df)}건)")
    return df

# ───────────────────────── 1단. 행동 스코어 ─────────────────────────
FEATS = ["log_pv","revisit","zoom_max","log_dt","log_ty"]

def make_X(df):
    X = pd.DataFrame({
        "log_pv": np.log1p(df["pure_view"]),
        "revisit": df["revisit"].astype(float),
        "zoom_max": df["zoom_max"].fillna(0.0),
        "log_dt": np.log1p(df["detail_sec"].fillna(0.0)),
        "log_ty": np.log1p(df["tryon_sec"].fillna(0.0)),
    }, index=df.index)
    pos = pd.get_dummies(df["stool_position"], prefix="POS", dtype=float)
    return pd.concat([X, pos], axis=1), list(pos.columns)

def fit_score_model(df):
    """L2 로지스틱. 타깃=prereg, 피처=행동만(라벨 누수 차단) + 위치 더미(통제)."""
    Xfull, pos_cols = make_X(df)
    y = df["prereg"].values
    scaler = StandardScaler().fit(Xfull[FEATS])
    Xs = Xfull.copy(); Xs[FEATS] = scaler.transform(Xfull[FEATS])
    try:   # sklearn >= 1.8
        clf = LogisticRegression(C=CFG["L2_C"], l1_ratio=0, max_iter=2000)
    except TypeError:  # sklearn < 1.8
        clf = LogisticRegression(penalty="l2", C=CFG["L2_C"], max_iter=2000)
    clf.fit(Xs.values, y)
    return clf, scaler, pos_cols, Xfull

def predict_score(df, clf, scaler, pos_cols):
    """예측 시 위치를 전 위치 평균으로 중립화 (§1.3)."""
    Xfull, _ = make_X(df)
    Xs = Xfull.copy(); Xs[FEATS] = scaler.transform(Xfull[FEATS])
    Xs[pos_cols] = 1.0 / len(pos_cols)             # POS 평균 고정
    # 학습 시 컬럼과 정렬
    for c in pos_cols:
        if c not in Xs: Xs[c] = 1.0/len(pos_cols)
    return clf.predict_proba(Xs.values)[:, 1]

# ───────────────────────── 2단. 게이트 + 가중 ─────────────────────────
def stage2_weights(df_raw_all, df):
    rep = []
    gate_pass = {}
    for g, sub_all in df_raw_all.groupby("seg"):
        custs = sub_all.customer_id.nunique()
        idle_rate = (sub_all["excluded"] == "Y").mean()
        skip_rate = sub_all.groupby("customer_id")["profile_skipped"].first().eq("Y").mean()
        n_min = CFG["DEMO_N_MIN"] if CFG["DEMO_MODE"] else CFG["GATE_N_MIN"]
        ok = (idle_rate < CFG["GATE_IDLE_MAX"]) and (skip_rate < CFG["GATE_SKIP_MAX"]) and (custs >= n_min)
        gate_pass[g] = ok
        rep.append((g, custs, idle_rate, skip_rate, "통과" if ok else "탈락→측정불가"))
    print("[2단] 품질 게이트:" + ("  ⚠️ DEMO_MODE=True → 표본수(n_min) 조건 무시, 방치율·skip률만 적용" if CFG["DEMO_MODE"] else ""))
    for g,c,i,s,st in rep:
        print(f"   {g}: 고객{c} 방치율{i:.1%} skip률{s:.1%} → {st}")
    # 표본 구성비(통과 세그먼트, 고객 기준)
    cust_seg = df.groupby("customer_id")["seg"].first()
    passed = [g for g,ok in gate_pass.items() if ok]
    sample_mix = cust_seg[cust_seg.isin(passed)].value_counts(normalize=True)
    mkt = pd.Series(CFG["MARKET_MIX"], dtype=float)
    mkt_p = (mkt[passed] / mkt[passed].sum())       # 통과 세그먼트 내 재정규화
    w_seg = (mkt_p / sample_mix).clip(upper=CFG["W_MAX"])
    df = df[df["seg"].isin(passed)].copy()
    df["w"] = df["seg"].map(w_seg)
    excluded_share = 1 - mkt[passed].sum() / mkt.sum()
    print(f"   → 가중치 {dict(w_seg.round(3))} / 측정불가 시장비중 {excluded_share:.1%} (불확실성 확대 사유)")
    return df, gate_pass, excluded_share

# ───────────────────────── 3단. 확정층 + 확산층 ─────────────────────────
def bass_weekly(m, p, q, T):
    t = np.arange(1, T+1, dtype=float)
    F = (1 - np.exp(-(p+q)*t)) / (1 + (q/p)*np.exp(-(p+q)*t))
    F = np.concatenate([[0.0], F])
    return m * np.diff(F)                            # 주차별 신규수요

def stage3_allocation(df, scores):
    df = df.assign(s=scores)
    agg = df.groupby("product_id").apply(
        lambda g: pd.Series({
            "score_raw": np.average(g["s"], weights=g["w"]),
            "n_eff": g["customer_id"].nunique(),
            "R": g["prereg"].sum(),                  # 확정층 실측 (여기 한 번만 사용)
        }), include_groups=False)
    gamma = agg["n_eff"] / (agg["n_eff"] + CFG["N0_SHRINK"])   # 수축 지수
    agg["score_shrunk"] = agg["score_raw"] ** gamma
    kap = CFG["KAPPA_BASE"]
    D_anchor = CFG["PREV_SOLD"] * (1 + CFG["DEMAND_GROWTH"])   # 수요 앵커 = 전 시즌 실판매
    n_visitors = df["customer_id"].nunique()
    agg["reg_rate"] = agg["R"] / n_visitors
    agg["confirmed"] = agg["reg_rate"] * CFG["V_EXPECTED"] * kap   # 표본 등록률 → 예상 관람객 외삽
    confirmed_total = agg["confirmed"].sum()
    D_diff = max(D_anchor - confirmed_total, 0)      # 확정층 선차감 (이중 계상 금지)
    agg["share"] = agg["score_shrunk"] / agg["score_shrunk"].sum()
    agg["m_i"] = D_diff * agg["share"]
    agg["D_total"] = agg["confirmed"] + agg["m_i"]
    agg.attrs["D_anchor"] = D_anchor
    agg.attrs["confirmed_total"] = confirmed_total
    agg.attrs["n_visitors"] = n_visitors
    return agg, gamma

# ───────────────────────── 5단. 부트스트랩 ⊗ 음이항 → Q* ─────────────────────────
def simulate_demand(df, agg):
    """예측분포 = 추정 불확실성(고객 클러스터 부트스트랩) ⊗ 수요 변동성(음이항)"""
    custs = df["customer_id"].unique()
    p, q, T = CFG["P_WEEKLY"], CFG["Q_WEEKLY"], CFG["SEASON_WEEKS"]
    Fcov = (1 - np.exp(-(p+q)*T)) / (1 + (q/p)*np.exp(-(p+q)*T))
    kap = CFG["KAPPA_BASE"]
    D_anchor = agg.attrs["D_anchor"]; nv = agg.attrs["n_visitors"]
    by_c = {c: g for c, g in df.groupby("customer_id")}
    sims = {pid: [] for pid in agg.index}
    for _ in range(CFG["B_BOOT"]):
        bc = RNG.choice(custs, size=len(custs), replace=True)
        dfb = pd.concat([by_c[c] for c in bc], ignore_index=True)
        gb = dfb.groupby("product_id").apply(
            lambda g: pd.Series({"sc": np.average(g["s"], weights=g["w"]),
                                 "ne": g["customer_id"].nunique(),
                                 "R": g["prereg"].sum()}), include_groups=False)
        gb = gb.reindex(agg.index).fillna({"sc": 1e-6, "ne": 1, "R": 0})
        gam = gb["ne"]/(gb["ne"]+CFG["N0_SHRINK"])
        sh = gb["sc"]**gam; sh = sh/sh.sum()
        conf = (gb["R"]/nv) * CFG["V_EXPECTED"] * kap
        Ddiff = max(D_anchor - conf.sum(), 0)
        for pid in agg.index:
            mean_d = conf[pid] + Ddiff*sh[pid]*Fcov
            phi = CFG["NB_DISPERSION"]
            sims[pid].append(RNG.negative_binomial(phi, phi/(phi+mean_d)) if mean_d > 0 else 0)
    return {pid: np.array(v) for pid, v in sims.items()}

def newsvendor_free(sims, c_u, c_o):
    """예산 제약 없음 → 총 생산량이 '도출'된다 (이번 시즌 몇 개 만들지의 답)"""
    cr = c_u/(c_u+c_o)
    return {pid: float(np.percentile(a, cr*100)) for pid, a in sims.items()}, cr

def newsvendor_capped(sims, c_u, c_o, budget):
    """예산 상한 有: c_u→c_u−λ 로 낮추며 ΣQ*=budget 되는 λ 이분탐색 (λ=캐파 그림자가격)"""
    lo, hi = 0.0, c_u*0.999
    for _ in range(60):
        lam = (lo+hi)/2
        cr = max((c_u-lam), 1e-9)/((c_u-lam)+c_o)
        tot = sum(np.percentile(a, cr*100) for a in sims.values())
        if tot > budget: lo = lam
        else: hi = lam
    lam = (lo+hi)/2
    cr = max((c_u-lam), 1e-9)/((c_u-lam)+c_o)
    return {pid: float(np.percentile(a, cr*100)) for pid, a in sims.items()}, lam, cr



# ───────────────────────── 실행 ─────────────────────────
def main(path_csv=None):
    path_csv = path_csv or _resolve_data_path()
    print("="*74)
    print(f"[입력] {path_csv}\n[출력] {OUT_DIR}")
    df_all = pd.read_csv(path_csv); df_all["seg"] = df_all["age_group"]
    df = stage0_load(path_csv)

    clf, scaler, pos_cols, _ = fit_score_model(df)
    print(f"[1단] 계수(표준화): {dict(zip(FEATS, clf.coef_[0][:len(FEATS)].round(3)))}")

    df, gate, excl_share = stage2_weights(df_all, df)
    df["s"] = predict_score(df, clf, scaler, pos_cols)
    agg, gamma = stage3_allocation(df, df["s"].values)

    print(f"\n[3단] 수요 앵커 {agg.attrs['D_anchor']:.0f}개 "
          f"(= 전 시즌 실판매 {CFG['PREV_SOLD']} × 성장 {CFG['DEMAND_GROWTH']:+.0%})")
    print(f"      확정층 {agg.attrs['confirmed_total']:.0f}개 "
          f"(표본 등록률 → 예상 관람객 {CFG['V_EXPECTED']}명 외삽 × κ={CFG['KAPPA_BASE']})")
    for pid, r in agg.sort_values("share", ascending=False).iterrows():
        print(f"   {PRODUCT_NAMES[pid]:7s} share={r.share:5.1%} 확정={r.confirmed:6.1f} "
              f"확산={r.m_i:6.1f} 기대수요={r.D_total:7.1f}")

    print("\n[4단] 컬러 분해 (멀티컬러 한정):")
    COLOR_KO = {"black":"검정","cognac":"꼬냑","green":"녹색","white":"흰색","lotus":"연꽃색"}
    color_out = {}
    for pid in MULTI_COLOR_PIDS:
        cnt = df[df.product_id==pid]["final_color"].value_counts()
        cnt.index = [COLOR_KO.get(str(c).lower(), c) for c in cnt.index]
        cnt = cnt.groupby(cnt.index).sum()
        a = cnt + CFG["DIRICHLET_PRIOR"]; th = (a/a.sum()).round(3)
        color_out[pid] = {k: float(v) for k, v in th.items()}
        print(f"   {PRODUCT_NAMES[pid]}: {color_out[pid]}")

    sims = simulate_demand(df, agg)
    Qfree, cr = newsvendor_free(sims, CFG["C_UNDER"], CFG["C_OVER"])
    total_free = sum(Qfree.values())
    Qcap, lam, cr_cap = newsvendor_capped(sims, CFG["C_UNDER"], CFG["C_OVER"], CFG["PREV_PRODUCED"])
    Qold, cr_old = newsvendor_free(sims, CFG["C_UNDER"], CFG["C_OVER_PRE_REG"])

    print(f"\n[5단] 뉴스벤더")
    print(f"   임계비율(현행 규제) = {cr:.3f} → 권장 총 생산 {total_free:.0f}개")
    print(f"   임계비율(규제 이전) = {cr_old:.3f} → {sum(Qold.values()):.0f}개  "
          f"(폐기 금지로 인한 감소분 {sum(Qold.values())-total_free:+.0f}개)")
    print(f"   예산 {CFG['PREV_PRODUCED']}개 상한 모드: λ(캐파 그림자가격)={lam:,.0f}원/개, ΣQ*={sum(Qcap.values()):.0f}")

    rows = []
    for pid in agg.index:
        a = sims[pid]
        rows.append(dict(product=PRODUCT_NAMES[pid], share=agg.loc[pid,"share"],
                         confirmed=agg.loc[pid,"confirmed"], D_mean=a.mean(),
                         D_p10=np.percentile(a,10), D_p90=np.percentile(a,90),
                         Q_star=Qfree[pid], Q_capped=Qcap[pid]))
    res = pd.DataFrame(rows).set_index("product").round(1)

    prev_prod, prev_sold = CFG["PREV_PRODUCED"], CFG["PREV_SOLD"]
    prev_left = prev_prod - prev_sold
    exp_sold = float(sum(np.minimum(sims[pid], Qfree[pid]).mean() for pid in agg.index))
    exp_left = total_free - exp_sold
    print("\n" + "="*74)
    print("[전 시즌 대비 생산 권고]")
    print(f"   전 시즌: 생산 {prev_prod:,}개 → 판매 {prev_sold:,}개 / 잔여 {prev_left:,}개 "
          f"(소진율 {prev_sold/prev_prod:.1%})")
    print(f"   이번 시즌 권고: {total_free:,.0f}개  ({total_free/prev_prod-1:+.1%})")
    print(f"   기대 판매 {exp_sold:,.0f} / 기대 잔여 {exp_left:,.0f} (소진율 {exp_sold/total_free:.1%})")
    print(f"   잔여 감소 {prev_left-exp_left:,.0f}개 → 과잉비용 절감 "
          f"{(prev_left-exp_left)*CFG['C_OVER']/1e8:.2f}억원 [가정 단가 기준]")
    print("   ⚠️ 위 절감액은 '본 모델의 예측이 맞다'는 가정 하의 값 — 파일럿 전 검증 불가")
    print("="*74)

    res.to_csv(os.path.join(OUT_DIR, "phantom_stock_forecast_result.csv"), encoding="utf-8-sig")
    curves = {}
    for pid, r in agg.iterrows():
        c = bass_weekly(r["m_i"], CFG["P_WEEKLY"], CFG["Q_WEEKLY"], CFG["SEASON_WEEKS"])
        c[0] += r["confirmed"]; curves[PRODUCT_NAMES[pid]] = [round(x,1) for x in c]
    blob = dict(
        assumptions={k: v for k, v in CFG.items()},
        gate={k: bool(v) for k, v in gate.items()}, unmeasured_market_share=round(excl_share,3),
        color_split={PRODUCT_NAMES[k]: v for k, v in color_out.items()},
        weekly_curves=curves, critical_ratio=round(cr,3), lambda_shadow_price=round(lam),
        totals=dict(prev_produced=prev_prod, prev_sold=prev_sold, prev_leftover=prev_left,
                    recommended=round(total_free,1), delta_pct=round(total_free/prev_prod-1,4),
                    expected_sold=round(exp_sold,1), expected_leftover=round(exp_left,1),
                    pre_regulation_total=round(sum(Qold.values()),1)),
        result=json.loads(res.to_json(orient="index", force_ascii=False)))
    with open(os.path.join(OUT_DIR, "phantom_stock_forecast_result.json"), "w", encoding="utf-8") as f:
        json.dump(blob, f, ensure_ascii=False, indent=2)
    print(f"\n저장: {OUT_DIR}/phantom_stock_forecast_result.csv / .json")
    print(res.to_string())
    return blob



def _in_notebook():
    try:
        from IPython import get_ipython
        return get_ipython() is not None and "IPKernelApp" in get_ipython().config
    except Exception:
        return False

if __name__ == "__main__":
    # 노트북 커널은 argv에 '-f kernel.json'을 넣으므로 .csv 인자만 취한다
    _args = [a for a in sys.argv[1:] if a.lower().endswith(".csv")]
    main(_args[0] if _args else None)
