package com.personalfinance.tracker.data.backup

import com.personalfinance.tracker.data.local.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/** Versioned database snapshot encoding. Does not include preferences or lock credentials. */
object FullBackupCodec {
    private const val VERSION = 1
    private const val LIMIT = 100_000

    fun encode(snapshot: FullBackupSnapshot): ByteArray {
        validateFullBackup(snapshot)
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { out ->
            out.writeInt(VERSION)
            out.writeBoolean(snapshot.profile != null)
            snapshot.profile?.let { out.writeInt(it.id); out.writeUTF(it.name); out.writeUTF(it.languageTag); out.writeUTF(it.baseCurrency); out.writeBoolean(it.onboardingComplete) }
            out.list(snapshot.categories) { c -> out.writeUTF(c.id); out.nullable(c.nameRu); out.nullable(c.nameEn); out.writeBoolean(c.isCustom); out.writeBoolean(c.isArchived); out.writeUTF(c.iconKey) }
            out.list(snapshot.transactions) { t -> out.writeUTF(t.id); out.writeUTF(t.owner); out.writeUTF(t.type); out.writeUTF(t.date); out.writeLong(t.minorUnits); out.writeUTF(t.currencyCode); out.nullable(t.categoryId); out.writeUTF(t.comment); out.writeLong(t.createdOrder) }
            out.list(snapshot.familyTransactions) { t -> out.writeUTF(t.id); out.writeUTF(t.owner); out.writeUTF(t.type); out.writeUTF(t.date); out.writeLong(t.minorUnits); out.writeUTF(t.currencyCode); out.nullable(t.categoryName); out.nullable(t.importRate); out.writeUTF(t.importBaseCurrency); out.nullable(t.batchId); out.writeUTF(t.comment) }
            out.list(snapshot.batches) { b -> out.writeUTF(b.id); out.writeLong(b.importedAtEpochMillis); out.writeInt(b.recordCount) }
            out.list(snapshot.rates) { r -> out.writeUTF(r.rangeStart); out.writeUTF(r.rangeEndExclusive); out.writeUTF(r.baseCurrency); out.writeUTF(r.sourceCurrency); out.writeUTF(r.rateToBase) }
            out.writeBoolean(snapshot.goal != null)
            snapshot.goal?.let { out.writeInt(it.id); out.writeLong(it.targetMinorUnits); out.writeUTF(it.targetCurrency); out.writeLong(it.savedMinorUnits); out.writeUTF(it.savedCurrency) }
        }
        return output.toByteArray()
    }

    fun decode(bytes: ByteArray): FullBackupSnapshot {
        require(bytes.size <= 50 * 1024 * 1024)
        val input = DataInputStream(ByteArrayInputStream(bytes))
        require(input.readInt() == VERSION)
        val profile = if (input.readBoolean()) UserProfileEntity(input.readInt(), input.readUTF(), input.readUTF(), input.readUTF(), input.readBoolean()) else null
        val categories = input.list { CategoryEntity(input.readUTF(), input.nullable(), input.nullable(), input.readBoolean(), input.readBoolean(), input.readUTF()) }
        val transactions = input.list { FinanceTransactionEntity(input.readUTF(), input.readUTF(), input.readUTF(), input.readUTF(), input.readLong(), input.readUTF(), input.nullable(), input.readUTF(), input.readLong()) }
        val family = input.list { FamilyTransactionEntity(input.readUTF(), input.readUTF(), input.readUTF(), input.readUTF(), input.readLong(), input.readUTF(), input.nullable(), input.nullable(), input.readUTF(), input.nullable(), input.readUTF()) }
        val batches = input.list { FamilyImportBatchEntity(input.readUTF(), input.readLong(), input.readInt()) }
        val rates = input.list { ExchangeRateEntity(input.readUTF(), input.readUTF(), input.readUTF(), input.readUTF(), input.readUTF()) }
        val goal = if (input.readBoolean()) SavingsGoalEntity(input.readInt(), input.readLong(), input.readUTF(), input.readLong(), input.readUTF()) else null
        require(input.available() == 0)
        return FullBackupSnapshot(profile, categories, transactions, family, batches, rates, goal).also(::validateFullBackup)
    }

    private fun DataOutputStream.nullable(value: String?) { writeBoolean(value != null); if (value != null) writeUTF(value) }
    private fun DataInputStream.nullable(): String? = if (readBoolean()) readUTF() else null
    private fun <T> DataOutputStream.list(items: List<T>, write: (T) -> Unit) { require(items.size <= LIMIT); writeInt(items.size); items.forEach(write) }
    private fun <T> DataInputStream.list(read: () -> T): List<T> { val count = readInt(); require(count in 0..LIMIT); return List(count) { read() } }
}
