package com.personalfinance.tracker.data.csv

import org.junit.Assert.assertTrue
import org.junit.Test

class CsvLimitsTest {
    @Test fun boundedReaderAcceptsLimitAndRejectsOneMoreByte() {
        val limit = ByteArray(10 * 1024 * 1024)
        assertTrue(readCsvBytes(limit.inputStream()).size == limit.size)
        try {
            readCsvBytes((limit + byteArrayOf(1)).inputStream())
            throw AssertionError("oversized stream accepted")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test fun rejectsOversizedFieldWithoutImportingRecords() {
        val csv = "owner,record_id,date,category,income,expense,currency,comment\n" + "x".repeat(65537) + ",00000000-0000-0000-0000-000000000001,2026-10-01,,1.00,,USD,\n"
        val result = FinanceCsvCodec.decode(csv.toByteArray())
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.records.isEmpty())
    }

    @Test fun rejectsOversizedInputWithoutImportingRecords() {
        val result = FinanceCsvCodec.decode(ByteArray(10 * 1024 * 1024 + 1) { 32 })
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.records.isEmpty())
    }

    @Test fun limitsRowsBeforeBuildingAnUnboundedList() {
        val header = "owner,record_id,date,category,income,expense,currency\n"
        val atLimit = FinanceCsvCodec.decode((header + "x\n".repeat(100_000)).toByteArray())
        assertTrue(atLimit.errors.size == 100)
        assertTrue(atLimit.errors.none { it.contains("too many rows") })
        val tooMany = FinanceCsvCodec.decode((header + "x\n".repeat(100_001)).toByteArray())
        assertTrue(tooMany.errors.single().contains("too many rows"))
        assertTrue(tooMany.records.isEmpty())
    }

    @Test fun capsErrorsAndDoesNotReturnPartialRecords() {
        val header = "owner,record_id,date,category,income,expense,currency\n"
        val valid = "Alice,00000000-0000-0000-0000-000000000001,2026-10-01,,1.00,,USD\n"
        val result = FinanceCsvCodec.decode((header + valid + "x\n".repeat(101)).toByteArray())
        assertTrue(result.errors.size == 100)
        assertTrue(result.records.isEmpty())
    }

    @Test fun quotedFieldsEnforceDecodedLengthIncludingEscapedQuotesAndNewlines() {
        val header = "owner,record_id,date,category,income,expense,currency\n"
        val suffix = ",00000000-0000-0000-0000-000000000001,2026-10-01,,1.00,,USD\n"
        val owner = "a".repeat(65_534) + "\"\n"
        val escaped = owner.replace("\"", "\"\"")
        val valid = FinanceCsvCodec.decode((header + "\"" + escaped + "\"" + suffix).toByteArray())
        assertTrue(valid.errors.isEmpty())
        assertTrue(valid.records.single().owner == owner.trim())
        val oversized = FinanceCsvCodec.decode((header + "\"" + escaped + "b\"" + suffix).toByteArray())
        assertTrue(oversized.errors.single().contains("field is too large"))
        assertTrue(oversized.records.isEmpty())
    }

    @Test fun legacyAndCurrentRecordsStillDecodeWithExactText() {
        val header = "owner,record_id,date,category,income,expense,currency"
        val row = "Alice,00000000-0000-0000-0000-000000000001,2026-10-01,,1.00,,USD"
        val legacy = FinanceCsvCodec.decode((header + "\n" + row + "\n").toByteArray())
        val current = FinanceCsvCodec.decode((header + ",comment\n" + row + ",\"=1+1\"\n").toByteArray())
        assertTrue(legacy.errors.isEmpty())
        assertTrue(legacy.records.single().comment == "")
        assertTrue(current.errors.isEmpty())
        assertTrue(current.records.single().comment == "=1+1")
    }
    @Test fun cancellationStopsReadingAndClosesStream() {
        var closed = false
        var checks = 0
        val input = object : java.io.ByteArrayInputStream(ByteArray(32_768)) {
            override fun close() { closed = true; super.close() }
        }
        try {
            input.use { readCsvBytes(it) { if (++checks == 2) throw java.util.concurrent.CancellationException() } }
            throw AssertionError("cancellation ignored")
        } catch (_: java.util.concurrent.CancellationException) {
        }
        assertTrue(closed)
        assertTrue(input.available() > 0)
    }

    @Test fun cancellationStopsParsingAndPropagates() {
        var checks = 0
        val input = ("owner,record_id,date,category,income,expense,currency\n" + "x".repeat(16_000)).toByteArray()
        try {
            FinanceCsvCodec.decode(input) { if (++checks == 2) throw java.util.concurrent.CancellationException() }
            throw AssertionError("cancellation ignored")
        } catch (_: java.util.concurrent.CancellationException) {
        }
        assertTrue(checks == 2)
    }
    @Test fun exportRejectsRecordsBeyondImportRowLimit() {
        val row = com.personalfinance.tracker.domain.model.FinanceTransaction(
            "00000000-0000-0000-0000-000000000001", "Alice", com.personalfinance.tracker.domain.model.TransactionType.INCOME,
            java.time.LocalDate.of(2026, 10, 1), com.personalfinance.tracker.domain.money.Money(100, "USD"), null,
        )
        try {
            FinanceCsvCodec.encode(java.util.Collections.nCopies(100_001, row), "Alice")
            throw AssertionError("oversized export accepted")
        } catch (_: CsvExportLimitException) { }
    }

    @Test fun exportRejectsFieldsAndBytesBeyondImportLimits() {
        val row = com.personalfinance.tracker.domain.model.FinanceTransaction(
            "00000000-0000-0000-0000-000000000001", "Alice", com.personalfinance.tracker.domain.model.TransactionType.INCOME,
            java.time.LocalDate.of(2026, 10, 1), com.personalfinance.tracker.domain.money.Money(100, "USD"), null,
        )
        try {
            FinanceCsvCodec.encode(listOf(row), "a".repeat(65_537))
            throw AssertionError("oversized field accepted")
        } catch (_: CsvExportLimitException) { }
        try {
            FinanceCsvCodec.encode(java.util.Collections.nCopies(200, row), "я".repeat(30_000))
            throw AssertionError("oversized UTF-8 output accepted")
        } catch (_: CsvExportLimitException) { }
        val acceptable = FinanceCsvCodec.encode(listOf(row), "я".repeat(100))
        assertTrue(FinanceCsvCodec.decode(acceptable).errors.isEmpty())
    }}
