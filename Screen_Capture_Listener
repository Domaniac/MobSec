import socket
import threading
import time

# --- CONFIG ---
PORT = 7000
HOST = '0.0.0.0'

# --- GLOBALS ---
latest_frame = None
frame_lock = threading.Lock()

def handle_client(conn, addr):
    global latest_frame

    try:
        data = conn.recv(1024)
        if not data:
            conn.close()
            return
    except:
        conn.close()
        return

    # --- THE WEB INTERFACE (Auto-fits to screen) ---
    if b"GET / " in data or b"GET /index.html" in data:
        html = """HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n
        <html>
        <head>
            <title>Android Live</title>
            <style>
                body {
                    margin: 0; padding: 0; background: #000;
                    display: flex; align-items: center; justify-content: center;
                    height: 100vh; width: 100vw; overflow: hidden;
                }
                img {
                    max-height: 100vh; max-width: 100vw;
                    object-fit: contain;
                }
            </style>
        </head>
        <body><img src="/video"></body>
        </html>"""
        conn.sendall(html.encode())
        conn.close()
        return

    # --- THE VIDEO STREAM ---
    if b"GET /video" in data:
        header = ("HTTP/1.1 200 OK\r\n"
                  "Content-Type: multipart/x-mixed-replace; boundary=frame\r\n\r\n")
        try:
            conn.sendall(header.encode())
            while True:
                with frame_lock:
                    img_data = latest_frame[:] if latest_frame else None

                if img_data:
                    msg = (f"--frame\r\nContent-Type: image/jpeg\r\n"
                           f"Content-Length: {len(img_data)}\r\n\r\n")
                    conn.sendall(msg.encode() + img_data + b"\r\n")
                    time.sleep(0.04) # Control refresh rate
                else:
                    time.sleep(0.1)
        except:
            pass
        finally:
            conn.close()
        return

    # --- INCOMING DATA FROM EMULATOR ---
    else:
        print(f"[+] Emulator Feed Started: {addr}")
        stream_buffer = data
        try:
            while True:
                chunk = conn.recv(16384)
                if not chunk: break
                stream_buffer += chunk

                while True:
                    a = stream_buffer.find(b'\xff\xd8') # JPEG Start
                    b = stream_buffer.find(b'\xff\xd9') # JPEG End
                    if a != -1 and b != -1:
                        jpg = stream_buffer[a:b+2]
                        stream_buffer = stream_buffer[b+2:]
                        with frame_lock:
                            latest_frame = bytearray(jpg)
                    else:
                        break
        finally:
            conn.close()

def start_server():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    s.bind((HOST, PORT))
    s.listen(5)
    print(f"[*] View stream at http://localhost:{PORT}")

    while True:
        c, a = s.accept()
        threading.Thread(target=handle_client, args=(c, a), daemon=True).start()

if __name__ == "__main__":
    start_server()
