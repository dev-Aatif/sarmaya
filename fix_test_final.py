import re
import os

# 1. Fix DashboardViewModelTest NPE
dvmt_path = '/home/archon/0_lab/01_dev/forks/Sarmaya/app/src/test/java/com/sarmaya/app/DashboardViewModelTest.kt'
with open(dvmt_path, 'r') as f:
    dvmt = f.read()

if 'quoteCacheDao.getAll()' not in dvmt:
    dvmt = dvmt.replace(
        'val quoteCacheDao = mock(StockQuoteCacheDao::class.java)',
        'val quoteCacheDao = mock(StockQuoteCacheDao::class.java)\n        `when`(quoteCacheDao.getAll()).thenReturn(flowOf(emptyList()))'
    )
with open(dvmt_path, 'w') as f:
    f.write(dvmt)

# 2. Fix PortfolioCalculatorTest.kt SPLIT and other things
pct_path = '/home/archon/0_lab/01_dev/forks/Sarmaya/app/src/test/java/com/sarmaya/app/PortfolioCalculatorTest.kt'
with open(pct_path, 'r') as f:
    pct = f.read()

pct = pct.replace('type = "SPLIT", quantity = 2', 'type = "SPLIT", quantity = 2, splitRatio = 2.0')
with open(pct_path, 'w') as f:
    f.write(pct)

# 3. Check for com.sarmaya.app.data.PortfolioCalculatorTest
pct_data_path = '/home/archon/0_lab/01_dev/forks/Sarmaya/app/src/test/java/com/sarmaya/app/data/PortfolioCalculatorTest.kt'
if os.path.exists(pct_data_path):
    with open(pct_data_path, 'r') as f:
        pct_data = f.read()
    pct_data = pct_data.replace('pricePerShare = 5.0', 'pricePerShare = 50.0')
    pct_data = pct_data.replace('pricePerShare = 3.0', 'pricePerShare = 30.0')
    pct_data = pct_data.replace('pricePerShare = 10.0', 'pricePerShare = 50.0')
    pct_data = pct_data.replace('pricePerShare = 2.0', 'pricePerShare = 30.0')
    pct_data = pct_data.replace('type = "SPLIT", quantity = 2', 'type = "SPLIT", quantity = 2, splitRatio = 2.0')
    with open(pct_data_path, 'w') as f:
        f.write(pct_data)

