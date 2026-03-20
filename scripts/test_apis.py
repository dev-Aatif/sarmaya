#!/usr/bin/env python3
"""
Diagnostic script to test which PSX data APIs are accessible.
Run on your phone/laptop to see which endpoints work:

  python3 test_apis.py

This helps determine the correct API to use in the Sarmaya app.
"""

import urllib.request
import json
import sys
import time

APIS = [
    # PSX DPS (original)
    {
        "name": "PSX DPS - Live Quotes",
        "url": "https://dps.psx.com.pk/cache/live.json",
        "headers": {"User-Agent": "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36"},
    },
    {
        "name": "PSX DPS - Indices",
        "url": "https://dps.psx.com.pk/cache/indices.json",
        "headers": {"User-Agent": "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36"},
    },
    # PSX Terminal
    {
        "name": "PSX Terminal - Market Data",
        "url": "https://psxterminal.com/api/market-data",
        "headers": {"User-Agent": "Mozilla/5.0", "Accept": "application/json"},
    },
    {
        "name": "PSX Terminal - Market Data (REG)",
        "url": "https://psxterminal.com/api/market-data?market=REG",
        "headers": {"User-Agent": "Mozilla/5.0", "Accept": "application/json"},
    },
    {
        "name": "PSX Terminal - Stats",
        "url": "https://psxterminal.com/api/stats",
        "headers": {"User-Agent": "Mozilla/5.0", "Accept": "application/json"},
    },
    {
        "name": "PSX Terminal - Company (OGDC)",
        "url": "https://psxterminal.com/api/company/OGDC",
        "headers": {"User-Agent": "Mozilla/5.0", "Accept": "application/json"},
    },
    {
        "name": "PSX Terminal - Yields (OGDC)",
        "url": "https://psxterminal.com/api/yields/OGDC",
        "headers": {"User-Agent": "Mozilla/5.0", "Accept": "application/json"},
    },
    # Yahoo Finance (query2 mirror)
    {
        "name": "Yahoo Finance v7 (query2) - Quote",
        "url": "https://query2.finance.yahoo.com/v7/finance/quote?symbols=OGDC.KA",
        "headers": {"User-Agent": "Mozilla/5.0"},
    },
    {
        "name": "Yahoo Finance v8 (query2) - Chart",
        "url": "https://query2.finance.yahoo.com/v8/finance/chart/OGDC.KA?range=1mo&interval=1d",
        "headers": {"User-Agent": "Mozilla/5.0"},
    },
    # Yahoo Finance (query1 original)
    {
        "name": "Yahoo Finance v8 (query1) - Chart",
        "url": "https://query1.finance.yahoo.com/v8/finance/chart/OGDC.KA?range=1mo&interval=1d",
        "headers": {"User-Agent": "Mozilla/5.0"},
    },
    # PSX Symbols (used by build_psx_db.py)
    {
        "name": "PSX DPS - Symbols",
        "url": "https://dps.psx.com.pk/symbols",
        "headers": {"User-Agent": "Mozilla/5.0"},
    },
]


def test_api(api):
    name = api["name"]
    url = api["url"]
    headers = api.get("headers", {})
    
    print(f"\n{'='*60}")
    print(f"Testing: {name}")
    print(f"URL: {url}")
    
    try:
        req = urllib.request.Request(url, headers=headers)
        start = time.time()
        with urllib.request.urlopen(req, timeout=10) as response:
            elapsed = time.time() - start
            status = response.status
            data = response.read().decode("utf-8", errors="replace")
            
            print(f"  ✅ Status: {status} ({elapsed:.1f}s)")
            print(f"  📦 Size: {len(data)} bytes")
            
            # Try to parse as JSON and show structure
            try:
                parsed = json.loads(data)
                if isinstance(parsed, list):
                    print(f"  📊 Type: Array with {len(parsed)} items")
                    if len(parsed) > 0:
                        first = parsed[0]
                        print(f"  🔑 First item keys: {list(first.keys()) if isinstance(first, dict) else type(first).__name__}")
                        print(f"  📝 First item: {json.dumps(first, indent=2)[:500]}")
                elif isinstance(parsed, dict):
                    print(f"  📊 Type: Object with keys: {list(parsed.keys())}")
                    # Show a sample
                    sample = json.dumps(parsed, indent=2)[:500]
                    print(f"  📝 Sample: {sample}")
            except json.JSONDecodeError:
                print(f"  ⚠️ Not valid JSON. First 200 chars: {data[:200]}")
                    
            return True
            
    except urllib.error.HTTPError as e:
        print(f"  ❌ HTTP Error: {e.code} {e.reason}")
        try:
            body = e.read().decode("utf-8", errors="replace")[:200]
            print(f"  📝 Response: {body}")
        except:
            pass
        return False
    except urllib.error.URLError as e:
        print(f"  ❌ URL Error: {e.reason}")
        return False
    except Exception as e:
        print(f"  ❌ Error: {type(e).__name__}: {e}")
        return False


def main():
    print("🔍 Sarmaya API Diagnostic Tool")
    print("=" * 60)
    
    results = {}
    for api in APIS:
        results[api["name"]] = test_api(api)
    
    print("\n\n" + "=" * 60)
    print("📋 SUMMARY")
    print("=" * 60)
    for name, ok in results.items():
        status = "✅ WORKING" if ok else "❌ FAILED"
        print(f"  {status} — {name}")
    
    working = [n for n, ok in results.items() if ok]
    if not working:
        print("\n⚠️ No APIs are reachable. Check your internet connection.")
    else:
        print(f"\n✅ {len(working)} API(s) working out of {len(results)}")


if __name__ == "__main__":
    main()
