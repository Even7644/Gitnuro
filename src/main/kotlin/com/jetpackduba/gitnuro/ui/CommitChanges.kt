package com.jetpackduba.gitnuro.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.extensions.*
import com.jetpackduba.gitnuro.git.DiffEntryType
import com.jetpackduba.gitnuro.theme.*
import com.jetpackduba.gitnuro.ui.components.*
import com.jetpackduba.gitnuro.ui.context_menu.ContextMenu
import com.jetpackduba.gitnuro.ui.context_menu.committedChangesEntriesContextMenuItems
import com.jetpackduba.gitnuro.viewmodels.CommitChangesState
import com.jetpackduba.gitnuro.viewmodels.CommitChangesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevCommit

@Composable
fun CommitChanges(
    commitChangesViewModel: CommitChangesViewModel = gitnuroViewModel(),
    selectedItem: SelectedItem.CommitBasedItem,
    onDiffSelected: (DiffEntry) -> Unit,
    diffSelected: DiffEntryType?,
    onBlame: (String) -> Unit,
    onHistory: (String) -> Unit,
) {
    LaunchedEffect(selectedItem) {
        commitChangesViewModel.loadChanges(selectedItem.revCommit)
    }

    val commitChangesStatus = commitChangesViewModel.commitChangesState.collectAsState().value
    val showSearch by commitChangesViewModel.showSearch.collectAsState()
    val changesListScroll by commitChangesViewModel.changesLazyListState.collectAsState()
    val textScroll by commitChangesViewModel.textScroll.collectAsState()

    var searchFilter by remember(commitChangesViewModel, showSearch, commitChangesStatus) {
        mutableStateOf(commitChangesViewModel.searchFilter.value)
    }

    when (commitChangesStatus) {
        CommitChangesState.Loading -> {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colors.primaryVariant)
        }

        is CommitChangesState.Loaded -> {
            CommitChangesView(
                diffSelected = diffSelected,
                commit = commitChangesStatus.commit,
                changes = commitChangesStatus.changesFiltered,
                onBlame = onBlame,
                onHistory = onHistory,
                showSearch = showSearch,
                changesListScroll = changesListScroll,
                textScroll = textScroll,
                searchFilter = searchFilter,
                onDiffSelected = onDiffSelected,
                onSearchFilterToggled = { visible ->
                    commitChangesViewModel.onSearchFilterToggled(visible)
                },
                onSearchFilterChanged = { filter ->
                    searchFilter = filter
                    commitChangesViewModel.onSearchFilterChanged(filter)
                },
            )
        }
    }
}

@Composable
fun CommitChangesView(
    commit: RevCommit,
    changes: List<DiffEntry>,
    diffSelected: DiffEntryType?,
    changesListScroll: LazyListState,
    textScroll: ScrollState,
    showSearch: Boolean,
    searchFilter: TextFieldValue,
    onBlame: (String) -> Unit,
    onHistory: (String) -> Unit,
    onDiffSelected: (DiffEntry) -> Unit,
    onSearchFilterToggled: (Boolean) -> Unit,
    onSearchFilterChanged: (TextFieldValue) -> Unit,
) {

    /**
     * State used to prevent the text field from getting the focus when returning from another tab
     */
    var requestFocus by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(end = 8.dp, bottom = 8.dp)
            .fillMaxSize(),
    ) {
        val searchFocusRequester = remember { FocusRequester() }

        Column(
            modifier = Modifier
                .padding(bottom = 4.dp)
                .fillMaxWidth()
                .weight(1f, fill = true)
                .background(MaterialTheme.colors.background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(MaterialTheme.colors.tertiarySurface),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    text = "Files changed",
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Left,
                    color = MaterialTheme.colors.onBackground,
                    maxLines = 1,
                    style = MaterialTheme.typography.body2,
                )

                Box(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = {
                        onSearchFilterToggled(!showSearch)

                        if (!showSearch)
                            requestFocus = true
                    },
                    modifier = Modifier.handOnHover(),
                ) {
                    Icon(
                        painter = painterResource(AppIcons.SEARCH),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colors.onBackground,
                    )
                }
            }

            if (showSearch) {
                SearchTextField(
                    searchFilter = searchFilter,
                    onSearchFilterChanged = onSearchFilterChanged,
                    searchFocusRequester = searchFocusRequester,
                    onClose = { onSearchFilterToggled(false) },
                )
            }

            LaunchedEffect(showSearch, requestFocus) {
                if (showSearch && requestFocus) {
                    searchFocusRequester.requestFocus()
                    requestFocus = false
                }
            }

            CommitLogChanges(
                diffSelected = diffSelected,
                changesListScroll = changesListScroll,
                diffEntries = changes,
                onDiffSelected = onDiffSelected,
                onBlame = onBlame,
                onHistory = onHistory,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colors.background),
        ) {
            SelectionContainer {
                Text(
                    text = commit.fullMessage,
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(8.dp)
                        .verticalScroll(textScroll),
                )
            }

            Author(commit.shortName, commit.name, commit.authorIdent)
        }
    }
}

