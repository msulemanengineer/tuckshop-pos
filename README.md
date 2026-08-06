# Tuck Shop POS

An offline point-of-sale system for a tuck shop at a petrol pump. Runs entirely
on the counter PC — billing, inventory, and sales history all work with zero
internet connection. The owner can view the dashboard from his phone over the
shop's WiFi (also no internet required).

## What's included

**Phase 1:**
- **Dashboard** — today's sales, transactions, stock value (owner only), low stock alerts, weekly sales chart, top sellers, recent transactions
- **Billing (POS)** — barcode scanning, live search fallback, cart, multiple payment methods, stock auto-deducts on sale
- **Inventory** — add/edit/delete products (owner only), everyone can view/search for billing
- **Sales history** — every transaction, with void support

**Phase 2 (new):**
- **Login system** — separate Owner and Cashier accounts. Cashiers can bill, view stock, manage khata payments, and view sales history, but cannot add/edit/delete products, cannot see Reports, and cannot manage staff accounts. This is enforced on the server, not just hidden in the UI.
- **Khata (customer credit ledger)** — add regular customers with a credit limit, sell to them on credit from the POS ("Khata" payment option), record payments they make later, see full ledger per customer, dashboard-style outstanding balance summary
- **Receipt printing** — optional "Print receipt" button after checkout (nothing prints automatically). Opens a receipt-formatted page and uses your browser's normal print dialog, which works with any thermal receipt printer installed as a regular Windows printer
- **Reports** — owner only. Date range presets (Today, Yesterday, Last 7 days, This month, Last month, This quarter, This year, All time) or a custom range, filterable by customer or payment method, with sales-by-day chart, top products, payment method breakdown, khata sales by customer, and CSV export
- **Shift cash reconciliation** — cashier enters opening cash at the start of their shift, enters counted cash at the end, system shows the expected total and flags any mismatch
- **Sale voiding with owner PIN** — a cashier can request a sale be voided (e.g. wrong item scanned), but it only goes through with the owner's PIN — so the owner can approve it over the phone without needing to be on-site or log in themselves. Voiding restocks the items and reverses any khata charge automatically.
- **Activity log** — every price change, product add/edit/delete, and voided sale is logged with who did it and when (visible to owner, on the Reports page)

### Default logins (change these immediately)

| Role | Username | Password | PIN |
|---|---|---|---|
| Owner | `owner` | `owner123` | `1234` |
| Cashier | `cashier` | `cashier123` | - |

Change passwords from the **Staff accounts** page (owner only) → click "Reset password" next to each account. The PIN is currently fixed at setup time (`1234`) — if you want to change it, tell me and I'll add a "change PIN" option.

## Not yet built (tell me if you want these next)

Purchase orders & suppliers, expense tracking, barcode label generation/printing, multi-terminal support, daily SMS/WhatsApp summary to the owner.

---

## 1. What you need to install

| Tool | Version | Why | Link |
|---|---|---|---|
| **Java JDK** | 17 or newer | Runs the application | https://adoptium.net/ (choose "Temurin 17 LTS") |
| **Maven** | 3.9+ | Builds the project and downloads libraries | https://maven.apache.org/download.cgi |

That's it. No database server, no Node, no separate web server — everything
is bundled into one runnable application.

### Check your installs (open Command Prompt / Terminal)
```bash
java -version
mvn -version
```
Both should print a version number. If not, reinstall and make sure they're
added to your system PATH (the installers usually offer to do this automatically).

---

## 2. Running it for the first time

1. Unzip this project anywhere, e.g. `C:\tuckshop-pos` or `~/tuckshop-pos`
2. Open a terminal inside that folder
3. Run:
   ```bash
   mvn spring-boot:run
   ```
4. Wait for the terminal to show:
   ```
   Tuck Shop POS is running.
   On this computer, open: http://localhost:8080
   ```
5. Open that link in a browser (Chrome/Edge/Firefox) on the counter PC.

The first time it starts, it automatically creates its local database file
(`data/tuckshop.mv.db` inside the project folder) and loads 15 sample tuck
shop products so you can try it immediately. You can edit or delete these
from the Inventory page.

## 3. Letting the owner check it from his phone (no internet needed)

1. Make sure the counter PC and the owner's phone are joined to the **same
   WiFi router** (a basic router is enough — internet on that router is optional)
2. On the counter PC, find its local IP address:
   - Windows: `ipconfig` → look for "IPv4 Address" (e.g. `192.168.1.10`)
   - Mac/Linux: `ifconfig` or `ip a`
3. On the phone's browser, open `http://192.168.1.10:8080` (use your PC's actual IP)
4. Bookmark it — now he can check the dashboard any time he's on-site, even
   with zero internet.

## 4. Setting up the barcode scanner

Any standard USB laser barcode scanner works with zero configuration — plug
it in, click into the "Scan barcode" box on the Billing page, and scan. The
scanner types the code and presses Enter automatically, just like a keyboard.
No drivers or special setup needed on Windows.

## 5. Packaging it as a single file to run without typing commands (optional)

Once you're happy with it, run:
```bash
mvn clean package
```
This creates `target/tuckshop-pos.jar`. From then on you (or the owner) can
start the whole system by double-clicking a simple script instead of typing
Maven commands:

**Windows** — save as `start-pos.bat` next to the jar:
```bat
@echo off
java -jar tuckshop-pos.jar
pause
```

**Mac/Linux** — save as `start-pos.sh`, then `chmod +x start-pos.sh`:
```bash
#!/bin/bash
java -jar tuckshop-pos.jar
```

You can also set this to run automatically when the counter PC boots up
(Windows Task Scheduler / a login item), so the owner never has to think
about starting it.

---

## Project structure

```
tuckshop-pos/
  pom.xml                          # Maven config, all dependencies
  src/main/java/com/tuckshop/pos/
    PosApplication.java            # entry point
    model/                         # Product, Sale, SaleItem (database tables)
    repository/                    # database queries
    service/                       # business logic (checkout, stock, stats)
    controller/                    # web pages + REST API endpoints
    dto/                           # request/response shapes
    config/DataInitializer.java    # loads sample products on first run
  src/main/resources/
    application.properties         # database & server settings
    templates/                     # HTML pages (Thymeleaf)
    static/css/style.css           # all styling & animations
    static/js/                     # page behavior (pos.js, products.js, dashboard.js...)
```

## Troubleshooting

- **Port 8080 already in use** — edit `src/main/resources/application.properties`,
  change `server.port=8080` to e.g. `server.port=9090`
- **Want to start fresh / wipe all data** — stop the app, delete the `data/`
  folder inside the project, restart
- **`mvn` not recognized** — Maven isn't on your PATH; reinstall and restart your terminal
