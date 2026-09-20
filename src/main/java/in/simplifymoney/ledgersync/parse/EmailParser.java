package in.simplifymoney.ledgersync.parse;

import in.simplifymoney.ledgersync.model.Direction;
import in.simplifymoney.ledgersync.model.RawMessage;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses bank transaction alert emails.
 *
 * Both HDFC and ICICI emails in the corpus use the same transaction-alert
 * structure, so one parser handles both formats.
 */
public final class EmailParser implements MessageParser {

    private static final Pattern TRANSACTION = Pattern.compile(
            "Your account ending (?<account>\\d{4}) "
                    + "has been (?<direction>debited|credited) with "
                    + "(?:Rs\\.?|INR)\\s*(?<amount>[0-9,]+(?:\\.[0-9]{2})?)\\."
    );

    private static final Pattern MERCHANT = Pattern.compile(
            "Merchant / Remarks:\\s*(?<merchant>[^\\r\\n]+)"
    );

    private static final Pattern EMAIL_DATE = Pattern.compile(
            "(?m)^Date:\\s*(?<date>[^\\r\\n]+)"
    );

    private static final DateTimeFormatter EMAIL_DATE_FORMAT =
            DateTimeFormatter.ofPattern(
                    "EEE, dd MMM yyyy HH:mm:ss Z",
                    Locale.ENGLISH
            );

    @Override
    public boolean supports(RawMessage m) {
        return "email".equals(m.channel());
    }

    @Override
    public Optional<ParsedTxn> parse(RawMessage m) {

        Matcher transaction = TRANSACTION.matcher(m.body());
        Matcher merchant = MERCHANT.matcher(m.body());
        Matcher date = EMAIL_DATE.matcher(m.body());

        if (!transaction.find()
                || !merchant.find()
                || !date.find()) {
            return Optional.empty();
        }

        String account = transaction.group("account");

        Direction direction =
                "debited".equals(transaction.group("direction"))
                        ? Direction.DEBIT
                        : Direction.CREDIT;

        BigDecimal amount = new BigDecimal(
                transaction.group("amount").replace(",", "")
        ).setScale(2);

        String merchantName =
                merchant.group("merchant").trim();

        OffsetDateTime occurredAt =
                OffsetDateTime.parse(
                        date.group("date").trim(),
                        EMAIL_DATE_FORMAT
                );

        return Optional.of(new ParsedTxn(
                account,
                occurredAt,
                direction,
                amount,
                merchantName,
                null,
                m.messageId()
        ));
    }
}