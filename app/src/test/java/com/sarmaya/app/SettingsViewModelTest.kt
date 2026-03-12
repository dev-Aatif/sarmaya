package com.sarmaya.app

import android.content.Context
import com.sarmaya.app.data.DataStoreManager
import kotlinx.coroutines.flow.flowOf
import com.sarmaya.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import com.sarmaya.app.data.TransactionDao
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var mockContext: Context
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var transactionDao: TransactionDao
    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockContext = mock(Context::class.java)
        transactionDao = mock(TransactionDao::class.java)
        dataStoreManager = mock(DataStoreManager::class.java)
        
        `when`(dataStoreManager.darkThemePreference).thenReturn(flowOf("true"))
        `when`(dataStoreManager.username).thenReturn(flowOf("TestUser"))
        `when`(dataStoreManager.notificationsPortfolio).thenReturn(flowOf(true))
        `when`(dataStoreManager.notificationsMarket).thenReturn(flowOf(true))
        `when`(dataStoreManager.notificationsUpdates).thenReturn(flowOf(true))
        
        viewModel = SettingsViewModel(mockContext, transactionDao, dataStoreManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isDarkTheme initializes to DataStore value`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.isDarkTheme.collect { }
        }
        advanceUntilIdle()
        assertTrue(viewModel.isDarkTheme.value == true)
    }

    @Test
    fun `setTheme updates DataStore`() = runTest {
        viewModel.setTheme(false)
        advanceUntilIdle()
        verify(dataStoreManager).setDarkTheme("false")
    }
}
