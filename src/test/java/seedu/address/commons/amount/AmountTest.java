package seedu.address.commons.amount;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.Test;

import seedu.address.commons.core.amount.Amount;

public class AmountTest {

    @Test
    public void constructor_illegalArguments_throwsExceptions() {

        assertThrows(IllegalArgumentException.class, () -> new Amount("111.111"));
        assertThrows(IllegalArgumentException.class, () -> new Amount("123412341234123.11"));
        assertThrows(IllegalArgumentException.class, () -> new Amount(".12"));
        assertThrows(IllegalArgumentException.class, () -> new Amount("123."));
        assertThrows(IllegalArgumentException.class, () -> new Amount("-.45"));
    }

    @Test
    public void equals() {
        Amount amount1a = new Amount("111");
        Amount amount1b = new Amount("111");
        Amount amount1c = new Amount("111.0");
        Amount amount1d = new Amount("111.00");
        Amount amount1e = new Amount("111.00");

        Amount amount2 = new Amount("111.55");

        assertTrue(amount1a.equals(amount1a));
        assertTrue(amount1a.equals(amount1b));
        assertTrue(amount1b.equals(amount1c));
        assertTrue(amount1c.equals(amount1d));
        assertTrue(amount1d.equals(amount1e));

        assertFalse(amount1a.equals(amount2));
    }
}
