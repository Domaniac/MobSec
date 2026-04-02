# MobSec: Educational Platform & Security Research Case Study

## Project Overview
MobSec is an Android application developed as a dual-purpose platform. Nominally, it serves as a **Classroom Resource Management System**, allowing users (Students, Teachers, and Admins) to share, view, and manage educational resources such as PDFs and external links. 

However, beneath the surface of this functional application lies a sophisticated implementation of **covert malicious logic**, designed as a case study for mobile security research, threat detection, and advanced Android malware analysis.

---

## 1. Functional Features (The "Front")
The app provides a complete user experience for classroom management:
*   **Role-Based Access Control:** Distinct interfaces for Students, Teachers, and Admins.
*   **Resource Library:** A centralized screen (`ResourceLibraryScreen`) for uploading, viewing (with an integrated PDF renderer), and deleting educational materials.
*   **Database Integration:** Persistent storage for users, classes, and shared resources.
*   **API Integration:** Communication with a backend for resource retrieval and synchronization.

---

## 2. Malicious Logic & Security Research (The "Back")
The application implements several advanced malware techniques used to demonstrate how intent can be hidden from both average users and automated analysis tools.

### a) Malicious Logic Overview
The malicious intent is concealed by blending harmful actions into routine background system activity. Key evasion strategies include:
*   **Disguised Services:** Malicious functions are hidden behind harmless class names and metaphors (e.g., `TacoDeliveryService`, `RubbishTruckService`).
*   **Stealthy Execution:** Tasks like screenshot capture and data exfiltration are performed silently in the background.
*   **Anti-Analysis Triggers:** The logic employs a **`SafetyNet`** utility that prevents execution on emulators or in unauthorized regions (geofencing) to avoid detection by automated sandboxes.

### b) Detailed Technical Capabilities
*   **Persistence & C2:** The `TacoDeliveryService` maintains a persistent connection to a remote Command & Control (C2) server, capable of receiving and executing arbitrary shell commands via dynamic reflection on `Runtime.exec()`.
*   **Covert Data Collection:**
    *   **SMS Exfiltration:** `RubbishTruckService` queries the Android SMS content provider to extract message metadata and body content.
    *   **Root-Assisted Filesystem Scraping:** `RecyclingTruckService` and `SignageRecylingService` leverage `su` access to steal sensitive system files (like WiFi configurations) and private gallery images (`DCIM/Camera`).
*   **Advanced Obfuscation:** 
    *   **String Encryption:** All sensitive C2 details, endpoints, and shell commands are encrypted using a custom XOR-based Base64 scheme in the `SecretBox` utility.
    *   **Control Flow Obfuscation:** Uses opaque predicates and state-machine-based flow flattening to complicate static code analysis and reverse engineering.

---

## 3. Project Structure
*   **`com.example.mobsec_823.ui`**: Contains the Jetpack Compose screens for the educational platform.
*   **`com.example.mobsec_823.RecylingService`**: Houses the various exfiltration services (SMS, Images, Configs).
*   **`com.example.mobsec_823.utils`**:
    *   `SecretBox.kt`: Logic for decryption, reflection, and C2 configuration.
    *   `SafetyNet.kt`: Anti-emulator, logic bomb, and anti-analysis checks.
*   **`com.example.mobsec_823.data`**: Database helpers and entity models.

## 4. Security Disclaimer
This project is intended for **educational and research purposes only**. The malicious components are designed to demonstrate detection bypass techniques and are not for actual use in any unauthorized environment.
