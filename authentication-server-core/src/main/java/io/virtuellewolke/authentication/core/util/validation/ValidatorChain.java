package io.virtuellewolke.authentication.core.util.validation;

import java.util.ArrayList;
import java.util.List;

public class ValidatorChain<T> implements Validator<T> {

    private final List<Validator<T>> validators = new ArrayList<>();

    public void addValidator(Validator<T> validator) {
        this.validators.add(validator);
    }

    @Override
    public boolean isValid(T argument) {
        boolean isValid = false;

        for (Validator<T> validator: validators)  {
            if (validator.isValid(argument)) {
                isValid = true;
                break;
            }
        }

        return isValid;
    }
}
