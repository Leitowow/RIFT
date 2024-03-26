package dev.nohus.rift.characters

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import java.io.File
import java.io.IOException

private val logger = KotlinLogging.logger {}

@Single
class CopyEveCharacterSettingsUseCase {

    operator fun invoke(fromFile: File, toFiles: List<File>): Boolean {
        try {
            logger.info { "Copying character settings from $fromFile to $toFiles" }
            if (!fromFile.exists()) {
                logger.error { "Source character settings file does not exist" }
                return false
            }
            if (toFiles.any { !it.exists() }) {
                logger.error { "Target character settings file does not exist" }
                return false
            }
            val directory = fromFile.parentFile
            if (toFiles.any { it.parentFile != directory }) {
                logger.error { "Character settings files are not in the same directory" }
                return false
            }
            if (!directory.canWrite()) {
                logger.error { "Character settings directory is not writeable" }
                return false
            }

            toFiles.forEach { file ->
                val backup = getNewBackupFile(directory, file)
                file.copyTo(backup)
                file.delete()
                fromFile.copyTo(file)
            }

            return true
        } catch (e: IOException) {
            logger.error(e) { "Copying character settings failed" }
            return false
        }
    }

    private fun getNewBackupFile(directory: File, file: File): File {
        var count = 1
        while (true) {
            val backup = File(directory, "${file.nameWithoutExtension}_rift_backup_$count.${file.extension}")
            if (!backup.exists()) return backup
            count++
        }
    }
}
