package io.gnupinguin.nevis.wealthtech.service.ai;

public enum AiProviderOperation {
    EMBEDDING_SEARCH("embedding_search"),
    DOCUMENT_CHUNKING("document_chunking"),
    SUMMARY_GENERATION("summary_generation");

    private final String label;

    AiProviderOperation(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
