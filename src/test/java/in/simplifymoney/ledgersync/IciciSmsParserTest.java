package in.simplifymoney.ledgersync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import in.simplifymoney.ledgersync.model.Direction;
import in.simplifymoney.ledgersync.model.RawMessage;
import in.simplifymoney.ledgersync.parse.IciciSmsParser;
import in.simplifymoney.ledgersync.parse.ParsedTxn;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IciciSmsParserTest {

    @Test
    void parsesDrTransactionFormat() {
        RawMessage message = new RawMessage(
                "m-test-icici-v2",
                "sms",
                IciciSmsParser.SENDER,
                OffsetDateTime.parse("2026-08-05T07:51:00+05:30"),
                "dev-test",
                "ICICI Bank Acct XX9075 Dr INR 25 on 05-Aug-2026 07:51; "
                        + "UPI/VEGETABLE VENDOR ref no 216695933242. "
                        + "BalAvl Rs 63,239.45"
        );

        Optional<ParsedTxn> result = new IciciSmsParser().parse(message);

        assertTrue(result.isPresent());

        ParsedTxn txn = result.get();

        assertEquals("9075", txn.accountLast4());
        assertEquals(Direction.DEBIT, txn.direction());
        assertEquals(new BigDecimal("25.00"), txn.amount());
        assertEquals("UPI/VEGETABLE VENDOR", txn.merchant());
        assertEquals(new BigDecimal("63239.45"), txn.statedBalance());
    }

    @Test
    void parsesCrTransactionFormat() {
        RawMessage message = new RawMessage(
                "m-test-icici-v2-credit",
                "sms",
                IciciSmsParser.SENDER,
                OffsetDateTime.parse("2026-08-01T14:22:00+05:30"),
                "dev-test",
                "ICICI Bank Acct XX9075 Cr INR 5000.00 on 01-Aug-2026 14:22; "
                        + "IMPS/P2A/PARAG KAPOOR ref no 908129880743. "
                        + "BalAvl Rs 52,928.02"
        );

        Optional<ParsedTxn> result = new IciciSmsParser().parse(message);

        assertTrue(result.isPresent());

        ParsedTxn txn = result.get();

        assertEquals("9075", txn.accountLast4());
        assertEquals(Direction.CREDIT, txn.direction());
        assertEquals(new BigDecimal("5000.00"), txn.amount());
        assertEquals("IMPS/P2A/PARAG KAPOOR", txn.merchant());
        assertEquals(new BigDecimal("52928.02"), txn.statedBalance());
    }
}