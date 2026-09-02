import { useEffect, useMemo, useRef, useState } from "react";
import "@/App.css";
import { api, auth, unwrap } from "@/api";

/* ---------------- toast helper (tiny) ---------------- */
let _toastId = 0;
const listeners = new Set();
const toast = (msg, type = "info") => {
  const id = ++_toastId;
  listeners.forEach((fn) => fn({ id, msg, type, add: true }));
  setTimeout(() => listeners.forEach((fn) => fn({ id, remove: true })), 3200);
};
function Toaster() {
  const [items, setItems] = useState([]);
  useEffect(() => {
    const fn = (e) =>
      setItems((cur) => (e.remove ? cur.filter((i) => i.id !== e.id) : [...cur, e]));
    listeners.add(fn);
    return () => listeners.delete(fn);
  }, []);
  return (
    <div className="te-toast-wrap">
      {items.map((i) => (
        <div key={i.id} className={`te-toast te-toast--${i.type}`} data-testid={`toast-${i.type}`}>{i.msg}</div>
      ))}
    </div>
  );
}

/* ---------------- root ---------------- */
export default function App() {
  const [session, setSession] = useState(() => ({
    token: auth.getToken(),
    user: auth.getUser(),
  }));

  const onAuthed = (token, user) => {
    auth.set(token, user);
    setSession({ token, user });
  };
  const onLogout = () => {
    auth.clear();
    setSession({ token: null, user: null });
    toast("Logged out", "info");
  };

  return (
    <>
      <Toaster />
      {!session.token ? (
        <AuthFlow onAuthed={onAuthed} />
      ) : !session.user?.profileComplete ? (
        <ProfileForm user={session.user} onAuthed={onAuthed} />
      ) : (
        <Dashboard user={session.user} onLogout={onLogout} onUserUpdate={(u) => { auth.set(session.token, u); setSession((s) => ({ ...s, user: u })); }} />
      )}
    </>
  );
}

/* ---------------- auth: mobile → OTP → dashboard ---------------- */
function BrandPanel() {
  return (
    <aside className="te-auth__brand">
      <div className="te-brand__logo">
        <span className="te-brand__logo-dot">TE</span>
        TAXEDGE
      </div>
      <div className="te-brand__hero">
        <h1>India's compliance stack, <span>in one place.</span></h1>
        <p>File GST returns, track ITR, manage TDS deductions, apply for loans, and stash every document — all wired to a single mobile-verified account.</p>
      </div>
      <div className="te-brand__stats">
        <div className="te-brand__stat"><b>10</b><span>Modules</span></div>
        <div className="te-brand__stat"><b>OTP</b><span>Passwordless</span></div>
        <div className="te-brand__stat"><b>JWT</b><span>Secured</span></div>
      </div>
    </aside>
  );
}

