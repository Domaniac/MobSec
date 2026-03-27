import socket
import threading
import time

# Store the latest JPEG frame in memory
# Initialize as None so we know when stream is stopped
latest_frame = None
frame_lock = threading.Lock()

# Store connection to phone
phone_conn = None

def send_command_to_phone(cmd_bytes):
    global phone_conn
    if phone_conn:
        try:
            print(f"Sending command: {cmd_bytes}")
            phone_conn.sendall(cmd_bytes)
            return True
        except Exception as e:
            print(f"Failed to send command: {e}")
            phone_conn = None
            return False
    return False

def handle_client(conn, addr):
    global latest_frame, phone_conn

    try:
        data = conn.recv(1024) # Read request immediately
        if not data:
            conn.close()
            return

        request_line = data.decode('utf-8', errors='ignore').split('\n')[0]

        # --- BROWSER COMMANDS ---

        if "GET /stop" in request_line:
            print(f"Browser {addr} requested STOP")
            send_command_to_phone(b"CMD_STOP")

            # CRITICAL FIX: Clear the frame buffer immediately
            with frame_lock:
                latest_frame = None

            response = (
                "HTTP/1.1 200 OK\r\n"
                "Content-Type: text/html\r\n"
                "Connection: close\r\n\r\n"
                "<html><body><h1>Camera Paused</h1><a href='/start'>RESUME</a></body></html>"
            )
            conn.sendall(response.encode())
            conn.close()
            return

        if "GET /start" in request_line:
            print(f"Browser {addr} requested START")
            send_command_to_phone(b"CMD_START")
            response = (
                "HTTP/1.1 200 OK\r\n"
                "Content-Type: text/html\r\n"
                "Connection: close\r\n\r\n"
                "<html><head><meta http-equiv='refresh' content='2;url=/' /></head>"
                "<body><h1>Resuming...</h1></body></html>"
            )
            conn.sendall(response.encode())
            conn.close()
            return

        if "GET /kill" in request_line:
            print(f"Browser {addr} requested KILL")
            send_command_to_phone(b"CMD_KILL")
            with frame_lock:
                latest_frame = None
            conn.sendall(b"HTTP/1.1 200 OK\r\n\r\nService Killed.")
            conn.close()
            return

        # --- BROWSER STREAM VIEWING ---
        if "GET / " in request_line or "GET /index" in request_line:
            print(f"Browser {addr} watching stream")
            header = (
                "HTTP/1.1 200 OK\r\n"
                "Content-Type: multipart/x-mixed-replace; boundary=frame\r\n"
                "Cache-Control: no-cache\r\n\r\n"
            )
            conn.sendall(header.encode())

            try:
                while True:
                    frame_data = None
                    with frame_lock:
                        if latest_frame:
                            frame_data = latest_frame[:]

                    if frame_data:
                        part_header = (
                            f"--frame\r\n"
                            f"Content-Type: image/jpeg\r\n"
                            f"Content-Length: {len(frame_data)}\r\n\r\n"
                        )
                        conn.sendall(part_header.encode() + frame_data + b"\r\n")
                        time.sleep(0.04)
                    else:
                        # If stopped/paused, just send a tiny heartbeat to keep connection alive
                        # or just wait. We wait here to save bandwidth.
                        time.sleep(0.5)
            except BrokenPipeError:
                print(f"Browser {addr} closed tab")
                return

    except Exception as e:
        pass

    # --- PHONE STREAMING LOGIC ---
    # Since we read the first 1024 bytes, we need to check if it WAS NOT a GET request
    # If it wasn't HTTP, it's likely raw data (JPEG header starts with FF D8)

    if len(data) > 0 and (b"GET " not in data):
        # This is likely the phone connecting or sending data
        print(f"Phone connected from {addr}")
        phone_conn = conn

        # We need to process the initial data chunk we read
        stream_buffer = data

        try:
            while True:
                chunk = conn.recv(8192)
                if not chunk: break
                stream_buffer += chunk

                while True:
                    a = stream_buffer.find(b'\xff\xd8')
                    b = stream_buffer.find(b'\xff\xd9')
                    if a != -1 and b != -1:
                        jpg_data = stream_buffer[a:b+2]
                        stream_buffer = stream_buffer[b+2:]
                        with frame_lock:
                            latest_frame = bytearray(jpg_data)
                    else:
                        break
        except:
            print("Phone disconnected")
            if phone_conn == conn: phone_conn = None
        finally:
            conn.close()
    else:
        conn.close()

def start_server():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(('0.0.0.0', 6767))
    server.listen(10)
    print("Relay Server running on port 6767...")

    while True:
        conn, addr = server.accept()
        threading.Thread(target=handle_client, args=(conn, addr), daemon=True).start()

if __name__ == "__main__":
    start_server()
