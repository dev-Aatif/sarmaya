import os
import re

test_dir = '/home/archon/0_lab/01_dev/forks/Sarmaya/app/src/test/java/com/sarmaya/app'

for root, _, files in os.walk(test_dir):
    for file in files:
        if file.endswith('.kt'):
            path = os.path.join(root, file)
            with open(path, 'r') as f:
                content = f.read()

            # 1. Fix FakeStockDao
            content = re.sub(
                r'override suspend fun updateTickData\(.*?\)\s*{}',
                r'override suspend fun updateTickData(symbol: String, price: Double, change: Double, changePercent: Double, volume: Long, trades: Long, value: Long, high: Double, low: Double, state: String, updatedAt: Long) {}',
                content,
                flags=re.DOTALL
            )
            
            # If updateTickData is missing, insert it into FakeStockDao
            if 'updateTickData' not in content and 'FakeStockDao' in content:
                content = re.sub(
                    r'(class FakeStockDao.*?\{)',
                    r'\1\n        override suspend fun updateTickData(symbol: String, price: Double, change: Double, changePercent: Double, volume: Long, trades: Long, value: Long, high: Double, low: Double, state: String, updatedAt: Long) {}',
                    content,
                    flags=re.DOTALL
                )
                
            # For anonymous FakeStockDao objects
            if 'object : StockDao' in content or 'object: FakeStockDao' in content:
                if 'updateTickData' not in content:
                    content = content.replace('object: FakeStockDao() {', 'object: FakeStockDao() {\n        override suspend fun updateTickData(symbol: String, price: Double, change: Double, changePercent: Double, volume: Long, trades: Long, value: Long, high: Double, low: Double, state: String, updatedAt: Long) {}')
                    content = content.replace('object : FakeStockDao() {', 'object : FakeStockDao() {\n        override suspend fun updateTickData(symbol: String, price: Double, change: Double, changePercent: Double, volume: Long, trades: Long, value: Long, high: Double, low: Double, state: String, updatedAt: Long) {}')

            # 2. Fix FakeTransactionDao
            if 'insertTransactions' not in content and 'FakeTransactionDao' in content:
                content = re.sub(
                    r'(class FakeTransactionDao.*?\{)',
                    r'\1\n        override suspend fun insertTransactions(transactions: List<com.sarmaya.app.data.Transaction>) {}',
                    content,
                    flags=re.DOTALL
                )
            
            if 'object: FakeTransactionDao' in content or 'object : FakeTransactionDao' in content:
                if 'insertTransactions' not in content:
                    content = content.replace('object: FakeTransactionDao(emptyList()) {', 'object: FakeTransactionDao(emptyList()) {\n        override suspend fun insertTransactions(transactions: List<com.sarmaya.app.data.Transaction>) {}')
                    content = content.replace('object : FakeTransactionDao(emptyList()) {', 'object : FakeTransactionDao(emptyList()) {\n        override suspend fun insertTransactions(transactions: List<com.sarmaya.app.data.Transaction>) {}')

            # 3. Remove WatchlistDao references
            content = re.sub(r'import com\.sarmaya\.app\.data\.WatchlistDao\n', '', content)
            content = re.sub(r'val watchlistDao\d* = mock\(WatchlistDao::class\.java\)\n', '', content)
            
            # Remove watchlistDao from ViewModel constructors
            # This handles both watchlistDao and watchlistDao2
            content = re.sub(r',\s*watchlistDao\d*', '', content)
            content = re.sub(r'watchlistDao\d*,\s*', '', content)

            # 4. SettingsViewModelTest
            if 'SettingsViewModelTest' in file:
                if 'GithubApi' not in content:
                    content = content.replace('import org.mockito.Mockito.*', 'import org.mockito.Mockito.*\nimport com.sarmaya.app.network.github.GithubApi')
                    content = content.replace('viewModel = SettingsViewModel(mockContext, transactionDao, dataStoreManager)', 'val githubApi = mock(GithubApi::class.java)\n        viewModel = SettingsViewModel(mockContext, transactionDao, dataStoreManager, githubApi)')

            # 5. DashboardViewModelTest
            if 'DashboardViewModelTest' in file:
                if 'StockQuoteCacheDao' not in content:
                    content = content.replace('import com.sarmaya.app.data.Stock', 'import com.sarmaya.app.data.Stock\nimport com.sarmaya.app.data.StockQuoteCacheDao\nimport com.sarmaya.app.network.websocket.PsxWebSocketManager')
                    content = content.replace('viewModel = DashboardViewModel(transactionDao, stockDao, portfolioDao, dataStoreManager, stockDataRepository)', 
                                              'val quoteCacheDao = mock(StockQuoteCacheDao::class.java)\n        val wsManager = mock(PsxWebSocketManager::class.java)\n        viewModel = DashboardViewModel(transactionDao, stockDao, portfolioDao, dataStoreManager, stockDataRepository, quoteCacheDao, wsManager)')

            with open(path, 'w') as f:
                f.write(content)

