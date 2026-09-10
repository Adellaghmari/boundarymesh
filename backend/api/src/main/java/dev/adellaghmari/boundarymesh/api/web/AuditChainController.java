package dev.adellaghmari.boundarymesh.api.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.adellaghmari.boundarymesh.application.audit.AuditChainVerifier;
import dev.adellaghmari.boundarymesh.application.audit.AuditRepository;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainId;
import dev.adellaghmari.boundarymesh.domain.audit.AuditEntry;
import dev.adellaghmari.boundarymesh.domain.audit.AuditVerificationResult;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/audit/chains", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuditChainController {

    public static final int MAX_ENTRIES = 100;

    private final AuditRepository repository;
    private final AuditChainVerifier verifier;

    public AuditChainController(AuditRepository repository, AuditChainVerifier verifier) {
        this.repository = repository;
        this.verifier = verifier;
    }

    @GetMapping
    public ChainsView list() {
        return new ChainsView(
                "LOCAL / SYNTHETIC SESSION DATA. One hash chain per integration. Not Firestore.",
                repository.chainIds().stream().map(AuditChainId::value).sorted().toList());
    }

    @GetMapping("/{integrationId}")
    public ChainView chain(@PathVariable String integrationId) {
        AuditChainId chainId = new AuditChainId(integrationId);
        List<AuditEntry> all = repository.entries(chainId, Integer.MAX_VALUE);
        List<AuditEntry> window = all.size() <= MAX_ENTRIES ? all : all.subList(all.size() - MAX_ENTRIES, all.size());
        return ChainView.from(chainId, window, verifier.verify(chainId, all));
    }

    @GetMapping("/{integrationId}/verify")
    public VerifyView verify(@PathVariable String integrationId) {
        AuditChainId chainId = new AuditChainId(integrationId);
        List<AuditEntry> entries = repository.entries(chainId, Integer.MAX_VALUE);
        return VerifyView.from(verifier.verify(chainId, entries));
    }

    public record ChainsView(String banner, List<String> chains) {}

    public record ChainView(
            String banner,
            @JsonProperty("chain_id") String chainId,
            @JsonProperty("entry_count") int entryCount,
            String continuity,
            List<EntryView> entries) {

        static ChainView from(AuditChainId chainId, List<AuditEntry> entries, AuditVerificationResult verification) {
            return new ChainView(
                    "LOCAL / SYNTHETIC SESSION DATA. Tamper-evident hash chain. Not immutable. Not Firestore.",
                    chainId.value(),
                    entries.size(),
                    verification.status().name(),
                    entries.stream().map(EntryView::from).toList());
        }
    }

    public record EntryView(
            long sequence,
            @JsonProperty("occurred_at") String occurredAt,
            @JsonProperty("event_type") String eventType,
            Map<String, String> attributes,
            @JsonProperty("previous_hash") String previousHash,
            @JsonProperty("previous_hash_short") String previousHashShort,
            @JsonProperty("current_hash") String currentHash,
            @JsonProperty("current_hash_short") String currentHashShort) {

        static EntryView from(AuditEntry entry) {
            return new EntryView(
                    entry.sequence(),
                    entry.occurredAt().toString(),
                    entry.eventType().name(),
                    entry.attributes(),
                    entry.previousHash().hex(),
                    entry.previousHash().shortened(),
                    entry.currentHash().hex(),
                    entry.currentHash().shortened());
        }
    }

    public record VerifyView(
            String banner,
            @JsonProperty("chain_id") String chainId,
            String status,
            @JsonProperty("entries_checked") long entriesChecked,
            @JsonProperty("break_sequence") Long breakSequence,
            String explanation) {

        static VerifyView from(AuditVerificationResult result) {
            return new VerifyView(
                    "Verification is computed by Java SHA-256 over RFC 8785 canonical bytes.",
                    result.chainId().value(),
                    result.status().name(),
                    result.entriesChecked(),
                    result.breakSequence(),
                    result.explanation());
        }
    }
}
