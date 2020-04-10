package gov.cms.dpc.common.hibernate.validator;

import gov.cms.dpc.common.annotations.NoHtml;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class NoHtmlValidator implements ConstraintValidator<NoHtml, String> {

    public static final String VALIDATION_MESSAGE = "Cannot contain HTML";

    @Override
    public void initialize(NoHtml noHtml) {
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        String safe = Jsoup.clean(s, Whitelist.none());
        return safe.equals(s);
    }
}
