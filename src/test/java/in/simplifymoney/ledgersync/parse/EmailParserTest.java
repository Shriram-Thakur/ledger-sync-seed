package in.simplifymoney.ledgersync.parse;

import in.simplifymoney.ledgersync.model.Direction;
import in.simplifymoney.ledgersync.model.RawMessage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EmailParserTest {

    private final EmailParser parser = new EmailParser();

    @Test
    void parsesDebitTransactionEmail() {
        RawMessage message = new RawMessage(
                "m-email-001",
                "email",
                "alerts@hdfcbank.net",
                OffsetDateTime.parse("2026-07-22T06:40:00+05:30"),
                "dev-test",
                """
                Date: Wed, 22 Jul 2026 06:15:00 +0530
                Subject: Transaction alert on your account

                Dear Customer,

                Your account ending 4821 has been debited with Rs.649.00.
                Merchant / Remarks: NETFLIX ENTERTAINMENT
                Transaction reference: 9201732154

                This is a system generated email.
                """
        );

        ParsedTxn txn = parser.parse(message).orElseThrow();

        assertEquals("4821", txn.accountLast4());
        assertEquals(Direction.DEBIT, txn.direction());
        assertEquals(new BigDecimal("649.00"), txn.amount());
        assertEquals("NETFLIX ENTERTAINMENT", txn.merchant());
        assertEquals("m-email-001", txn.sourceMessageId());
    }

    @Test
    void parsesCreditTransactionEmail() {
        RawMessage message = new RawMessage(
                "m-email-002",
                "email",
                "alerts@hdfcbank.net",
                OffsetDateTime.parse("2026-07-25T12:51:00+05:30"),
                "dev-test",
                """
                Date: Sat, 25 Jul 2026 12:44:00 +0530
                Subject: Transaction alert on your account

                Dear Customer,

                Your account ending 4821 has been credited with INR 1250.33.
                Merchant / Remarks: UPI/P2P/REFUND
                Transaction reference: 3008522132

                This is a system generated email.
                """
        );

        ParsedTxn txn = parser.parse(message).orElseThrow();

        assertEquals(Direction.CREDIT, txn.direction());
        assertEquals(new BigDecimal("1250.33"), txn.amount());
    }

    @Test
    void preservesTimezoneFromEmailDate() {
        RawMessage message = new RawMessage(
                "m-email-003",
                "email",
                "alerts@hdfcbank.net",
                OffsetDateTime.parse("2026-07-19T00:26:00+05:30"),
                "dev-test",
                """
                Date: Sat, 18 Jul 2026 18:50:00 +0000
                Subject: Transaction alert on your account

                Dear Customer,

                Your account ending 4821 has been debited with INR 412.67.
                Merchant / Remarks: UBER INDIA
                Transaction reference: 4190129089

                This is a system generated email.
                """
        );

        ParsedTxn txn = parser.parse(message).orElseThrow();

        assertEquals(
                OffsetDateTime.parse("2026-07-18T18:50:00Z"),
                txn.occurredAt()
        );
    }
}