function AuthFlow({ onAuthed }) {
  const [mode, setMode] = useState("otp"); // "otp" | "password"
  const [step, setStep] = useState("mobile"); // mobile → otp (only used when mode=otp)
  const [mobile, setMobile] = useState("");
  const [password, setPassword] = useState("");
  const [devOtp, setDevOtp] = useState("");
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");

  const requestOtp = async (e) => {
    e?.preventDefault();
    setErr("");
    if (!/^\d{10}$/.test(mobile)) return setErr("Enter a valid 10-digit mobile number");
    setLoading(true);
    try {
      const { data } = await api.post("/auth/login", { mobile });
      setDevOtp(data.devOtp || "");
      setStep("otp");
      toast("OTP sent — dev code is prefilled", "success");
    } catch (e2) {
      setErr(e2?.response?.data?.message || "Could not send OTP");
    } finally { setLoading(false); }
  };

  const passwordLogin = async (e) => {
    e?.preventDefault();
    setErr("");
    if (!/^\d{10}$/.test(mobile)) return setErr("Enter a valid 10-digit mobile number");
    if (!password) return setErr("Enter your password");
    setLoading(true);
    try {
      const r = await api.post("/auth/password-login", { mobile, password });
      const { token, user } = r.data.data;
      toast(`Welcome back, ${user.fullName || "user"}`, "success");
      onAuthed(token, user);
    } catch (e2) {
      setErr(e2?.response?.data?.message || "Invalid mobile or password");
    } finally { setLoading(false); }
  };

  const switchMode = (m) => {
    setMode(m);
    setStep("mobile");
    setErr("");
    setPassword("");
    setDevOtp("");
  };

  return (
    <div className="te-auth">
      <BrandPanel />
      <section className="te-auth__form">
        {step === "otp" ? (
          <OtpStep mobile={mobile} devOtp={devOtp} onAuthed={onAuthed} onBack={() => setStep("mobile")} />
        ) : (
          <form className="te-card" onSubmit={mode === "otp" ? requestOtp : passwordLogin} data-testid="login-card">
            <h2>Welcome back 👋</h2>
            <p className="muted">
              {mode === "otp" ? "Sign in with your mobile number. We'll text you a code." : "Sign in with your mobile and password."}
            </p>

            <div className="te-authmode" data-testid="login-mode-toggle">
              <button type="button"
                className={`te-authmode__tab ${mode === "otp" ? "active" : ""}`}
                onClick={() => switchMode("otp")}
                data-testid="login-mode-otp">
                OTP
              </button>
              <button type="button"
                className={`te-authmode__tab ${mode === "password" ? "active" : ""}`}
                onClick={() => switchMode("password")}
                data-testid="login-mode-password">
                Password
              </button>
            </div>

            <label className="te-label" htmlFor="mob">Mobile Number</label>
            <div className="te-input--phone">
              <div className="cc">+91</div>
              <input
                id="mob"
                data-testid="login-mobile-input"
                className="te-input"
                placeholder="10-digit mobile"
                value={mobile}
                onChange={(e) => { setMobile(e.target.value.replace(/\D/g, "").slice(0, 10)); if (err) setErr(""); }}
                inputMode="numeric"
                maxLength={10}
              />
            </div>

            {mode === "password" && (
              <div className="te-field" style={{ marginTop: 16 }}>
                <label className="te-label" htmlFor="pwd">Password</label>
                <input
                  id="pwd"
                  data-testid="login-password-input"
                  className="te-input"
                  type="password"
                  placeholder="Your password"
                  value={password}
                  onChange={(e) => { setPassword(e.target.value); if (err) setErr(""); }}
                />
              </div>
            )}

            {err && <div className="te-error" data-testid="login-error">{err}</div>}
            <div style={{ marginTop: 20 }}>
              <button type="submit" className="te-btn te-btn--primary" disabled={loading} data-testid="login-continue-btn">
                {loading ? <span className="te-spin" /> : mode === "otp" ? "Send OTP" : "Sign in"}
              </button>
            </div>

            {mode === "otp" ? (
              <div className="te-hint" data-testid="login-hint-otp">
                <b>Dev mode:</b> OTP is always <code>123456</code>. Any 10-digit mobile works.
              </div>
            ) : (
              <div className="te-hint" data-testid="login-hint-password" style={{ background: "#eff6ff", borderColor: "#bfdbfe", color: "#1e40af" }}>
                Only works if you set a password during profile setup. New here? Use OTP.
              </div>
            )}
          </form>
        )}
      </section>
    </div>
  );
}

function OtpStep({ mobile, devOtp, onAuthed, onBack }) {
  const [otp, setOtp] = useState(devOtp || "");
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");
  const [timer, setTimer] = useState(30);
  const inputRef = useRef(null);

  useEffect(() => {
    if (timer <= 0) return;
    const id = setInterval(() => setTimer((t) => t - 1), 1000);
    return () => clearInterval(id);
  }, [timer]);

  useEffect(() => { inputRef.current?.focus(); }, []);

  const submit = async (code) => {
    setErr("");
    if (!/^\d{6}$/.test(code)) return setErr("Enter the 6-digit code");
    setLoading(true);
    try {
      const r = await api.post("/auth/otp/verify", { mobile, otp: code });
      const { token, user } = r.data.data;
      toast(user.profileComplete ? `Welcome back, ${user.fullName || "user"}` : "Almost there — complete your profile", "success");
      onAuthed(token, user);
    } catch (e2) {
      setErr(e2?.response?.data?.message || "Invalid OTP");
    } finally { setLoading(false); }
  };

  const resend = async () => {
    try {
      const { data } = await api.post("/auth/login", { mobile });
      setOtp(data.devOtp || "");
      setTimer(30);
      toast("OTP resent", "success");
    } catch { toast("Could not resend", "error"); }
  };

  return (
    <form className="te-card" onSubmit={(e) => { e.preventDefault(); submit(otp); }} data-testid="otp-card">
      <h2>Verify OTP</h2>
      <p className="muted">Sent a 6-digit code to <b>+91 {mobile}</b>. <button type="button" className="te-btn--ghost" style={{border:0,background:"transparent",padding:0,color:"#f97316",fontWeight:700,cursor:"pointer"}} onClick={onBack} data-testid="otp-change-btn">Change</button></p>
      <div className="te-otp-grid" onClick={() => inputRef.current?.focus()}>
        {Array.from({ length: 6 }).map((_, i) => (
          <div key={i} className={`te-otp-cell ${i === otp.length ? "active" : ""}`} data-testid={`otp-cell-${i}`}>{otp[i] || ""}</div>
        ))}
      </div>
      <input
        ref={inputRef}
        className="te-otp-hidden"
        value={otp}
        onChange={(e) => {
          const v = e.target.value.replace(/\D/g, "").slice(0, 6);
          setOtp(v);
          if (err) setErr("");
          if (v.length === 6) setTimeout(() => submit(v), 100);
        }}
        inputMode="numeric"
        maxLength={6}
        data-testid="otp-input"
      />
      {err && <div className="te-error" data-testid="otp-error">{err}</div>}
      <button type="submit" className="te-btn te-btn--primary" disabled={loading} data-testid="otp-verify-btn" style={{ marginTop: 6 }}>
        {loading ? <span className="te-spin" /> : "Verify & continue"}
      </button>
      <div style={{ textAlign: "center", marginTop: 16, fontSize: 13, color: "#64748b" }}>
        {timer > 0 ? (
          <>Resend in <b style={{ color: "#0f2e5c" }}>0:{String(timer).padStart(2, "0")}</b></>
        ) : (
          <button type="button" onClick={resend} style={{ border: 0, background: "transparent", color: "#f97316", fontWeight: 700, cursor: "pointer" }} data-testid="otp-resend-btn">Resend OTP</button>
        )}
      </div>
    </form>
  );
}

