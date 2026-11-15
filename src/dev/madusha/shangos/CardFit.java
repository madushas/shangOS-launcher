package dev.madusha.shangos;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * How the cards of one Home page take the row: spare width (from a card that is off, say) goes to
 * the cards that can grow, up to a cap, and the group is centred. Pure, so it is unit tested.
 */
final class CardFit {
    private CardFit() {}

    /**
     * @param ids the cards of the page, in order
     * @param preferred each card's usual width
     * @param growable whether a card may stretch
     * @param row the width of the row; {@code gap} between cards
     * @param maxGrow the most a growable card may become
     * @return each card's width (the total is at most {@code row})
     */
    static float[] widths(
            List<String> ids,
            ToDoubleFunction<String> preferred,
            Predicate<String> growable,
            float row,
            float gap,
            float maxGrow) {
        int n = ids.size();
        float[] w = new float[n];
        float sum = 0;
        int growing = 0;
        for (int i = 0; i < n; i++) {
            w[i] = (float) preferred.applyAsDouble(ids.get(i));
            sum += w[i];
            if (growable.test(ids.get(i))) growing++;
        }
        float spare = row - sum - gap * Math.max(0, n - 1);
        if (growing > 0 && spare > 0)
            for (int i = 0; i < n; i++)
                if (growable.test(ids.get(i))) w[i] = Math.min(maxGrow, w[i] + spare / growing);
        return w;
    }

    /** The left edge of a group of cards of these widths, centred in the row starting at {@code x}. */
    static float centred(float[] widths, float x, float row, float gap) {
        float total = gap * Math.max(0, widths.length - 1);
        for (float v : widths) total += v;
        return x + (row - total) / 2;
    }
}
