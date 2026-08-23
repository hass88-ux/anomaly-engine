package com.hassan.anomaly;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AccountHistory {

    private final Map<String, List<TransactionView>> byAccount = new HashMap<>();

    public void add(TransactionView txn) {
        byAccount.computeIfAbsent(txn.accountId(), k -> new ArrayList<>()).add(txn);
    }

    public List<TransactionView> forAccount(String accountId) {
        return Collections.unmodifiableList(
                byAccount.getOrDefault(accountId, List.of()));
    }

    public List<TransactionView> since(String accountId, Instant cutoff) {
        List<TransactionView> all = byAccount.getOrDefault(accountId, List.of());
        int from = all.size();
        while (from > 0 && all.get(from - 1).occurredAt().isAfter(cutoff)) {
            from--;
        }
        return Collections.unmodifiableList(all.subList(from, all.size()));
    }

    public Optional<TransactionView> mostRecent(String accountId) {
        List<TransactionView> all = byAccount.getOrDefault(accountId, List.of());
        return all.isEmpty()
                ? Optional.empty()
                : Optional.of(all.get(all.size() - 1));
    }
}