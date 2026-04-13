package com.sarmaya.app.viewmodel

import com.sarmaya.app.data.*
import com.sarmaya.app.network.StockDataRepository
import com.sarmaya.app.network.websocket.PsxWebSocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class MarketViewModelTest {

    @Mock private lateinit var repository: StockDataRepository
    @Mock private lateinit var stockDao: StockDao
    @Mock private lateinit var quoteCacheDao: StockQuoteCacheDao
    @Mock private lateinit var watchlistDao: WatchlistDao
    @Mock private lateinit var wsManager: PsxWebSocketManager

    private lateinit var viewModel: MarketViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Default mock behaviors
        `when`(stockDao.getAllStocks()).thenReturn(flowOf(emptyList()))
        `when`(quoteCacheDao.getAll()).thenReturn(flowOf(emptyList()))
        `when`(watchlistDao.getAllItems()).thenReturn(flowOf(emptyList()))
        `when`(wsManager.tickUpdates).thenReturn(flowOf())
        
        viewModel = MarketViewModel(repository, stockDao, quoteCacheDao, watchlistDao, wsManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `marketStocks filters by search query`() = runTest {
        val stock1 = Stock(symbol = "LUCK", name = "Lucky Cement", sector = "Cement")
        val stock2 = Stock(symbol = "EPCL", name = "Engro Polymer", sector = "Chemical")
        
        `when`(stockDao.getAllStocks()).thenReturn(flowOf(listOf(stock1, stock2)))
        
        // Update query
        viewModel.updateSearchQuery("LUCK")
        
        val results = viewModel.marketStocks.value
        assertEquals(1, results.size)
        assertEquals("LUCK", results[0].stock.symbol)
    }

    @Test
    fun `marketStocks filters by sector`() = runTest {
        val stock1 = Stock(symbol = "LUCK", name = "Lucky Cement", sector = "Cement")
        val stock2 = Stock(symbol = "EPCL", name = "Engro Polymer", sector = "Chemical")
        
        `when`(stockDao.getAllStocks()).thenReturn(flowOf(listOf(stock1, stock2)))
        
        viewModel.selectSector("Chemical")
        
        val results = viewModel.marketStocks.value
        assertEquals(1, results.size)
        assertEquals("EPCL", results[0].stock.symbol)
    }

    @Test
    fun `marketStocks shows watchlist only when toggled`() = runTest {
        val stock1 = Stock(symbol = "LUCK", name = "Lucky Cement", sector = "Cement")
        val stock2 = Stock(symbol = "EPCL", name = "Engro Polymer", sector = "Chemical")
        
        `when`(stockDao.getAllStocks()).thenReturn(flowOf(listOf(stock1, stock2)))
        `when`(watchlistDao.getAllItems()).thenReturn(flowOf(listOf(WatchlistItem(stockSymbol = "LUCK"))))
        
        viewModel.toggleOnlyWatchlist()
        
        val results = viewModel.marketStocks.value
        assertEquals(1, results.size)
        assertEquals("LUCK", results[0].stock.symbol)
    }
}
