import axios from "axios";

const BASE = (process.env.REACT_APP_BACKEND_URL || "") + "/api";

export const api = axios.create({ baseURL: BASE });

const TOKEN_KEY = "taxedge_token";
const USER_KEY = "taxedge_user";

export const auth = {
  getToken: () => localStorage.getItem(TOKEN_KEY),
  getUser: () => {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  },
  set: (token, user) => {
    localStorage.setItem(TOKEN_KEY, token);
    if (user) localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  clear: () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },
};

api.interceptors.request.use((cfg) => {
  const t = auth.getToken();
  if (t) cfg.headers.Authorization = `Bearer ${t}`;
  return cfg;
});

api.interceptors.response.use(
  (r) => r,
  (err) => {
    if (err?.response?.status === 401) {
      auth.clear();
      if (window.location.pathname !== "/") window.location.href = "/";
    }
    return Promise.reject(err);
  }
);

// unwrap standard ApiResponse envelope
export const unwrap = (r) => (r?.data?.data !== undefined ? r.data.data : r.data);
