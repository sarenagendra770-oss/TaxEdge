"""Tests for PAN/Aadhaar uniqueness enforcement on /auth/register."""
import os
import time
import random
import pytest
import requests

BASE_URL = os.environ.get("REACT_APP_BACKEND_URL", "https://compliance-hub-1046.preview.emergentagent.com").rstrip("/")
API = f"{BASE_URL}/api"
OTP = "123456"

ANCHOR_MOBILE = "9444000004"
ANCHOR_PAN = "QRSTU5678V"
ANCHOR_AADHAAR = "999988887777"


def _rand_mobile():
    return "9" + "".join(str(random.randint(0, 9)) for _ in range(9))


def _rand_pan():
    letters = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    p = "".join(random.choice(letters) for _ in range(5))
    p += "".join(str(random.randint(0, 9)) for _ in range(4))
    p += random.choice(letters)
    return p


def _rand_aadhaar():
    return "".join(str(random.randint(0, 9)) for _ in range(12))


def _login_and_verify(mobile):
    r = requests.post(f"{API}/auth/login", json={"mobile": mobile})
    assert r.status_code == 200, r.text
    r = requests.post(f"{API}/auth/otp/verify", json={"mobile": mobile, "otp": OTP})
    assert r.status_code == 200, r.text
    return r.json()["data"]["token"]


def _register(mobile, pan, aadhaar, email=None, full_name="Test User"):
    _login_and_verify(mobile)
    payload = {
        "mobile": mobile,
        "fullName": full_name,
        "email": email or f"u{int(time.time()*1000)}{random.randint(0,999)}@example.com",
        "customerType": "Individual",
        "pan": pan,
        "aadhaar": aadhaar,
        "address": "123 Test St",
    }
    return requests.post(f"{API}/auth/register", json=payload)


@pytest.fixture(scope="module", autouse=True)
def anchor_user():
    """Ensure anchor user A exists with the fixed PAN/aadhaar/mobile."""
    r = _register(ANCHOR_MOBILE, ANCHOR_PAN, ANCHOR_AADHAAR,
                  email="anchor@example.com", full_name="Anchor User")
    # Either 200 (fresh or self-update) — must succeed
    assert r.status_code == 200, f"Anchor setup failed: {r.status_code} {r.text}"
    body = r.json()
    assert body.get("success") is True
    yield


def test_anchor_created_successfully():
    # sanity: hit register again with same values (self-update path)
    r = _register(ANCHOR_MOBILE, ANCHOR_PAN, ANCHOR_AADHAAR,
                  email="anchor@example.com", full_name="Anchor User")
    assert r.status_code == 200
    body = r.json()
    assert body["success"] is True
    assert body["data"]["registered"] is True


def test_pan_conflict_returns_409_masked_mobile():
    mobile_b = _rand_mobile()
    r = _register(mobile_b, ANCHOR_PAN, _rand_aadhaar())
    assert r.status_code == 409, r.text
    body = r.json()
    assert body["success"] is False
    assert body["message"] == "PAN already registered with us. Mobile: ******0004"


def test_aadhaar_conflict_returns_409_masked_mobile():
    mobile_c = _rand_mobile()
    r = _register(mobile_c, _rand_pan(), ANCHOR_AADHAAR)
    assert r.status_code == 409, r.text
    body = r.json()
    assert body["success"] is False
    assert body["message"] == "Aadhaar already registered with us. Mobile: ******0004"


def test_self_update_succeeds():
    """Anchor user re-registering with same PAN+aadhaar must succeed (exclude self)."""
    r = _register(ANCHOR_MOBILE, ANCHOR_PAN, ANCHOR_AADHAAR,
                  email="anchor@example.com", full_name="Anchor User Updated")
    assert r.status_code == 200
    body = r.json()
    assert body["success"] is True
    assert body["data"]["user"]["fullName"] == "Anchor User Updated"


def test_fresh_user_unique_pan_aadhaar_succeeds():
    mobile_d = _rand_mobile()
    r = _register(mobile_d, _rand_pan(), _rand_aadhaar())
    assert r.status_code == 200, r.text
    body = r.json()
    assert body["success"] is True
    assert body["data"]["registered"] is True