function ProfileForm({ user, onAuthed }) {
  const [form, setForm] = useState({
    mobile: user?.mobile || "",
    fullName: "",
    email: "",
    customerType: "Individual",
    dob: "",
    pan: "",
    aadhaar: "",
    address: "",
    password: "",
  });
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState({});

  const upd = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const submit = async (e) => {
    e.preventDefault();
    const errs = {};
    if (!form.fullName.trim()) errs.fullName = "Required";
    if (!/^\S+@\S+\.\S+$/.test(form.email)) errs.email = "Valid email required";
    if (!/^[A-Z]{5}[0-9]{4}[A-Z]$/.test(form.pan.toUpperCase())) errs.pan = "Format: ABCDE1234F";
    if (!/^\d{12}$/.test(form.aadhaar)) errs.aadhaar = "12 digits";
    if (Object.keys(errs).length) { setErr(errs); return; }
    setErr({}); setLoading(true);
    try {
      const payload = { ...form, pan: form.pan.toUpperCase() };
      Object.keys(payload).forEach((k) => {
        if (payload[k] === "" || payload[k] === null || payload[k] === undefined) delete payload[k];
      });
      const r = await api.post("/auth/register", payload);
      const { token, user: u } = r.data.data;
      toast("Profile saved — welcome to TaxEdge", "success");
      onAuthed(token, u);
    } catch (e2) {
      toast(e2?.response?.data?.message || "Could not save profile", "error");
    } finally { setLoading(false); }
  };

  return (
    <div className="te-auth">
      <BrandPanel />
      <section className="te-auth__form">
        <form className="te-card" onSubmit={submit} data-testid="profile-card" style={{ maxWidth: 520 }}>
          <h2>Complete your profile</h2>
          <p className="muted">Just a few details so we can file on your behalf.</p>

          <div className="te-field">
            <label className="te-label">Full name</label>
            <input className="te-input" value={form.fullName} onChange={(e) => upd("fullName", e.target.value)} data-testid="profile-fullname" />
            {err.fullName && <div className="te-error">{err.fullName}</div>}
          </div>
          <div className="te-row">
            <div className="te-field">
              <label className="te-label">Email</label>
              <input className="te-input" type="email" value={form.email} onChange={(e) => upd("email", e.target.value)} data-testid="profile-email" />
              {err.email && <div className="te-error">{err.email}</div>}
            </div>
            <div className="te-field">
              <label className="te-label">Customer type</label>
              <select className="te-select" value={form.customerType} onChange={(e) => upd("customerType", e.target.value)} data-testid="profile-ctype">
                {["Individual","Salaried","Business","Proprietorship","Partnership","LLP","Private Limited","Freelancer / Consultant"].map(t => <option key={t}>{t}</option>)}
              </select>
            </div>
          </div>
          <div className="te-row">
            <div className="te-field">
              <label className="te-label">PAN</label>
              <input className="te-input" value={form.pan} maxLength={10} onChange={(e) => upd("pan", e.target.value.toUpperCase())} data-testid="profile-pan" placeholder="ABCDE1234F" />
              {err.pan && <div className="te-error">{err.pan}</div>}
            </div>
            <div className="te-field">
              <label className="te-label">Aadhaar</label>
              <input className="te-input" value={form.aadhaar} maxLength={12} onChange={(e) => upd("aadhaar", e.target.value.replace(/\D/g, ""))} data-testid="profile-aadhaar" />
              {err.aadhaar && <div className="te-error">{err.aadhaar}</div>}
            </div>
          </div>
          <div className="te-row">
            <div className="te-field">
              <label className="te-label">Date of birth</label>
              <input className="te-input" type="date" value={form.dob} onChange={(e) => upd("dob", e.target.value)} data-testid="profile-dob" />
            </div>
            <div className="te-field">
              <label className="te-label">Password (optional)</label>
              <input className="te-input" type="password" value={form.password} onChange={(e) => upd("password", e.target.value)} data-testid="profile-password" placeholder="min 6 chars" />
            </div>
          </div>
          <div className="te-field">
            <label className="te-label">Address</label>
            <textarea className="te-textarea" value={form.address} onChange={(e) => upd("address", e.target.value)} data-testid="profile-address" />
          </div>
          <button className="te-btn te-btn--primary" disabled={loading} data-testid="profile-submit-btn">
            {loading ? <span className="te-spin" /> : "Save & enter dashboard"}
          </button>
        </form>
      </section>
    </div>
  );
}

