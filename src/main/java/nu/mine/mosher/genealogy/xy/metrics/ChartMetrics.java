/*
    Copyright © 2026, Christopher Alan Mosher, New York, New York, USA, <cmosher01@gmail.com>.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package nu.mine.mosher.genealogy.xy.metrics;

import nu.mine.mosher.genealogy.IndexedPerson;
import nu.mine.mosher.genealogy.xy.Indi;
import nu.mine.mosher.genealogy.xy.shape.Point;

import java.util.*;

public final class ChartMetrics {
    private static final double maxVisualWidthAsFractionOfMedianMinimumNeighbor = 0.8D;

    private final double medianMinimumDistanceToNeighbor;
    private final double scaleFactor;

    public ChartMetrics(final List<IndexedPerson> indis, final FontBasedMetrics metricsFont) {
        final var medianMinimumDistanceToNeighbor = calculateMedianMinimumDistanceToNeighbor(indis);
        final var scaleFactor = calculateScaleFactor(medianMinimumDistanceToNeighbor, metricsFont.maxWidthPlaque());

        this.medianMinimumDistanceToNeighbor = medianMinimumDistanceToNeighbor;
        this.scaleFactor = scaleFactor;
    }

    public double medianMinimumDistanceToNeighborOriginal() {
        return this.medianMinimumDistanceToNeighbor;
    }

    public double medianMinimumDistanceToNeighborScaled() {
        return this.medianMinimumDistanceToNeighbor * this.scaleFactor;
    }

    public double scaleFactor() {
        return this.scaleFactor;
    }



    private static double calculateMedianMinimumDistanceToNeighbor(final List<IndexedPerson> indis) {
        final var points = indis.stream()
                .map(IndexedPerson::xy)
                .map(Indi::alwaysCoord)
                .filter(p -> !p.equals(Point.ZERO))
                .toList();

        final var minimums = calculateMinimumDistancesToNeighbors(points);
        return calculateMedian(minimums);
    }

    private static List<Double> calculateMinimumDistancesToNeighbors(final List<Point> points) {
        final var minimums = new ArrayList<Double>(points.size());
        for (int i = 0; i < points.size(); ++i) {
            double min = Double.POSITIVE_INFINITY;
            final var p0 = points.get(i);
            for (int j = 0; j < points.size(); ++j) {
                if (j != i) {
                    final var p1 = points.get(j);
                    final var d = p0.distance(p1);
                    if (Double.compare(d, min) < 0) {
                        min = d;
                    }
                }
            }
            minimums.add(min);
        }
        return minimums;
    }

    private static double calculateMedian(final List<Double> values){
        values.sort(null);
        final double ret;
        final int n = values.size();
        if (n == 0) {
            ret = 0.0D; // TODO
        } else if (n == 1) {
            ret = Double.POSITIVE_INFINITY; // TODO
        } else {
            final int h = n / 2;
            if (n % 2 == 1) {
                ret = values.get(h);
            } else {
                ret = (values.get(h-1) + values.get(h)) / 2.0D;
            }
        }
        return ret;
    }

    private static double calculateScaleFactor(double medianMinimumDistanceToNeighbor, double maxVisualWidthIdeal) {
        final var maxVisualWidthCurrent = maxVisualWidthAsFractionOfMedianMinimumNeighbor * medianMinimumDistanceToNeighbor;
        return maxVisualWidthIdeal / maxVisualWidthCurrent;
    }
}
