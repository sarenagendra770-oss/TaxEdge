"""
TaxEdge Spring Boot Backend - Regression Test Suite
Tests all modules: auth/OTP, user, gst, itr, insurance, loan, tds, document, payment, notification
"""
import os
import io
import random
import subprocess
import pytest
import requests

BASE_URL = os.environ.get("REACT_APP_BACKEND_URL", "http://localhost:8001").rstrip("/")
API = f"{BASE_URL}/api"
OTP = "123456"


def _rand_mobile():
    # random 10-digit starting with 9
    return "9" + "".join(str(random.randint(0, 9)) for _ in range(9))


def _promote_to_admin(mobile):
    """Bump role to ADMIN via mysql for role tests."""
    try:
        subprocess.run(
            ["mysql", "-uroot", "taxedge", "-e", f"UPDATE users SET role='ADMIN' WHERE mobile='{mobile}'"],
            check=True, capture_output=True, timeout=10,
        )
        return True
    except Exception as e:
        print(f"Failed to promote {mobile}: {e}")
        return False


def _register_user(role=None, prefix="TEST"):
    """Login + verify + register. Returns (token, user, mobile)."""
    mobile = _rand_mobile()
    email = f"test_{mobile}@example.com"

    # login (send OTP)
    r = requests.post(f"{API}/auth/login", json={"mobile": mobile}, timeout=15)
    assert r.status_code == 200, r.text

    # verify OTP
    r = requests.post(f"{API}/auth/otp/verify", json={"mobile": mobile, "otp": OTP}, timeout=15)
    assert r.status_code == 200, r.text
    body = r.json()
    token = body["data"]["token"]

    # register
    payload = {
        "mobile": mobile,
        "fullName": f"{prefix} User",
        "email": email,
        "customerType": "INDIVIDUAL",
        "dob": "1990-01-01",
        "pan": f"ABCDE{random.randint(1000,9999)}F",
        "aadhaar": str(random.randint(100000000000, 999999999999)),
        "address": "Test address",
        "avatarUrl": "",
    }
    r = requests.post(
        f"{API}/auth/register",
        json=payload,
        headers={"Authorization": f"Bearer {token}"},
        timeout=15,
    )
    assert r.status_code == 200, r.text
    body = r.json()
    token = body["data"]["token"]
    user = body["data"]["user"]

    if role == "ADMIN":
        _promote_to_admin(mobile)
        # re-login to refresh JWT with new role
        requests.post(f"{API}/auth/login", json={"mobile": mobile}, timeout=15)
        r = requests.post(f"{API}/auth/otp/verify", json={"mobile": mobile, "otp": OTP}, timeout=15)
        token = r.json()["data"]["token"]

    return token, user, mobile, email


@pytest.fixture(scope="module")
def user_ctx():
    token, user, mobile, email = _register_user()
    return {"token": token, "user": user, "mobile": mobile, "email": email,
            "H": {"Authorization": f"Bearer {token}"}}


@pytest.fixture(scope="module")
def user2_ctx():
    token, user, mobile, email = _register_user(prefix="OTHER")
    return {"token": token, "user": user, "mobile": mobile, "email": email,
            "H": {"Authorization": f"Bearer {token}"}}


@pytest.fixture(scope="module")
def admin_ctx():
    token, user, mobile, email = _register_user(role="ADMIN", prefix="ADMIN")
    return {"token": token, "user": user, "mobile": mobile, "email": email,
            "H": {"Authorization": f"Bearer {token}"}}


# =========================================================================
# Health
# =========================================================================
def test_health():
    r = requests.get(f"{API}/health", timeout=10)
    assert r.status_code == 200
    assert r.json().get("status") == "UP"


