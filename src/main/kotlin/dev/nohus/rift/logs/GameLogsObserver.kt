package dev.nohus.rift.logs

import dev.nohus.rift.logs.DirectoryObserver.DirectoryObserverEvent.FileEvent
import dev.nohus.rift.logs.DirectoryObserver.DirectoryObserverEvent.OverflowEvent
import dev.nohus.rift.logs.DirectoryObserver.FileEventType.Created
import dev.nohus.rift.logs.parse.GameLogFileMetadata
import dev.nohus.rift.logs.parse.GameLogFileParser
import dev.nohus.rift.logs.parse.GameLogMessage
import dev.nohus.rift.logs.parse.GameLogMessageWithMetadata
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import java.io.File
import java.io.IOException

private val logger = KotlinLogging.logger {}

@Single
class GameLogsObserver(
    private val directoryObserver: DirectoryObserver,
    private val matchGameLogFilenameUseCase: MatchGameLogFilenameUseCase,
    private val logFileParser: GameLogFileParser,
) {

    private val logFiles = mutableListOf<GameLogFile>()
    private var activeLogFiles: Map<String, GameLogFileMetadata> = emptyMap() // String is the filename
    private var onMessageCallback: ((GameLogMessageWithMetadata) -> Unit)? = null
    private val handledMessages = mutableSetOf<GameLogMessage>()

    suspend fun observe(
        directory: File,
        onCharacterLogin: (characterId: Int) -> Unit,
        onMessage: (GameLogMessageWithMetadata) -> Unit,
    ) {
        logFiles.clear()
        activeLogFiles = emptyMap()
        onMessageCallback = onMessage

        logger.info { "Observing game logs: $directory" }
        reloadLogFiles(directory)
        directoryObserver.observe(directory) { event ->
            when (event) {
                is FileEvent -> {
                    val logFile = matchGameLogFilenameUseCase(event.file)
                    if (logFile != null) {
                        when (event.type) {
                            Created -> {
                                logFiles += logFile
                                updateActiveLogFiles()
                                matchGameLogFilenameUseCase(event.file)?.characterId?.toIntOrNull()?.let { onCharacterLogin(it) }
                            }
                            DirectoryObserver.FileEventType.Deleted -> {
                                val file = logFiles.find { it.file.name == logFile.file.name }
                                if (file != null) logFiles -= file
                                updateActiveLogFiles()
                            }
                            DirectoryObserver.FileEventType.Modified -> {
                                activeLogFiles[logFile.file.name]?.let { metadata ->
                                    readLogFile(logFile, metadata) // TODO: Optimise, we don't need to reread the file in full
                                }
                            }
                        }
                    }
                }
                OverflowEvent -> reloadLogFiles(directory)
            }
        }
    }

    fun stop() {
        directoryObserver.stop()
    }

    private fun reloadLogFiles(directory: File) {
        val logFiles = directory.listFiles()?.mapNotNull { file ->
            matchGameLogFilenameUseCase(file)
        } ?: emptyList()
        this.logFiles.clear()
        this.logFiles.addAll(logFiles)
        updateActiveLogFiles()
    }

    private fun updateActiveLogFiles() {
        try {
            val currentActiveLogFiles = logFiles.toList()
                .filter { it.file.exists() }
                .groupBy { it.characterId }
                .mapNotNull { (characterId, playerLogFiles) ->
                    // Take the latest file for this player
                    val logFile = playerLogFiles.maxBy { it.lastModified }
                    val existingMetadata = activeLogFiles[logFile.file.name]
                    val metadata = existingMetadata ?: logFileParser.parseHeader(characterId, logFile.file)
                    if (metadata != null) {
                        logFile to metadata
                    } else {
                        logger.error { "Could not parse metadata for $logFile" }
                        null
                    }
                }

            val newActiveLogFiles = currentActiveLogFiles.filter { it.first.file.name !in activeLogFiles.keys }
            activeLogFiles = currentActiveLogFiles.associate { (logFile, metadata) -> logFile.file.name to metadata }

            newActiveLogFiles.forEach { (logFile, metadata) ->
                readLogFile(logFile, metadata)
            }
        } catch (e: IOException) {
            logger.error(e) { "Could not update active game log files" }
        }
    }

    private fun readLogFile(logFile: GameLogFile, metadata: GameLogFileMetadata) {
        try {
            val newMessages = logFileParser.parse(logFile.file).filter { it !in handledMessages }
            if (newMessages.isEmpty()) return
            handledMessages += newMessages
            newMessages.forEach { onMessageCallback?.invoke(GameLogMessageWithMetadata(it, metadata)) }
        } catch (e: IOException) {
            logger.error(e) { "Could not read game log file" }
        }
    }
}
