import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import extensions.filePath
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import java.io.IOException

@Composable
fun RepositorySelected(gitManager: GitManager, repository: Repository) {
    var selectedRevCommit by remember {
        mutableStateOf<Pair<RevCommit, List<DiffEntry>>?>(null)
    }

    var diffSelected by remember {
        mutableStateOf<DiffEntry?>(null)
    }
    var uncommitedChangesSelected by remember {
        mutableStateOf<Boolean>(false)
    }

    val selectedIndexCommitLog = remember { mutableStateOf(-1) }

    Row {
        Column (
            modifier = Modifier
                .weight(0.15f)
                .fillMaxHeight()
        ) {
            Branches(gitManager = gitManager)
            Stashes(gitManager = gitManager)
        }
        Box(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
        ) {
            Crossfade(targetState = diffSelected) { diffEntry ->
                when (diffEntry) {
                    null -> {
                        Log(
                            gitManager = gitManager,
                            selectedIndex = selectedIndexCommitLog,
                            onRevCommitSelected = { commit ->
                                uncommitedChangesSelected = false

                                val parent = if (commit.parentCount == 0) {
                                    null
                                } else
                                    commit.parents.first()

                                val oldTreeParser =
                                    prepareTreeParser(repository, parent!!) //TODO Will crash with first commit
                                val newTreeParser = prepareTreeParser(repository, commit)
                                Git(repository).use { git ->
                                    val diffs = git.diff()
                                        .setNewTree(newTreeParser)
                                        .setOldTree(oldTreeParser)
                                        .call()

                                    selectedRevCommit = commit to diffs
                                }
                            },
                            onUncommitedChangesSelected = {
                                uncommitedChangesSelected = true
                                gitManager.updateStatus()
                            }
                        )
                    }
                    else -> {
                        Diff(
                            gitManager = gitManager,
                            diffEntry = diffEntry,
                            onCloseDiffView = { diffSelected = null })
                    }
                }
            }

        }
        Box(
            modifier = Modifier
                .weight(0.15f)
                .fillMaxHeight()
        ) {
            if (uncommitedChangesSelected) {
                UncommitedChanges(
                    gitManager = gitManager,
                    onDiffEntrySelected = { diffEntry ->
                        println(diffEntry.filePath)
                        diffSelected = diffEntry
                    }
                )
            } else {
                selectedRevCommit?.let {
                    CommitChanges(
                        commitDiff = it,
                        onDiffSelected = { diffEntry ->
                            diffSelected = diffEntry
                        }
                    )
                }
            }
        }
    }
}


@Throws(IOException::class)
fun prepareTreeParser(repository: Repository, commit: RevCommit): AbstractTreeIterator? {
    // from the commit we can build the tree which allows us to construct the TreeParser
    RevWalk(repository).use { walk ->
        val tree: RevTree = walk.parseTree(commit.tree.id)
        val treeParser = CanonicalTreeParser()
        repository.newObjectReader().use { reader -> treeParser.reset(reader, tree.id) }
        walk.dispose()
        return treeParser
    }
}