# =========================================================================
# Auth - login / OTP / register
# =========================================================================
class TestAuth:
    def test_login_new_mobile(self):
        mobile = _rand_mobile()
        r = requests.post(f"{API}/auth/login", json={"mobile": mobile}, timeout=15)
        assert r.status_code == 200, r.text
        data = r.json()
        assert data.get("success") is True
        assert data.get("registered") is False
        assert data.get("devOtp") == "123456"

    def test_login_invalid_mobile_short(self):
        r = requests.post(f"{API}/auth/login", json={"mobile": "12345"}, timeout=15)
        assert r.status_code == 400, r.text

    def test_login_invalid_mobile_nonnumeric(self):
        r = requests.post(f"{API}/auth/login", json={"mobile": "abcdefghij"}, timeout=15)
        assert r.status_code == 400, r.text

    def test_otp_verify_wrong(self):
        mobile = _rand_mobile()
        requests.post(f"{API}/auth/login", json={"mobile": mobile}, timeout=15)
        r = requests.post(f"{API}/auth/otp/verify", json={"mobile": mobile, "otp": "000000"}, timeout=15)
        assert r.status_code == 401, r.text

    def test_otp_verify_correct_returns_token(self):
        mobile = _rand_mobile()
        requests.post(f"{API}/auth/login", json={"mobile": mobile}, timeout=15)
        r = requests.post(f"{API}/auth/otp/verify", json={"mobile": mobile, "otp": OTP}, timeout=15)
        assert r.status_code == 200, r.text
        d = r.json()["data"]
        assert "token" in d and len(d["token"]) > 20
        assert "user" in d
        assert "registered" in d

    def test_register_completes_and_relogin_registered_true(self):
        token, user, mobile, email = _register_user()
        assert user.get("profileComplete") is True
        assert user.get("email") == email

        # duplicate email attempt via another new user
        m2 = _rand_mobile()
        requests.post(f"{API}/auth/login", json={"mobile": m2})
        r = requests.post(f"{API}/auth/otp/verify", json={"mobile": m2, "otp": OTP})
        t2 = r.json()["data"]["token"]
        dup_payload = {
            "mobile": m2,
            "fullName": "Dup", "email": email, "customerType": "INDIVIDUAL",
            "dob": "1990-01-01", "pan": "ZZZZZ9999Z", "aadhaar": "111122223333",
            "address": "x", "avatarUrl": "",
        }
        r = requests.post(f"{API}/auth/register", json=dup_payload,
                          headers={"Authorization": f"Bearer {t2}"}, timeout=15)
        assert r.status_code in (400, 409), f"expected conflict, got {r.status_code}: {r.text}"

        # re-login of first user → registered=true
        r = requests.post(f"{API}/auth/login", json={"mobile": mobile}, timeout=15)
        assert r.status_code == 200
        assert r.json().get("registered") is True


# =========================================================================
# Users
# =========================================================================
class TestUsers:
    def test_me_with_token(self, user_ctx):
        r = requests.get(f"{API}/users/me", headers=user_ctx["H"], timeout=15)
        assert r.status_code == 200, r.text
        d = r.json()["data"]
        assert d["mobile"] == user_ctx["mobile"]
        assert d["email"] == user_ctx["email"]

    def test_me_without_token(self):
        r = requests.get(f"{API}/users/me", timeout=15)
        assert r.status_code == 401, r.text

    def test_list_users_forbidden_for_user(self, user_ctx):
        r = requests.get(f"{API}/users", headers=user_ctx["H"], timeout=15)
        assert r.status_code == 403, r.text

    def test_list_users_admin_ok(self, admin_ctx):
        r = requests.get(f"{API}/users", headers=admin_ctx["H"], timeout=15)
        assert r.status_code == 200, r.text


