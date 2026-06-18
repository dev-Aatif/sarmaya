import urllib.request
import json
try:
    req = urllib.request.Request("https://psxterminal.com/api/status", headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req) as r:
        print("Status:", r.read().decode()[:100])
except Exception as e:
    print("Status error:", e)

try:
    req = urllib.request.Request("https://psxterminal.com/api/ticks/REG/ENGRO", headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req) as r:
        print("ENGRO tick:", r.read().decode()[:100])
except Exception as e:
    print("ENGRO tick error:", e)

try:
    req = urllib.request.Request("https://psxterminal.com/api/klines/ENGRO/1h", headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req) as r:
        print("Klines:", r.read().decode()[:100])
except Exception as e:
    print("Klines error:", e)
