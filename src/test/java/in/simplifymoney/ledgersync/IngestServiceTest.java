package in.simplifymoney.ledgersync;

import static org.junit.jupiter.api.Assertions.assertEquals;

import in.simplifymoney.ledgersync.ingest.IngestService;
import in.simplifymoney.ledgersync.model.NormalizedTxn;
import in.simplifymoney.ledgersync.parse.Parsers;
import in.simplifymoney.ledgersync.store.InMemoryLedgerStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class IngestServiceTest {

    @Test
    void mergesMultipleMessagesForSameTransaction() throws Exception {

        Path corpus = Files.createTempFile("duplicate-transactions", ".jsonl");

        String message1 =
                """
                {"message_id":"m-dup-001","channel":"sms","sender":"AD-HDFCBK-S","received_at":"2026-07-04T11:56:00+05:30","device_id":"dev-test","body":"Rs.5 debited from a/c **4821 on 04-07-26 at 11:54 to UPI/WATER CAN. Avl Bal: Rs.92,213.10. Not you? Call 18002586161"}
                """;

        String message2 =
                """
                {"message_id":"m-dup-002","channel":"sms","sender":"AD-HDFCBK-S","received_at":"2026-07-04T11:57:00+05:30","device_id":"dev-test","body":"Rs.5 debited from a/c **4821 on 04-07-26 at 11:54 to UPI/WATER CAN. Avl Bal: Rs.92,213.10. Not you? Call 18002586161"}
                """;

        Files.writeString(corpus, message1 + message2);

        InMemoryLedgerStore store = new InMemoryLedgerStore();

        IngestService service = new IngestService(
                new Parsers(),
                store
        );

        IngestService.Stats stats = service.ingestFile(corpus);

        assertEquals(2, stats.messagesRead());

        // Two messages are evidence of one real transaction.
        assertEquals(1, store.count());

        NormalizedTxn txn = store.all().get(0);

        assertEquals("4821", txn.accountLast4());
        assertEquals("5.00", txn.amount().toPlainString());
        assertEquals("UPI/WATER CAN", txn.merchant());

        assertEquals(
                List.of("m-dup-001", "m-dup-002"),
                txn.sourceMessageIds()
        );
    }

    @Test
    void ingestingSameCorpusTwiceDoesNotDuplicateTransactionOrEvidence() throws Exception {

        Path corpus = Files.createTempFile("idempotent-ingest", ".jsonl");

        String message1 =
                """
                {"message_id":"m-repeat-001","channel":"sms","sender":"AD-HDFCBK-S","received_at":"2026-07-04T11:56:00+05:30","device_id":"dev-test","body":"Rs.5 debited from a/c **4821 on 04-07-26 at 11:54 to UPI/WATER CAN. Avl Bal: Rs.92,213.10. Not you? Call 18002586161"}
                """;

        String message2 =
                """
                {"message_id":"m-repeat-002","channel":"sms","sender":"AD-HDFCBK-S","received_at":"2026-07-04T11:57:00+05:30","device_id":"dev-test","body":"Rs.5 debited from a/c **4821 on 04-07-26 at 11:54 to UPI/WATER CAN. Avl Bal: Rs.92,213.10. Not you? Call 18002586161"}
                """;

        Files.writeString(corpus, message1 + message2);

        InMemoryLedgerStore store = new InMemoryLedgerStore();

        IngestService service = new IngestService(
                new Parsers(),
                store
        );

        // First ingestion
        service.ingestFile(corpus);

        assertEquals(1, store.count());

        // Same corpus ingested again
        service.ingestFile(corpus);

        // Must still be one real transaction
        assertEquals(1, store.count());

        NormalizedTxn txn = store.all().get(0);

        // Evidence IDs must not be duplicated either
        assertEquals(
                List.of("m-repeat-001", "m-repeat-002"),
                txn.sourceMessageIds()
        );
    }
}