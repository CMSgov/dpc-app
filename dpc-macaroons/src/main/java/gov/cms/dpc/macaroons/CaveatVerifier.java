package gov.cms.dpc.macaroons;

import java.util.Optional;

/**
 * Interface for verifying the correctness of a given {@link MacaroonCondition}
 * When implementing, the user should make sure that the {@link CaveatVerifier} returns successfully
 * when given a caveat that it does not recognize.
 * <p>
 * The return value is an error message generated when the caveat FAILS verification.
 * {@link Optional#empty()} is used for a successful verification
 */
@FunctionalInterface
public interface CaveatVerifier {

    /**
     * Verify that the given {@link MacaroonCondition} is correct.
     * Should only return an optional {@link String} when the caveat FAILS verification
     *
     * @param caveat -{@link MacaroonCondition} to verify
     * @return - {@link Optional} {@link String} of verification error message
     */
    Optional<String> check(MacaroonCondition caveat);
}
