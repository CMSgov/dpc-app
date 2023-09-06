package gov.cms.dpc.fhir.helpers;


import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class DPCCollectors {
    private DPCCollectors() {}

    /**
     * A collector that returns a single {@link Optional}.  Converts the collection operated on to a list, and if there
     * is one and only one element, wraps it in an {@link Optional} and returns it.  If the collection is empty or has
     * more than one element, an empty {@link Optional} is returned.
     * @return {@link Optional}
     * @param <T>
     */
     public static <T> Collector<T, ?, Optional<T>> singleOrNone() {
        return Collectors.collectingAndThen(
                Collectors.toList(),
                list -> list.size() == 1 ? Optional.of(list.get(0)) : Optional.empty()
        );
    }
}
