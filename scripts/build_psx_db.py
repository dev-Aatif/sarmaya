import urllib.request
import json
import sqlite3
import os

URL = "https://dps.psx.com.pk/symbols"
DB_PATH = "../app/src/main/assets/database/sarmaya.db"

def main():
    print("Fetching PSX symbols...")
    req = urllib.request.Request(URL, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode())
    
    # Filter out debt and ETFs
    equities = [item for item in data if not item.get("isDebt", False) and not item.get("isETF", False)]
    print(f"Found {len(equities)} equity symbols.")

    db_dir = os.path.dirname(DB_PATH)
    os.makedirs(db_dir, exist_ok=True)
    
    if os.path.exists(DB_PATH):
        os.remove(DB_PATH)
        
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    # ─── Stock table (matches Room v3 schema) ───
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS Stock (
        symbol TEXT NOT NULL,
        name TEXT NOT NULL,
        sector TEXT NOT NULL,
        currentPrice REAL NOT NULL,
        priceUpdatedAt INTEGER,
        PRIMARY KEY(symbol)
    )
    ''')
    
    # ─── Transaction table ───
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS `Transaction` (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        portfolioId INTEGER NOT NULL DEFAULT 1,
        stockSymbol TEXT NOT NULL,
        type TEXT NOT NULL,
        quantity INTEGER NOT NULL,
        pricePerShare REAL NOT NULL,
        date INTEGER NOT NULL,
        notes TEXT NOT NULL DEFAULT '',
        commissionType TEXT NOT NULL DEFAULT 'FLAT',
        commissionAmount REAL NOT NULL DEFAULT 0.0
    )
    ''')
    
    # ─── WatchlistItem table ───
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS WatchlistItem (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        stockSymbol TEXT NOT NULL,
        addedAt INTEGER NOT NULL,
        notes TEXT NOT NULL DEFAULT ''
    )
    ''')
    
    # ─── StockQuoteCache table ───
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS StockQuoteCache (
        symbol TEXT NOT NULL PRIMARY KEY,
        price REAL NOT NULL,
        change REAL NOT NULL,
        changePercent REAL NOT NULL,
        volume INTEGER NOT NULL,
        high REAL NOT NULL,
        low REAL NOT NULL,
        cachedAt INTEGER NOT NULL
    )
    ''')
    
    # ─── Portfolio table ───
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS Portfolio (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        name TEXT NOT NULL,
        createdAt INTEGER NOT NULL,
        isDefault INTEGER NOT NULL
    )
    ''')
    
    # Insert default portfolio
    import time
    cursor.execute('''
        INSERT INTO Portfolio (id, name, createdAt, isDefault) 
        VALUES (1, 'My Portfolio', ?, 1)
    ''', (int(time.time() * 1000),))
    
    # Insert stock data
    inserted = 0
    for stock in equities:
        symbol = stock.get("symbol", "").strip()
        name = stock.get("name", "").strip()
        sector = stock.get("sectorName", "").strip()
        
        if not symbol or not name:
            continue
            
        cursor.execute('''
            INSERT INTO Stock (symbol, name, sector, currentPrice, priceUpdatedAt)
            VALUES (?, ?, ?, 0.0, NULL)
        ''', (symbol, name, sector))
        inserted += 1
        
    conn.commit()
    conn.close()
    print(f"Successfully generated {DB_PATH} with {inserted} stocks.")

if __name__ == "__main__":
    main()
