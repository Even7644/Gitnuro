import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import git.StashStatus
import org.eclipse.jgit.revwalk.RevCommit
import theme.headerBackground

@Composable
fun Stashes(gitManager: GitManager) {
    val stashStatusState = gitManager.stashStatus.collectAsState()
    val stashStatus = stashStatusState.value

    val stashList = if (stashStatus is StashStatus.Loaded)
        stashStatus.stashes
    else
        listOf()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f)
            .padding(8.dp)
    ) {
        Column {
            Text(
                modifier = Modifier
                    .background(MaterialTheme.colors.headerBackground)
                    .padding(vertical = 16.dp)
                    .fillMaxWidth(),
                text = "Stashes",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                maxLines = 1,
            )

            LazyColumn(modifier = Modifier.weight(5f)) {
                items(items = stashList) { stash ->
                    StashRow(
                        stash = stash,
                    )

                }
            }
        }
    }
}

@Composable
private fun StashRow(stash: RevCommit) {
    Row(
        modifier = Modifier
            .height(56.dp)
            .fillMaxWidth()
            .clickable(onClick = {}),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Icon(
            painter = painterResource("stash.svg"),
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(24.dp),
            tint = MaterialTheme.colors.primary,
        )

        Text(
            text = stash.name,
            modifier = Modifier.weight(1f, fill = true),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        IconButton(
            onClick = {},
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
            )
        }
    }
}