# =========================================================================
# Generic CRUD helper for user-scoped modules
# =========================================================================
def _crud_test(path, payload, update_payload, user_ctx, user2_ctx):
    H1 = user_ctx["H"]
    H2 = user2_ctx["H"]

    # CREATE
    r = requests.post(f"{API}{path}", json=payload, headers=H1, timeout=15)
    assert r.status_code in (200, 201), f"CREATE {path}: {r.status_code} {r.text}"
    created = r.json()["data"]
    rid = created.get("id")
    assert rid is not None

    # LIST (mine)
    r = requests.get(f"{API}{path}", headers=H1, timeout=15)
    assert r.status_code == 200, r.text
    ids = [x["id"] for x in r.json()["data"]]
    assert rid in ids

    # user2 list should NOT contain it
    r = requests.get(f"{API}{path}", headers=H2, timeout=15)
    assert r.status_code == 200
    ids2 = [x["id"] for x in r.json()["data"]]
    assert rid not in ids2

    # user2 cannot access
    r = requests.get(f"{API}{path}/{rid}", headers=H2, timeout=15)
    assert r.status_code in (403, 404), f"expected forbidden for other user, got {r.status_code}"

    # UPDATE
    r = requests.put(f"{API}{path}/{rid}", json=update_payload, headers=H1, timeout=15)
    assert r.status_code == 200, r.text

    # DELETE
    r = requests.delete(f"{API}{path}/{rid}", headers=H1, timeout=15)
    assert r.status_code in (200, 204), r.text
    return rid


class TestGST:
    def test_gst_crud_and_isolation(self, user_ctx, user2_ctx):
        _crud_test("/gst",
                   {"gstin": "27ABCDE1234F1Z5", "periodMonth": 3, "periodYear": 2024, "returnType": "GSTR1", "status": "PENDING"},
                   {"gstin": "27ABCDE1234F1Z5", "periodMonth": 3, "periodYear": 2024, "returnType": "GSTR1", "status": "FILED"},
                   user_ctx, user2_ctx)


class TestITR:
    def test_itr_crud_and_isolation(self, user_ctx, user2_ctx):
        _crud_test("/itr",
                   {"pan": "ABCDE1234F", "assessmentYear": "2024-25", "itrForm": "ITR1", "status": "DRAFT", "totalIncome": 500000},
                   {"pan": "ABCDE1234F", "assessmentYear": "2024-25", "itrForm": "ITR1", "status": "FILED", "totalIncome": 550000},
                   user_ctx, user2_ctx)


class TestInsurance:
    def test_insurance_crud(self, user_ctx, user2_ctx):
        _crud_test("/insurance",
                   {"policyType": "HEALTH", "policyNumber": "POL123", "provider": "ACME", "premium": 1000, "sumAssured": 500000},
                   {"policyType": "HEALTH", "policyNumber": "POL123", "provider": "ACME", "premium": 1500, "sumAssured": 500000},
                   user_ctx, user2_ctx)


class TestTDS:
    def test_tds_crud(self, user_ctx, user2_ctx):
        _crud_test("/tds",
                   {"tan": "TAN12345", "deductorName": "Acme Corp", "quarter": "Q1", "financialYear": "2024-25", "tdsAmount": 10000, "status": "PENDING"},
                   {"tan": "TAN12345", "deductorName": "Acme Corp", "quarter": "Q1", "financialYear": "2024-25", "tdsAmount": 12000, "status": "FILED"},
                   user_ctx, user2_ctx)


# =========================================================================
# Loan - EMI + role-based status update
# =========================================================================
class TestLoan:
    def test_loan_flow(self, user_ctx, admin_ctx):
        payload = {"loanType": "HOME", "amount": 1000000, "interestRate": 8.5,
                   "tenureMonths": 120, "status": "PENDING"}
        r = requests.post(f"{API}/loans", json=payload, headers=user_ctx["H"], timeout=15)
        assert r.status_code in (200, 201), r.text
        d = r.json()["data"]
        emi = d.get("emi") or d.get("monthlyEmi")
        assert emi is not None and float(emi) > 0, f"emi missing/invalid in {d}"
        lid = d["id"]

        # user should be forbidden from PATCH status
        r = requests.patch(f"{API}/loans/{lid}/status", json={"status": "APPROVED"},
                           headers=user_ctx["H"], timeout=15)
        assert r.status_code == 403, r.text

        # admin should succeed
        r = requests.patch(f"{API}/loans/{lid}/status", json={"status": "APPROVED"},
                           headers=admin_ctx["H"], timeout=15)
        assert r.status_code == 200, r.text


