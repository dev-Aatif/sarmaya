package com.sarmaya.app.network.repository

import com.sarmaya.app.data.NewsArticle
import com.sarmaya.app.data.NewsArticleDao
import com.sarmaya.app.network.ConnectivityChecker
import com.sarmaya.app.network.rss.GoogleRssParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.MockitoAnnotations

class NewsRepositoryTest {

    @Mock
    private lateinit var newsDao: NewsArticleDao
    
    @Mock
    private lateinit var rssParser: GoogleRssParser
    
    @Mock
    private lateinit var connectivityChecker: ConnectivityChecker

    private lateinit var repository: NewsRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = NewsRepository(newsDao, rssParser, connectivityChecker)
    }

    @Test
    fun `refreshNews returns failure when offline`() = runBlocking {
        `when`(connectivityChecker.isOnline()).thenReturn(false)
        
        val result = repository.refreshNews()
        
        assertTrue(result.isFailure)
        verifyNoInteractions(rssParser)
        verifyNoInteractions(newsDao)
    }

    @Test
    fun `refreshNews fetches and saves news when online`() = runBlocking {
        val articles = listOf(
            NewsArticle(title = "Test Article", link = "http://test.com", date = System.currentTimeMillis())
        )
        `when`(connectivityChecker.isOnline()).thenReturn(true)
        `when`(rssParser.fetchAndParse()).thenReturn(articles)
        
        val result = repository.refreshNews()
        
        assertTrue(result.isSuccess)
        verify(rssParser).fetchAndParse()
        verify(newsDao).insertAll(articles)
    }

    @Test
    fun `refreshNews failure does not crash and returns failure`() = runBlocking {
        `when`(connectivityChecker.isOnline()).thenReturn(true)
        `when`(rssParser.fetchAndParse()).thenThrow(RuntimeException("Network error"))
        
        val result = repository.refreshNews()
        
        assertTrue(result.isFailure)
    }
}
