"""Backend tests for POST /api/auth/password-login and OTP regression."""
import os
import requests
import pytest

BASE_URL = os.environ.get("REACT_APP_BACKEND_URL", "https://compliance-hub-1046.preview.emergentagent.com").rstrip("/")
API = f"{BASE_URL}/api"


@pytest.fixture(scope="module")
def session():
    s = requests.Session()
    s.headers.update({"Content-Type": "application/json"})
    return s


# --- password-login endpoint tests ---
class TestPasswordLogin:
    def test_correct_credentials(self, session):
        r = session.post(f"{API}/auth/password-login",
                         json={"mobile": "9911223344", "password": "secret123"})
        assert r.status_code == 200
        body = r.json()
        assert body["success"] is True
        assert body["data"]["token"]
        assert body["data"]["user"]["mobile"] == "9911223344"
        assert body["data"]["user"]["fullName"] == "Pwd Tester"

    def test_wrong_password(self, session):
        r = session.post(f"{API}/auth/password-login",
                         json={"mobile": "9911223344", "password": "wrongpass"})
        assert r.status_code == 401
        body = r.json()
        assert body["success"] is False
        assert "Invalid" in body["message"]

    def test_no_password_set(self, session):
        # Create a fresh OTP-only user (no register → no password)
        mobile = "9000000123"
        session.post(f"{API}/auth/login", json={"mobile": mobile})
        r = session.post(f"{API}/auth/password-login",
                         json={"mobile": mobile, "password": "anything"})
        assert r.status_code == 401
        assert "No password set" in r.json()["message"]

    def test_invalid_mobile_format(self, session):
        r = session.post(f"{API}/auth/password-login",
                         json={"mobile": "12345", "password": "anything"})
        assert r.status_code == 400
        assert r.json()["success"] is False

    def test_token_authorizes_users_me(self, session):
        r = session.post(f"{API}/auth/password-login",
                         json={"mobile": "9911223344", "password": "secret123"})
        token = r.json()["data"]["token"]
        me = session.get(f"{API}/users/me", headers={"Authorization": f"Bearer {token}"})
        assert me.status_code == 200
        assert me.json()["data"]["mobile"] == "9911223344"


# --- OTP flow regression ---
class TestOtpRegression:
    def test_otp_login_flow(self, session):
        m = "9876543210"
        r1 = session.post(f"{API}/auth/login", json={"mobile": m})
        assert r1.status_code == 200
        r2 = session.post(f"{API}/auth/otp/verify", json={"mobile": m, "otp": "123456"})
        assert r2.status_code == 200
        token = r2.json()["data"]["token"]
        me = session.get(f"{API}/users/me", headers={"Authorization": f"Bearer {token}"})
        assert me.status_code == 200
        assert me.json()["data"]["mobile"] == m
