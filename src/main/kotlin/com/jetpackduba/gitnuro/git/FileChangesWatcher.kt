package com.jetpackduba.gitnuro.git

import com.jetpackduba.gitnuro.system.systemSeparator
import com.jetpackduba.gitnuro.logging.printLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.attribute.BasicFileAttributes
import javax.inject.Inject

private const val TAG = "FileChangesWatcher"

class FileChangesWatcher @Inject constructor() {

    private val _changesNotifier = MutableSharedFlow<Boolean>()
    val changesNotifier: SharedFlow<Boolean> = _changesNotifier
    val keys = mutableMapOf<WatchKey, Path>()

    suspend fun watchDirectoryPath(pathStr: String, ignoredDirsPath: List<String>) = withContext(Dispatchers.IO) {
        val watchService = FileSystems.getDefault().newWatchService()

        val path = Paths.get(pathStr)

        path.register(
            watchService,
            ENTRY_CREATE,
            ENTRY_DELETE,
            ENTRY_MODIFY
        )

        // register directory and subdirectories but ignore dirs by gitignore
        Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
            @Throws(IOException::class)
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                val isIgnoredDirectory = ignoredDirsPath.any { "$pathStr/$it" == dir.toString() }

                return if (!isIgnoredDirectory && !isGitDir(dir, pathStr)) {
                    val watchKey = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
                    keys[watchKey] = dir
                    FileVisitResult.CONTINUE
                } else {
                    FileVisitResult.SKIP_SUBTREE
                }
            }
        })

        var key: WatchKey
        while (watchService.take().also { key = it } != null) {
            val events = key.pollEvents()

            val dir = keys[key] ?: return@withContext

            _changesNotifier.emit(false)

            // Check if new directories have been added to add them to the watchService
            launch(Dispatchers.IO) {
                for (event in events) {
                    if (event.kind() == ENTRY_CREATE) {
                        try {
                            val eventFile = File(dir.toAbsolutePath().toString() + systemSeparator + event.context())

                            if (eventFile.isDirectory) {
                                val eventPath = eventFile.toPath()
                                printLog(TAG, "New directory $eventFile detected, adding it to watchService")
                                val watchKey =
                                    eventPath.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
                                keys[watchKey] = eventPath
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }
            }

            key.reset()
        }
    }

    private fun isGitDir(dir: Path, pathStr: String): Boolean {
        return dir.startsWith("$pathStr$systemSeparator.git$systemSeparator")
    }
}