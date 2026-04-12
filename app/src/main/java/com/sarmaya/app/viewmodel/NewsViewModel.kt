package com.sarmaya.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.sarmaya.app.SarmayaApplication
import com.sarmaya.app.data.NewsArticle
import com.sarmaya.app.network.repository.NewsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NewsViewModel(
    private val newsRepository: NewsRepository
) : ViewModel() {

    val newsArticles: StateFlow<List<NewsArticle>> = newsRepository.getAllNewsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshNews()
    }

    fun refreshNews() {
        viewModelScope.launch {
            newsRepository.refreshNews()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as SarmayaApplication
                return NewsViewModel(application.container.newsRepository) as T
            }
        }
    }
}
