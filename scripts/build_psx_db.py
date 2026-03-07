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
    
    # Filter out debt and ETFs, and GEM boards if we just want main equity. 
    # Or keep GEM. The user said ~561 equities, filtered from debt.
    equities = [item for item in data if not item.get("isDebt", False) and not item.get("isETF", False)]
    print(f"Found {len(equities)} equity symbols.")

    db_dir = os.path.dirname(DB_PATH)
    os.makedirs(db_dir, exist_ok=True)
    
    if os.path.exists(DB_PATH):
        os.remove(DB_PATH)
        
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    # Create the Stock table matching Room's schema exactly
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
    
    # Create room master table (optional, but good practice if you know the hash, 
    # but Room will auto-generate it if missing and schema matches).
    # We will just rely on schema matching. Room compares columns perfectly.
    
    # Insert data
    inserted = 0
    for stock in equities:
        # symbol, name, sectorName
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
