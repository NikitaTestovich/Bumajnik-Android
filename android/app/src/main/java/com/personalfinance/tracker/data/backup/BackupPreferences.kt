package com.personalfinance.tracker.data.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

data class BackupPreferences(val themeId: Int, val selectedTab: String, val homeBlocks: String, val categoryIconLabels: Boolean)

object BackupPreferencesCodec {
    fun encode(value: BackupPreferences): ByteArray {
        require(value.themeId > 0 && value.selectedTab in setOf("INCOME", "EXPENSE") && value.homeBlocks.length <= 4096)
        val buffer = ByteArrayOutputStream()
        DataOutputStream(buffer).use { out ->
            out.writeInt(1); out.writeInt(value.themeId); out.writeUTF(value.selectedTab)
            out.writeUTF(value.homeBlocks); out.writeBoolean(value.categoryIconLabels)
        }
        return buffer.toByteArray()
    }
    fun decode(bytes: ByteArray): BackupPreferences {
        require(bytes.size <= 16384)
        val input = DataInputStream(ByteArrayInputStream(bytes))
        require(input.readInt() == 1)
        val result = BackupPreferences(input.readInt(), input.readUTF(), input.readUTF(), input.readBoolean())
        require(input.available() == 0)
        encode(result) // Reuse the same bounds and value validation.
        return result
    }
}
