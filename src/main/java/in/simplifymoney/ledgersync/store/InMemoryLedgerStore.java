package in.simplifymoney.ledgersync.store;

import in.simplifymoney.ledgersync.model.NormalizedTxn;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Used by SelfCheck and by tests. Keeps everything it is given. */
public final class InMemoryLedgerStore implements LedgerStore {

    private final List<NormalizedTxn> rows = new ArrayList<>();

    @Override public void save(NormalizedTxn txn) { rows.add(txn); }

    @Override public List<NormalizedTxn> all() { return Collections.unmodifiableList(rows); }

    @Override public long count() { return rows.size(); }

    @Override
    public void saveOrMerge(NormalizedTxn txn) {
        for (int i = 0; i < rows.size(); i++) {
            NormalizedTxn existing = rows.get(i);

            if (sameTransaction(existing, txn)) {
                List<String> sourceIds = new ArrayList<>();
                sourceIds.addAll(existing.sourceMessageIds());
                sourceIds.addAll(txn.sourceMessageIds());

                sourceIds = sourceIds.stream()
                        .distinct()
                        .sorted()
                        .toList();

                rows.set(i, new NormalizedTxn(
                        existing.accountLast4(),
                        existing.occurredAt(),
                        existing.direction(),
                        existing.amount(),
                        existing.category(),
                        existing.merchant(),
                        sourceIds
                ));

                return;
            }
        }

        rows.add(txn);
    }

    private boolean sameTransaction(NormalizedTxn a, NormalizedTxn b) {
        return a.accountLast4().equals(b.accountLast4())
                && a.occurredAt().equals(b.occurredAt())
                && a.direction() == b.direction()
                && a.amount().compareTo(b.amount()) == 0;
    }
}
