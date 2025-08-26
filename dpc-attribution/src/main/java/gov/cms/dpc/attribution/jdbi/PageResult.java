package gov.cms.dpc.attribution.jdbi;

import java.util.List;

public class PageResult<T> {
    private final List<T> results;
    private final boolean hasNext;

    public PageResult(List<T> results, boolean hasNext) {
        this.results = results;
        this.hasNext = hasNext;
    }

    public List<T> getResults() { return results; }
    public boolean hasNext() { return hasNext; }
}

