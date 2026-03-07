package com.sarmaya.app

import android.content.Context
import android.content.SharedPreferences
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

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var mockContext: Context
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var transactionDao: TransactionDao
    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockContext = mock(Context::class.java)
        sharedPrefs = mock(SharedPreferences::class.java)
        editor = mock(SharedPreferences.Editor::class.java)
        transactionDao = mock(TransactionDao::class.java)
        
        `when`(mockContext.getSharedPreferences("sarmaya_settings", Context.MODE_PRIVATE)).thenReturn(sharedPrefs)
        `when`(sharedPrefs.edit()).thenReturn(editor)
        `when`(sharedPrefs.getBoolean(eq("dark_theme"), anyBoolean())).thenReturn(true) // Default to true
        `when`(sharedPrefs.contains("dark_theme")).thenReturn(true)
        
        viewModel = SettingsViewModel(mockContext, transactionDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isDarkTheme initializes to SharedPreferences value`() = runTest {
        assertTrue(viewModel.isDarkTheme.value == true)
    }

    @Test
    fun `setTheme updates SharedPreferences and StateFlow`() = runTest {
        viewModel.setTheme(false)
        verify(editor).putBoolean("dark_theme", false)
        verify(editor).apply()
        
        // Emulate the listener callback
        `when`(sharedPrefs.getBoolean(eq("dark_theme"), anyBoolean())).thenReturn(false)
        
        viewModel.setTheme(false) // Trigger StateFlow validation
        assertFalse(viewModel.isDarkTheme.value == true)
    }
}