# =========================================================================
# Documents (multipart)
# =========================================================================
class TestDocuments:
    def test_upload_download_delete(self, user_ctx):
        content = b"hello taxedge doc content"
        files = {"file": ("test.txt", io.BytesIO(content), "text/plain")}
        data = {"documentType": "PAN", "description": "test doc"}
        r = requests.post(f"{API}/documents", files=files, data=data,
                          headers={"Authorization": user_ctx["H"]["Authorization"]}, timeout=30)
        assert r.status_code in (200, 201), r.text
        d = r.json()["data"]
        did = d["id"]
        assert d.get("fileName") or d.get("originalFileName") or d.get("originalName")

        # download
        r = requests.get(f"{API}/documents/{did}/download", headers=user_ctx["H"], timeout=30)
        assert r.status_code == 200, r.text
        assert r.content == content

        # delete
        r = requests.delete(f"{API}/documents/{did}", headers=user_ctx["H"], timeout=15)
        assert r.status_code in (200, 204), r.text


# =========================================================================
# Payments (stub)
# =========================================================================
class TestPayments:
    def test_initiate_and_callback(self, user_ctx):
        r = requests.post(f"{API}/payments/initiate",
                          json={"amount": 500, "purpose": "GST_FILING"},
                          headers=user_ctx["H"], timeout=15)
        assert r.status_code in (200, 201), r.text
        d = r.json()["data"]
        txn = d.get("transactionId")
        assert txn
        assert d.get("status") == "PENDING"

        # callback
        r = requests.post(f"{API}/payments/callback",
                          json={"transactionId": txn, "status": "SUCCESS"},
                          headers=user_ctx["H"], timeout=15)
        assert r.status_code == 200, r.text
        d2 = r.json().get("data") or {}
        assert (d2.get("status") == "SUCCESS") or (r.json().get("success") is True)


# =========================================================================
# Notifications
# =========================================================================
class TestNotifications:
    def test_create_user_forbidden(self, user_ctx):
        r = requests.post(f"{API}/notifications",
                          json={"userId": user_ctx["user"]["id"], "title": "hi", "message": "m", "type": "INFO"},
                          headers=user_ctx["H"], timeout=15)
        assert r.status_code == 403, r.text

    def test_admin_create_and_user_list_and_read(self, user_ctx, admin_ctx):
        r = requests.post(f"{API}/notifications",
                          json={"userId": user_ctx["user"]["id"], "title": "hello", "message": "msg", "type": "INFO"},
                          headers=admin_ctx["H"], timeout=15)
        assert r.status_code in (200, 201), r.text
        notif = r.json()["data"]
        nid = notif["id"]

        # list mine
        r = requests.get(f"{API}/notifications", headers=user_ctx["H"], timeout=15)
        assert r.status_code == 200
        ids = [x["id"] for x in r.json()["data"]]
        assert nid in ids

        # unread count > 0
        r = requests.get(f"{API}/notifications/unread-count", headers=user_ctx["H"], timeout=15)
        assert r.status_code == 200
        cnt = r.json().get("data")
        if isinstance(cnt, dict):
            cnt = cnt.get("count", cnt.get("unreadCount", 0))
        assert cnt is None or cnt >= 1

        # mark read
        r = requests.patch(f"{API}/notifications/{nid}/read", headers=user_ctx["H"], timeout=15)
        assert r.status_code == 200, r.text


# =========================================================================
# CORS preflight
# =========================================================================
def test_cors_preflight():
    r = requests.options(
        f"{API}/auth/login",
        headers={
            "Origin": "http://example.com",
            "Access-Control-Request-Method": "POST",
            "Access-Control-Request-Headers": "Content-Type",
        },
        timeout=10,
    )
    assert r.status_code in (200, 204), r.text
    # permissive CORS
    aco = r.headers.get("Access-Control-Allow-Origin")
    acm = r.headers.get("Access-Control-Allow-Methods", "")
    assert aco is not None
    assert "POST" in acm or "*" in acm
