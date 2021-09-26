import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import git.LogStatus
import org.eclipse.jgit.revwalk.RevCommit
import theme.primaryTextColor
import theme.secondaryTextColor

@Composable
fun Log(
    gitManager: GitManager,
    onRevCommitSelected: (RevCommit) -> Unit,
    onUncommitedChangesSelected: () -> Unit,
    selectedIndex: MutableState<Int> = remember { mutableStateOf(-1) }
) {
    val logStatusState = gitManager.logStatus.collectAsState()
    val logStatus = logStatusState.value

    val selectedUncommited = remember { mutableStateOf(false) }

    val log = if (logStatus is LogStatus.Loaded) {
        logStatus.commits
    } else
        listOf()

    Card(
        modifier = Modifier
            .padding(8.dp)
            .background(MaterialTheme.colors.surface)
            .fillMaxSize()
    ) {
        val hasUncommitedChanges by gitManager.hasUncommitedChanges.collectAsState()

        LazyColumn(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .fillMaxSize()
        ) {

            if (hasUncommitedChanges)
                item {
                    val textColor = if (selectedUncommited.value) {
                        MaterialTheme.colors.primary
                    } else
                        MaterialTheme.colors.primaryTextColor

                    Column(
                        modifier = Modifier
                            .height(64.dp)
                            .fillMaxWidth()
                            .clickable {
                                selectedIndex.value = -1
                                selectedUncommited.value = true
                                onUncommitedChangesSelected()
                            },
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Spacer(modifier = Modifier.weight(2f))

                        Text(
                            text = "Uncommited changes",
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(start = 16.dp),
                            color = textColor,
                        )
                        Text(
                            text = "You",
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(start = 16.dp),
                            color = MaterialTheme.colors.secondaryTextColor,
                        )

                        Spacer(modifier = Modifier.weight(2f))

                        Divider()
                    }
                }

            itemsIndexed(items = log) { index, item ->
                val textColor = if (selectedIndex.value == index) {
                    MaterialTheme.colors.primary
                } else
                    MaterialTheme.colors.primaryTextColor

                Column(
                    modifier = Modifier
                        .height(64.dp)
                        .fillMaxWidth()
                        .clickable {
                            selectedIndex.value = index
                            selectedUncommited.value = false
                            onRevCommitSelected(item)
                        },
                    verticalArrangement = Arrangement.Center,
                ) {
                    Spacer(modifier = Modifier.weight(2f))

                    Text(
                        text = item.shortMessage,
                        modifier = Modifier.padding(start = 16.dp),
                        color = textColor,
                    )
                    Text(
                        text = item.authorIdent.name,
                        modifier = Modifier.padding(start = 16.dp),
                        color = MaterialTheme.colors.secondaryTextColor,
                    )
                    Spacer(modifier = Modifier.weight(2f))

                    Divider()
                }
            }
        }
    }

}