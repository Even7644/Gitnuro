package com.jetpackduba.gitnuro.viewmodels

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.text.input.TextFieldValue
import com.jetpackduba.gitnuro.extensions.delayedStateChange
import com.jetpackduba.gitnuro.extensions.filePath
import com.jetpackduba.gitnuro.extensions.fullData
import com.jetpackduba.gitnuro.extensions.lowercaseContains
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.diff.GetCommitDiffEntriesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

private const val MIN_TIME_IN_MS_TO_SHOW_LOAD = 300L

class CommitChangesViewModel @Inject constructor(
    private val tabState: TabState,
    private val getCommitDiffEntriesUseCase: GetCommitDiffEntriesUseCase,
    tabScope: CoroutineScope,
) {
    private val _showSearch = MutableStateFlow(false)
    val showSearch: StateFlow<Boolean> = _showSearch

    private val _searchFilter = MutableStateFlow(TextFieldValue(""))
    val searchFilter: StateFlow<TextFieldValue> = _searchFilter

    val changesLazyListState = MutableStateFlow(
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    )

    val textScroll = MutableStateFlow(
        ScrollState(0)
    )

    private val _commitChangesState = MutableStateFlow<CommitChangesState>(CommitChangesState.Loading)
    val commitChangesState: StateFlow<CommitChangesState> =
        combine(_commitChangesState, _showSearch, _searchFilter) { state, showSearch, filter ->
            if (state is CommitChangesState.Loaded) {
                if (showSearch && filter.text.isNotBlank()) {
                    state.copy(changesFiltered = state.changes.filter { it.filePath.lowercaseContains(filter.text) })
                } else {
                    state
                }
            } else {
                state
            }
        }.stateIn(
            tabScope,
            SharingStarted.Lazily,
            CommitChangesState.Loading
        )


    fun loadChanges(commit: RevCommit) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        val state = _commitChangesState.value

        // Check if it's a different commit before resetting everything
        if (
            state is CommitChangesState.Loading ||
            state is CommitChangesState.Loaded && state.commit != commit
        ) {
            delayedStateChange(
                delayMs = MIN_TIME_IN_MS_TO_SHOW_LOAD,
                onDelayTriggered = { _commitChangesState.value = CommitChangesState.Loading }
            ) {
                val fullCommit = commit.fullData(git.repository)

                if (fullCommit != null) {
                    val changes = getCommitDiffEntriesUseCase(git, fullCommit).toMutableList()

                    if (fullCommit.parentCount == 3) {
                        val untrackedFilesCommit =
                            fullCommit.parents?.firstOrNull {
                                val parentCommit = it.fullData(git.repository) ?: return@firstOrNull false

                                parentCommit.fullMessage.startsWith("untracked files on") && parentCommit.parentCount == 0
                            }

                        if (untrackedFilesCommit != null) {
                            val untrackedFilesChanges = getCommitDiffEntriesUseCase(git, untrackedFilesCommit)

                            if(untrackedFilesChanges.all { it.changeType == DiffEntry.ChangeType.ADD }) { // All files should be new
                                changes.addAll(untrackedFilesChanges)
                            }
                        }
                    }

                    _commitChangesState.value = CommitChangesState.Loaded(commit, changes, changes)
                }
            }

            _showSearch.value = false
            _searchFilter.value = TextFieldValue("")
            changesLazyListState.value = LazyListState(
                0,
                0
            )
            textScroll.value = ScrollState(0)
        }
    }

    fun onSearchFilterToggled(visible: Boolean) {
        _showSearch.value = visible
    }

    fun onSearchFilterChanged(filter: TextFieldValue) {
        _searchFilter.value = filter
    }
}

sealed interface CommitChangesState {
    data object Loading : CommitChangesState
    data class Loaded(val commit: RevCommit, val changes: List<DiffEntry>, val changesFiltered: List<DiffEntry>) :
        CommitChangesState
}

