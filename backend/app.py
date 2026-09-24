import json
import os
from datetime import datetime, timezone

from flask import Flask, jsonify, request

app = Flask(__name__)

LOG_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "logs")
LOG_FILE = os.path.join(LOG_DIR, "mobile_sentinel.log")


@app.route("/ingest", methods=["POST"])
def ingest():
    try:
        payload = request.get_json(force=True)
    except Exception:
        return jsonify({"error": "invalid json"}), 400

    payload["received_at"] = datetime.now(timezone.utc).isoformat()

    os.makedirs(LOG_DIR, exist_ok=True)
    with open(LOG_FILE, "a", encoding="utf-8") as f:
        f.write(json.dumps(payload) + "\n")

    return jsonify({"status": "ok"}), 200


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