/* ---------------- dashboard ---------------- */
const MODULES = [
  { key: "overview", label: "Overview" },
  { key: "gst", label: "GST" },
  { key: "itr", label: "ITR" },
  { key: "tds", label: "TDS" },
  { key: "insurance", label: "Insurance" },
  { key: "loan", label: "Loans" },
  { key: "documents", label: "Documents" },
  { key: "payments", label: "Payments" },
  { key: "notifications", label: "Notifications" },
];

function Dashboard({ user, onLogout, onUserUpdate }) {
  const [tab, setTab] = useState("overview");
  const initials = (user.fullName || user.mobile || "T").split(" ").map(s => s[0]).join("").slice(0, 2).toUpperCase();

  return (
    <div className="te-shell">
      <nav className="te-nav">
        <div className="te-nav__brand">
          <span className="te-brand__logo-dot" style={{ width: 30, height: 30, borderRadius: 8, fontSize: 12 }}>TE</span>
          TAXEDGE
        </div>
        <div className="te-nav__spacer" />
        <div className="te-nav__user">
          <div className="te-nav__avatar" data-testid="nav-avatar">{initials}</div>
          <span style={{ opacity: 0.9 }} data-testid="nav-username">{user.fullName || user.mobile}</span>
          <button className="te-nav__logout" onClick={onLogout} data-testid="logout-btn">Log out</button>
        </div>
      </nav>

      <div className="te-container">
        <div className="te-tabs" role="tablist">
          {MODULES.map((m) => (
            <button key={m.key} className={`te-tab ${tab === m.key ? "active" : ""}`} onClick={() => setTab(m.key)} data-testid={`tab-${m.key}`}>
              {m.label}
            </button>
          ))}
        </div>

        {tab === "overview" && <Overview user={user} onUserUpdate={onUserUpdate} />}
        {tab === "gst" && <ModulePage key="gst" module="gst" title="GST Returns" endpoint="/gst" fields={gstFields} render={renderGst} />}
        {tab === "itr" && <ModulePage key="itr" module="itr" title="Income Tax Returns" endpoint="/itr" fields={itrFields} render={renderItr} />}
        {tab === "tds" && <ModulePage key="tds" module="tds" title="TDS Records" endpoint="/tds" fields={tdsFields} render={renderTds} />}
        {tab === "insurance" && <ModulePage key="insurance" module="insurance" title="Insurance Policies" endpoint="/insurance" fields={insuranceFields} render={renderInsurance} />}
        {tab === "loan" && <ModulePage key="loan" module="loans" title="Loan Applications" endpoint="/loans" fields={loanFields} render={renderLoan} />}
        {tab === "documents" && <Documents />}
        {tab === "payments" && <Payments />}
        {tab === "notifications" && <Notifications />}
      </div>
    </div>
  );
}