@Composable
fun Author(
    shortName: String,
    name: String,
    author: PersonIdent,
) {
    var copied by remember(name) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(MaterialTheme.colors.tertiarySurface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(40.dp),
            personIdent = author,
        )

        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            TooltipText(
                text = author.name,
                maxLines = 1,
                style = MaterialTheme.typography.body2,
                tooltipTitle = author.emailAddress,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = shortName,
                    color = MaterialTheme.colors.onBackgroundSecondary,
                    maxLines = 1,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.handMouseClickable {
                        scope.launch {
                            clipboard.setText(AnnotatedString(name))
                            copied = true
                            delay(2000) // 2s
                            copied = false
                        }
                    }
                )

                if (copied) {
                    Text(
                        text = "Copied!",
                        color = MaterialTheme.colors.primaryVariant,
                        maxLines = 1,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }


                Spacer(modifier = Modifier.weight(1f, fill = true))

                val smartDate = remember(author) {
                    author.`when`.toSmartSystemString()
                }

                val systemDate = remember(author) {
                    author.`when`.toSystemDateTimeString()
                }

                TooltipText(
                    text = smartDate,
                    color = MaterialTheme.colors.onBackgroundSecondary,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.body2,
                    tooltipTitle = systemDate
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommitLogChanges(
    diffEntries: List<DiffEntry>,
    diffSelected: DiffEntryType?,
    changesListScroll: LazyListState,
    onBlame: (String) -> Unit,
    onHistory: (String) -> Unit,
    onDiffSelected: (DiffEntry) -> Unit,
) {
    ScrollableLazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        state = changesListScroll,
    ) {
        items(items = diffEntries) { diffEntry ->
            ContextMenu(
                items = {
                    committedChangesEntriesContextMenuItems(
                        diffEntry,
                        onBlame = { onBlame(diffEntry.filePath) },
                        onHistory = { onHistory(diffEntry.filePath) },
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                        .handMouseClickable {
                            onDiffSelected(diffEntry)
                        }
                        .backgroundIf(
                            condition = diffSelected is DiffEntryType.CommitDiff && diffSelected.diffEntry == diffEntry,
                            color = MaterialTheme.colors.backgroundSelected,
                        ),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Spacer(modifier = Modifier.weight(2f))

                    Row {
                        Icon(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .size(16.dp),
                            imageVector = diffEntry.icon,
                            contentDescription = null,
                            tint = diffEntry.iconColor,
                        )

                        if (diffEntry.parentDirectoryPath.isNotEmpty()) {
                            Text(
                                text = diffEntry.parentDirectoryPath.removeSuffix("/"),
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.body2,
                                color = MaterialTheme.colors.onBackgroundSecondary,
                            )

                            Text(
                                text = "/",
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.body2,
                                overflow = TextOverflow.Visible,
                                color = MaterialTheme.colors.onBackgroundSecondary,
                            )
                        }
                        Text(
                            text = diffEntry.fileName,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(end = 16.dp),
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onBackground,
                        )
                    }

                    Spacer(modifier = Modifier.weight(2f))

                }
            }
        }
    }
}