/* -------- Overview -------- */
function Overview({ user, onUserUpdate }) {
  const [counts, setCounts] = useState({ gst: 0, itr: 0, tds: 0, insurance: 0, loans: 0, documents: 0, payments: 0, notifications: 0 });
  useEffect(() => {
    (async () => {
      const eps = { gst: "/gst", itr: "/itr", tds: "/tds", insurance: "/insurance", loans: "/loans", documents: "/documents", payments: "/payments", notifications: "/notifications" };
      const out = {};
      await Promise.all(Object.entries(eps).map(async ([k, e]) => {
        try { const r = await api.get(e); out[k] = (unwrap(r) || []).length; } catch { out[k] = 0; }
      }));
      setCounts((c) => ({ ...c, ...out }));
    })();
  }, []);

  return (
    <>
      <div className="te-page-title">
        <div>
          <h1>Hello, {user.fullName?.split(" ")[0] || "there"} 👋</h1>
          <p>Here's a snapshot of everything you have on TaxEdge.</p>
        </div>
      </div>

      <div className="te-kpis" data-testid="overview-kpis">
        {[
          ["GST Returns", counts.gst, "gst"],
          ["ITR Returns", counts.itr, "itr"],
          ["TDS Records", counts.tds, "tds"],
          ["Insurance", counts.insurance, "insurance"],
          ["Loans", counts.loans, "loans"],
          ["Documents", counts.documents, "documents"],
          ["Payments", counts.payments, "payments"],
          ["Notifications", counts.notifications, "notifications"],
        ].map(([label, val, k]) => (
          <div className="te-kpi" key={k} data-testid={`kpi-${k}`}>
            <span>{label}</span>
            <b>{val}</b>
          </div>
        ))}
      </div>

      <div className="te-panel">
        <h3 className="te-panel__title">Profile <span className="badge">{user.profileComplete ? "COMPLETE" : "INCOMPLETE"}</span></h3>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: 14, fontSize: 14 }}>
          <div><b>Mobile</b><br /><span style={{color:"#64748b"}}>+91 {user.mobile}</span></div>
          <div><b>Email</b><br /><span style={{color:"#64748b"}}>{user.email || "—"}</span></div>
          <div><b>PAN</b><br /><span style={{color:"#64748b"}}>{user.pan || "—"}</span></div>
          <div><b>Aadhaar</b><br /><span style={{color:"#64748b"}}>{user.aadhaar ? "•••• •••• " + user.aadhaar.slice(-4) : "—"}</span></div>
          <div><b>Type</b><br /><span style={{color:"#64748b"}}>{user.customerType || "—"}</span></div>
          <div><b>DOB</b><br /><span style={{color:"#64748b"}}>{user.dob || "—"}</span></div>
          <div style={{gridColumn:"1 / -1"}}><b>Address</b><br /><span style={{color:"#64748b"}}>{user.address || "—"}</span></div>
        </div>
      </div>
    </>
  );
}

/* -------- generic module CRUD page -------- */
function ModulePage({ module, title, endpoint, fields, render }) {
  const empty = useMemo(() => fields.reduce((a, f) => ({ ...a, [f.key]: f.default ?? "" }), {}), [fields]);
  const [form, setForm] = useState(empty);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const r = await api.get(endpoint);
      setItems(unwrap(r) || []);
    } catch (e) { toast(e?.response?.data?.message || "Failed to load", "error"); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); /* eslint-disable-next-line */ }, [endpoint]);

  const submit = async (e) => {
    e.preventDefault();
    const body = {};
    for (const f of fields) {
      let v = form[f.key];
      if (v === "" || v == null) { if (f.required) return toast(`${f.label} is required`, "error"); continue; }
      if (f.type === "number") v = Number(v);
      body[f.key] = v;
    }
    setSubmitting(true);
    try {
      await api.post(endpoint, body);
      toast(`${title.replace(/s$/, "")} created`, "success");
      setForm(empty);
      load();
    } catch (e2) {
      toast(e2?.response?.data?.message || "Create failed", "error");
    } finally { setSubmitting(false); }
  };

  const remove = async (id) => {
    if (!window.confirm("Delete this record?")) return;
    try { await api.delete(`${endpoint}/${id}`); toast("Deleted", "success"); load(); }
    catch (e) { toast(e?.response?.data?.message || "Delete failed", "error"); }
  };

  return (
    <>
      <div className="te-page-title">
        <div>
          <h1>{title}</h1>
          <p>{items.length} record{items.length === 1 ? "" : "s"} in your account.</p>
        </div>
      </div>
      <div className="te-grid">
        <div className="te-panel">
          <h3 className="te-panel__title">Add new <span className="badge">POST {endpoint}</span></h3>
          <form onSubmit={submit} data-testid={`${module}-form`}>
            {fields.map((f) => (
              <div className="te-field" key={f.key}>
                <label className="te-label">{f.label}{f.required && " *"}</label>
                {f.options ? (
                  <select className="te-select" value={form[f.key] || ""} onChange={(e) => setForm((s) => ({ ...s, [f.key]: e.target.value }))} data-testid={`${module}-field-${f.key}`}>
                    <option value="">Select…</option>
                    {f.options.map((o) => <option key={o} value={o}>{o}</option>)}
                  </select>
                ) : f.type === "textarea" ? (
                  <textarea className="te-textarea" value={form[f.key] || ""} onChange={(e) => setForm((s) => ({ ...s, [f.key]: e.target.value }))} data-testid={`${module}-field-${f.key}`} />
                ) : (
                  <input
                    className="te-input"
                    type={f.type || "text"}
                    value={form[f.key] ?? ""}
                    placeholder={f.placeholder || ""}
                    onChange={(e) => setForm((s) => ({ ...s, [f.key]: e.target.value }))}
                    data-testid={`${module}-field-${f.key}`}
                  />
                )}
              </div>
            ))}
            <button className="te-btn te-btn--primary" disabled={submitting} data-testid={`${module}-submit-btn`}>
              {submitting ? <span className="te-spin" /> : "Create"}
            </button>
          </form>
        </div>
        <div className="te-panel">
          <h3 className="te-panel__title">Your records <span className="badge">GET {endpoint}</span></h3>
          {loading ? (
            <div className="te-list__empty">Loading…</div>
          ) : items.length === 0 ? (
            <div className="te-list__empty" data-testid={`${module}-empty`}>No records yet. Create your first one on the left.</div>
          ) : (
            <div className="te-list" data-testid={`${module}-list`}>
              {items.map((it) => (
                <div className="te-item" key={it.id} data-testid={`${module}-item-${it.id}`}>
                  <div style={{ flex: 1 }}>{render(it)}</div>
                  <button className="te-btn te-btn--sm te-btn--danger" onClick={() => remove(it.id)} data-testid={`${module}-delete-${it.id}`}>Delete</button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </>
  );
}

/* -------- field defs & renderers -------- */
const statusPill = (s) => <span className={`te-status te-status--${(s || "").toLowerCase()}`}>{s || "—"}</span>;

const gstFields = [
  { key: "gstin", label: "GSTIN", required: true, placeholder: "29ABCDE1234F1Z5" },
  { key: "periodMonth", label: "Month (1-12)", type: "number", required: true, placeholder: "8" },
  { key: "periodYear", label: "Year", type: "number", required: true, placeholder: "2025" },
  { key: "returnType", label: "Return type", options: ["GSTR1", "GSTR3B", "GSTR9"], required: true },
  { key: "totalTaxableValue", label: "Taxable value (₹)", type: "number" },
  { key: "totalTax", label: "Total tax (₹)", type: "number" },
  { key: "status", label: "Status", options: ["DRAFT", "FILED", "PENDING"] },
];
const renderGst = (g) => (
  <>
    <div className="te-item__title">{g.gstin} · {g.returnType}</div>
    <div className="te-item__meta">
      Period <strong>{String(g.periodMonth).padStart(2,"0")}/{g.periodYear}</strong> · Taxable ₹{g.totalTaxableValue || 0} · Tax ₹{g.totalTax || 0} &nbsp; {statusPill(g.status)}
    </div>
  </>
);

const itrFields = [
  { key: "pan", label: "PAN", required: true },
  { key: "assessmentYear", label: "Assessment year", required: true, placeholder: "2024-25" },
  { key: "itrForm", label: "ITR Form", options: ["ITR1", "ITR2", "ITR3", "ITR4"], required: true },
  { key: "totalIncome", label: "Total income (₹)", type: "number" },
  { key: "taxLiability", label: "Tax liability (₹)", type: "number" },
  { key: "refundAmount", label: "Refund (₹)", type: "number" },
  { key: "status", label: "Status", options: ["DRAFT", "FILED", "VERIFIED"] },
];
const renderItr = (it) => (
  <>
    <div className="te-item__title">{it.itrForm} · AY {it.assessmentYear}</div>
    <div className="te-item__meta">PAN <strong>{it.pan}</strong> · Income ₹{it.totalIncome || 0} · Tax ₹{it.taxLiability || 0} · Refund ₹{it.refundAmount || 0} &nbsp; {statusPill(it.status)}</div>
  </>
);

const tdsFields = [
  { key: "tan", label: "TAN", required: true },
  { key: "deductorName", label: "Deductor name", required: true },
  { key: "financialYear", label: "Financial year", required: true, placeholder: "2024-25" },
  { key: "quarter", label: "Quarter", options: ["Q1", "Q2", "Q3", "Q4"], required: true },
  { key: "tdsAmount", label: "TDS amount (₹)", type: "number", required: true },
  { key: "section", label: "Section", placeholder: "194C" },
  { key: "status", label: "Status", options: ["PENDING", "FILED", "VERIFIED"] },
];
const renderTds = (t) => (
  <>
    <div className="te-item__title">{t.deductorName} · {t.quarter} {t.financialYear}</div>
    <div className="te-item__meta">TAN <strong>{t.tan}</strong> · Section {t.section || "—"} · ₹{t.tdsAmount} &nbsp; {statusPill(t.status)}</div>
  </>
);

const insuranceFields = [
  { key: "provider", label: "Provider", required: true },
  { key: "policyType", label: "Policy type", options: ["LIFE", "HEALTH", "VEHICLE", "TERM"], required: true },
  { key: "policyNumber", label: "Policy number", required: true },
  { key: "sumAssured", label: "Sum assured (₹)", type: "number" },
  { key: "premium", label: "Premium (₹)", type: "number" },
  { key: "startDate", label: "Start date", type: "date" },
  { key: "endDate", label: "End date", type: "date" },
];
const renderInsurance = (p) => (
  <>
    <div className="te-item__title">{p.provider} · {p.policyType}</div>
    <div className="te-item__meta">#<strong>{p.policyNumber}</strong> · Sum ₹{p.sumAssured || 0} · Premium ₹{p.premium || 0} &nbsp; {statusPill(p.status)}</div>
  </>
);

const loanFields = [
  { key: "loanType", label: "Loan type", options: ["HOME", "PERSONAL", "AUTO", "EDUCATION", "BUSINESS"], required: true },
  { key: "amount", label: "Amount (₹)", type: "number", required: true },
  { key: "tenureMonths", label: "Tenure (months)", type: "number", required: true },
  { key: "interestRate", label: "Interest rate (% p.a.)", type: "number" },
  { key: "purpose", label: "Purpose" },
];
const renderLoan = (l) => (
  <>
    <div className="te-item__title">{l.loanType} · ₹{Number(l.amount).toLocaleString("en-IN")}</div>
    <div className="te-item__meta">
      {l.tenureMonths} months @ {l.interestRate || 0}% · EMI <strong>₹{l.emi ? Number(l.emi).toLocaleString("en-IN") : "—"}</strong> &nbsp; {statusPill(l.status)}
    </div>
  </>
);

/* -------- Documents (multipart upload) -------- */
function Documents() {
  const [items, setItems] = useState([]);
  const [file, setFile] = useState(null);
  const [category, setCategory] = useState("OTHER");
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);

  const load = async () => {
    setLoading(true);
    try { const r = await api.get("/documents"); setItems(unwrap(r) || []); }
    catch { toast("Failed to load documents", "error"); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); }, []);

  const upload = async (e) => {
    e.preventDefault();
    if (!file) return toast("Pick a file first", "error");
    const fd = new FormData();
    fd.append("file", file);
    fd.append("category", category);
    setUploading(true);
    try {
      await api.post("/documents", fd, { headers: { "Content-Type": "multipart/form-data" } });
      toast("Uploaded", "success");
      setFile(null);
      document.getElementById("doc-file-input").value = "";
      load();
    } catch (e2) { toast(e2?.response?.data?.message || "Upload failed", "error"); }
    finally { setUploading(false); }
  };

  const remove = async (id) => {
    if (!window.confirm("Delete this document?")) return;
    await api.delete(`/documents/${id}`);
    toast("Deleted", "success"); load();
  };

  const download = async (id, name) => {
    try {
      const r = await api.get(`/documents/${id}/download`, { responseType: "blob" });
      const url = URL.createObjectURL(new Blob([r.data]));
      const a = document.createElement("a");
      a.href = url; a.download = name || `document-${id}`;
      document.body.appendChild(a); a.click(); a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch { toast("Download failed", "error"); }
  };

  return (
    <>
      <div className="te-page-title">
        <div><h1>Documents</h1><p>Encrypted at rest, tied to your account.</p></div>
      </div>
      <div className="te-grid">
        <div className="te-panel">
          <h3 className="te-panel__title">Upload <span className="badge">POST /documents</span></h3>
          <form onSubmit={upload} data-testid="documents-form">
            <div className="te-field">
              <label className="te-label">File</label>
              <input id="doc-file-input" type="file" className="te-input" onChange={(e) => setFile(e.target.files?.[0] || null)} data-testid="documents-field-file" />
            </div>
            <div className="te-field">
              <label className="te-label">Category</label>
              <select className="te-select" value={category} onChange={(e) => setCategory(e.target.value)} data-testid="documents-field-category">
                {["GST", "ITR", "INSURANCE", "TDS", "LOAN", "KYC", "OTHER"].map(o => <option key={o}>{o}</option>)}
              </select>
            </div>
            <button className="te-btn te-btn--primary" disabled={uploading} data-testid="documents-submit-btn">
              {uploading ? <span className="te-spin" /> : "Upload"}
            </button>
          </form>
        </div>
        <div className="te-panel">
          <h3 className="te-panel__title">Files <span className="badge">GET /documents</span></h3>
          {loading ? (
            <div className="te-list__empty">Loading…</div>
          ) : items.length === 0 ? (
            <div className="te-list__empty" data-testid="documents-empty">Nothing uploaded yet.</div>
          ) : (
            <div className="te-list" data-testid="documents-list">
              {items.map((d) => (
                <div className="te-item" key={d.id} data-testid={`documents-item-${d.id}`}>
                  <div style={{ flex: 1 }}>
                    <div className="te-item__title">{d.originalName}</div>
                    <div className="te-item__meta">{d.category} · {(d.size / 1024).toFixed(1)} KB · {d.contentType}</div>
                  </div>
                  <div style={{ display: "flex", gap: 6 }}>
                    <button className="te-btn te-btn--sm te-btn--outline" onClick={() => download(d.id, d.originalName)} data-testid={`documents-download-${d.id}`}>Download</button>
                    <button className="te-btn te-btn--sm te-btn--danger" onClick={() => remove(d.id)} data-testid={`documents-delete-${d.id}`}>Delete</button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </>
  );
}

/* -------- Payments (stub) -------- */
function Payments() {
  const [items, setItems] = useState([]);
  const [form, setForm] = useState({ amount: "", purpose: "", currency: "INR" });
  const [loading, setLoading] = useState(false);
  const load = async () => { setLoading(true); try { setItems(unwrap(await api.get("/payments")) || []); } finally { setLoading(false); } };
  useEffect(() => { load(); }, []);

  const initiate = async (e) => {
    e.preventDefault();
    if (!form.amount || !form.purpose) return toast("Fill amount & purpose", "error");
    await api.post("/payments/initiate", { ...form, amount: Number(form.amount) });
    toast("Payment initiated (stub)", "success");
    setForm({ amount: "", purpose: "", currency: "INR" });
    load();
  };

  const mark = async (txn, status) => {
    await api.post("/payments/callback", { transactionId: txn, status });
    toast(`Marked ${status}`, "success");
    load();
  };

  return (
    <>
      <div className="te-page-title"><div><h1>Payments</h1><p>STUB gateway — callbacks are simulated below.</p></div></div>
      <div className="te-grid">
        <div className="te-panel">
          <h3 className="te-panel__title">Initiate payment <span className="badge">POST /payments/initiate</span></h3>
          <form onSubmit={initiate} data-testid="payments-form">
            <div className="te-field"><label className="te-label">Amount (₹) *</label><input className="te-input" type="number" value={form.amount} onChange={(e) => setForm(s => ({...s, amount: e.target.value}))} data-testid="payments-field-amount" /></div>
            <div className="te-field"><label className="te-label">Purpose *</label><input className="te-input" value={form.purpose} onChange={(e) => setForm(s => ({...s, purpose: e.target.value}))} placeholder="GST filing fee" data-testid="payments-field-purpose" /></div>
            <button className="te-btn te-btn--primary" data-testid="payments-submit-btn">Initiate</button>
          </form>
        </div>
        <div className="te-panel">
          <h3 className="te-panel__title">Transactions <span className="badge">GET /payments</span></h3>
          {loading ? <div className="te-list__empty">Loading…</div> : items.length === 0 ? (
            <div className="te-list__empty" data-testid="payments-empty">No transactions yet.</div>
          ) : (
            <div className="te-list" data-testid="payments-list">
              {items.map((p) => (
                <div className="te-item" key={p.id} data-testid={`payments-item-${p.id}`}>
                  <div style={{ flex: 1 }}>
                    <div className="te-item__title">₹{Number(p.amount).toLocaleString("en-IN")} · {p.purpose}</div>
                    <div className="te-item__meta">Txn <strong>{p.transactionId}</strong> · {statusPill(p.status)}</div>
                  </div>
                  {p.status === "PENDING" && (
                    <div style={{ display: "flex", gap: 6 }}>
                      <button className="te-btn te-btn--sm te-btn--outline" onClick={() => mark(p.transactionId, "SUCCESS")} data-testid={`payments-success-${p.id}`}>Mark success</button>
                      <button className="te-btn te-btn--sm te-btn--danger" onClick={() => mark(p.transactionId, "FAILED")} data-testid={`payments-fail-${p.id}`}>Fail</button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </>
  );
}

/* -------- Notifications -------- */
function Notifications() {
  const [items, setItems] = useState([]);
  const [unread, setUnread] = useState(0);
  const [loading, setLoading] = useState(false);
  const load = async () => {
    setLoading(true);
    try {
      setItems(unwrap(await api.get("/notifications")) || []);
      const c = unwrap(await api.get("/notifications/unread-count"));
      setUnread(c?.count ?? 0);
    } finally { setLoading(false); }
  };
  useEffect(() => { load(); }, []);

  const markRead = async (id) => {
    await api.patch(`/notifications/${id}/read`, {});
    load();
  };

  return (
    <>
      <div className="te-page-title"><div><h1>Notifications</h1><p><b>{unread}</b> unread of {items.length} total.</p></div></div>
      <div className="te-panel">
        {loading ? <div className="te-list__empty">Loading…</div> : items.length === 0 ? (
          <div className="te-list__empty" data-testid="notifications-empty">You're all caught up 🎉<br /><span style={{fontSize:12}}>(New notifications are sent by CA/ADMIN roles from the backend.)</span></div>
        ) : (
          <div className="te-list" data-testid="notifications-list">
            {items.map((n) => (
              <div className="te-item" key={n.id} style={{ opacity: n.readFlag ? 0.6 : 1 }} data-testid={`notifications-item-${n.id}`}>
                <div style={{ flex: 1 }}>
                  <div className="te-item__title">{n.title} {statusPill(n.type)}</div>
                  <div className="te-item__meta">{n.message}</div>
                </div>
                {!n.readFlag && (
                  <button className="te-btn te-btn--sm te-btn--outline" onClick={() => markRead(n.id)} data-testid={`notifications-read-${n.id}`}>Mark read</button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </>
  );
}
