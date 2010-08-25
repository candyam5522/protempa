package org.protempa.proposition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.RandomAccess;
import org.protempa.proposition.comparator.MaxFinishTemporalPropositionComparator;
import org.protempa.proposition.comparator.TemporalPropositionIntervalComparator;

import org.protempa.proposition.value.Granularity;
import org.protempa.proposition.value.Unit;

/**
 * @author Andrew Post
 */
public class PropositionUtil {

    private PropositionUtil() {
    }

    /**
     * Filters a list of temporal propositions by time.
     *
     * @param params
     *            a <code>List</code> of propositions.
     * @param minValid
     * @param maxValid
     * @return
     */
    public static <T extends TemporalProposition> List<T> getView(
            List<T> params, Long minValid, Long maxValid) {
        if (params != null) {
            if (minValid == null && maxValid == null) {
                return params;
            } else {
                int min = minValid != null ? binarySearchMinStart(params,
                        minValid.longValue()) : 0;
                int max = maxValid != null ? binarySearchMaxFinish(params,
                        maxValid.longValue()) : params.size();
                return params.subList(min, max);
            }
        } else {
            return null;
        }
    }

    public static <T extends TemporalProposition> Map<String, List<T>> getView(
            Map<String, List<T>> paramsByKey, Long minValid, Long maxValid) {
        if (paramsByKey != null) {
            Map<String, List<T>> result = new HashMap<String, List<T>>();
            for (Map.Entry<String, List<T>> me : paramsByKey.entrySet()) {
                result.put(me.getKey(), getView(me.getValue(), minValid,
                        maxValid));
            }
            return result;
        } else {
            return null;
        }
    }

    /**
     * Divides a list of propositions up by id.
     *
     * @param propositions
     *            a <code>List</code> of <code>Proposition</code>s.
     * @return a Map of id <code>String</code> -> <code>List</code>, with
     *         propositions in the same order as they were found in the
     *         argument.
     */
    public static <T extends Proposition> Map<String, List<T>> createPropositionMap(
            List<T> propositions) {
        Map<String, List<T>> result = new HashMap<String, List<T>>();

        if (propositions != null) {
            for (T prop : propositions) {
                String propId = prop.getId();

                List<T> ts = null;
                if (result.containsKey(propId)) {
                    ts = result.get(propId);
                } else {
                    ts = new ArrayList<T>();
                    result.put(propId, ts);
                }
                ts.add(prop);
            }
        }

        return result;
    }

    /**
     * Binary search for a primitive parameter by timestamp, optimized for when
     * the parameters are stored in a list that implements
     * <code>java.util.RandomAccess</code>.
     *
     * @param list
     *            a <code>List</code> of <code>PrimitiveParameter</code>
     *            objects all with the same paramId, cannot be <code>null</code>.
     * @param tstamp
     *            the timestamp we're interested in finding.
     * @return a <code>PrimitiveParameter</code>, or <code>null</code> if
     *         not found.
     */
    private static int minStartIndexedBinarySearch(
            List<? extends TemporalProposition> list, long tstamp) {
        int low = 0;
        int high = list.size() - 1;

        while (low <= high) {
            /*
             * We use >>> instead of >> or /2 to avoid overflow. Sun's
             * implementation of binary search actually doesn't do this (bug
             * #5045582).
             */
            int mid = (low + high) >>> 1;
            TemporalProposition midVal = list.get(mid);
            Long minStart = midVal.getInterval().getMinimumStart();
            int cmp = minStart != null ? minStart.compareTo(tstamp) : -1;
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return low;
    }

    /**
     * Binary search for a primitive parameter by timestamp, optimized for when
     * the parameters are stored in a list that does not implement
     * <code>java.util.RandomAccess</code>.
     *
     * @param list
     *            a <code>List</code> of <code>PrimitiveParameter</code>
     *            objects all with the same paramId, cannot be <code>null</code>.
     * @param tstamp
     *            the timestamp we're interested in finding.
     * @return a <code>PrimitiveParameter</code>, or <code>null</code> if
     *         not found.
     */
    private static <T extends TemporalProposition> int minStartIteratorBinarySearch(
            List<T> list, long tstamp) {
        int low = 0;
        int high = list.size() - 1;
        ListIterator<T> i = list.listIterator();

        while (low <= high) {
            /*
             * We use >>> instead of >> or /2 to avoid overflow. Sun's
             * implementation of binary search actually doesn't do this (bug
             * #5045582).
             */
            int mid = (low + high) >>> 1;
            TemporalProposition midVal = iteratorBinarySearchGet(i, mid);
            Long maxStart = midVal.getInterval().getMinimumStart();
            int cmp = maxStart != null ? maxStart.compareTo(tstamp) : 1;
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return low;
    }

    /**
     * Binary search for a primitive parameter by timestamp.
     *
     * @param list
     *            a <code>List</code> of <code>PrimitiveParameter</code>
     *            objects all with the same paramId, cannot be <code>null</code>.
     * @param tstamp
     *            the timestamp we're interested in finding.
     * @return a <code>PrimitiveParameter</code>, or null if not found.
     */
    private static int binarySearchMinStart(
            List<? extends TemporalProposition> params, long timestamp) {
        /*
         * The conditions for using index versus iterator are grabbed from the
         * JDK source code.
         */
        if (params.size() < 5000 || params instanceof RandomAccess) {
            return minStartIndexedBinarySearch(params, timestamp);
        } else {
            return minStartIteratorBinarySearch(params, timestamp);
        }
    }

    /**
     * Binary search for a primitive parameter by timestamp, optimized for when
     * the parameters are stored in a list that implements
     * <code>java.util.RandomAccess</code>.
     *
     * @param list
     *            a <code>List</code> of <code>PrimitiveParameter</code>
     *            objects all with the same paramId, cannot be <code>null</code>.
     * @param tstamp
     *            the timestamp we're interested in finding.
     * @return a <code>PrimitiveParameter</code>, or <code>null</code> if
     *         not found.
     */
    private static int maxFinishIndexedBinarySearch(
            List<? extends TemporalProposition> list, long tstamp) {
        int low = 0;
        int high = list.size() - 1;

        while (low <= high) {
            /*
             * We use >>> instead of >> or /2 to avoid overflow. Sun's
             * implementation of binary search actually doesn't do this (bug
             * #5045582).
             */
            int mid = (low + high) >>> 1;
            TemporalProposition midVal = list.get(mid);
            Long maxFinish = midVal.getInterval().getMaximumFinish();
            int cmp = maxFinish != null ? maxFinish.compareTo(tstamp) : 1;
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid + 1;
            }
        }

        return high + 1;
    }

    /**
     * Binary search for a primitive parameter by timestamp, optimized for when
     * the parameters are stored in a list that does not implement
     * <code>java.util.RandomAccess</code>.
     *
     * @param list
     *            a <code>List</code> of <code>PrimitiveParameter</code>
     *            objects all with the same paramId, cannot be <code>null</code>.
     * @param tstamp
     *            the timestamp we're interested in finding.
     * @return a <code>PrimitiveParameter</code>, or <code>null</code> if
     *         not found.
     */
    private static <T extends TemporalProposition> int maxFinishIteratorBinarySearch(
            List<T> list, long tstamp) {
        int low = 0;
        int high = list.size() - 1;
        ListIterator<T> i = list.listIterator();

        while (low <= high) {
            /*
             * We use >>> instead of >> or /2 to avoid overflow. Sun's
             * implementation of binary search actually doesn't do this (bug
             * #5045582).
             */
            int mid = (low + high) >>> 1;
            T midVal = iteratorBinarySearchGet(i, mid);
            Long maxFinish = midVal.getInterval().getMaximumFinish();
            int cmp = maxFinish != null ? maxFinish.compareTo(tstamp) : 1;
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid + 1;
            }
        }
        return high + 1;
    }

    /**
     * Binary search for a primitive parameter by timestamp.
     *
     * @param list
     *            a <code>List</code> of <code>PrimitiveParameter</code>
     *            objects all with the same paramId, cannot be <code>null</code>.
     * @param tstamp
     *            the timestamp we're interested in finding.
     * @return a <code>PrimitiveParameter</code>, or null if not found.
     */
    private static int binarySearchMaxFinish(
            List<? extends TemporalProposition> params, long timestamp) {
        /*
         * The conditions for using index versus iterator are grabbed from the
         * JDK source code.
         */
        if (params.size() < 5000 || params instanceof RandomAccess) {
            return maxFinishIndexedBinarySearch(params, timestamp);
        } else {
            return maxFinishIteratorBinarySearch(params, timestamp);
        }
    }

    /*
     * The following four methods implement binary search for a primitive
     * parameter with a given timestamp. We don't use the JDK's built-in binary
     * search because we'd have to generate a new list containing just the
     * parameters' timestamps, search that for the timestamp of interest,
     * retrieve the index and grab the primitive parameter with that index. Just
     * seemed more efficient and not much more complicated to write our own.
     */
    /**
     * Binary search for a primitive parameter by timestamp, optimized for when
     * the parameters are stored in a list that implements
     * <code>java.util.RandomAccess</code>.
     *
     * @param list
     *            a <code>List</code> of <code>PrimitiveParameter</code>
     *            objects all with the same paramId, cannot be <code>null</code>.
     * @param tstamp
     *            the timestamp we're interested in finding.
     * @return a <code>PrimitiveParameter</code>, or <code>null</code> if
     *         not found.
     */
    private static PrimitiveParameter indexedBinarySearch(
            List<PrimitiveParameter> list, long tstamp) {
        int low = 0;
        int high = list.size() - 1;

        while (low <= high) {
            /*
             * We use >>> instead of >> or /2 to avoid overflow. Sun's
             * implementation of binary search actually doesn't do this (bug
             * #5045582).
             */
            int mid = (low + high) >>> 1;
            PrimitiveParameter midVal = list.get(mid);
            long cmp = midVal.getTimestamp() - tstamp;

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return midVal;
            }
        }
        return null;
    }

    /**
     * Binary search for a primitive parameter by timestamp, optimized for when
     * the parameters are stored in a list that does not implement
     * <code>java.util.RandomAccess</code>.
     *
     * @param list
     *            a <code>List</code> of <code>PrimitiveParameter</code>
     *            objects all with the same paramId, cannot be <code>null</code>.
     * @param tstamp
     *            the timestamp we're interested in finding.
     * @return a <code>PrimitiveParameter</code>, or <code>null</code> if
     *         not found.
     */
    private static <U extends Unit> PrimitiveParameter iteratorBinarySearch(
            List<PrimitiveParameter> list, long tstamp) {
        int low = 0;
        int high = list.size() - 1;
        ListIterator<PrimitiveParameter> i = list.listIterator();

        while (low <= high) {
            /*
             * We use >>> instead of >> or /2 to avoid overflow. Sun's
             * implementation of binary search actually doesn't do this (bug
             * #5045582).
             */
            int mid = (low + high) >>> 1;
            PrimitiveParameter midVal = iteratorBinarySearchGet(i, mid);
            long cmp = midVal.getTimestamp() - tstamp;

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return midVal;
            }
        }
        return null;
    }

    /**
     * Gets the ith element from a list by repositioning the specified list's
     * <code>ListIterator</code>.
     *
     * @param i
     *            a <code>ListIterator</code>.
     * @param index
     *            an index.
     */
    private static <T> T iteratorBinarySearchGet(ListIterator<T> i, int index) {
        T obj = null;
        int pos = i.nextIndex();
        if (pos <= index) {
            do {
                obj = i.next();
            } while (pos++ < index);
        } else {
            do {
                obj = i.previous();
            } while (--pos > index);
        }
        return obj;
    }

    /**
     * Binary search for a primitive parameter by timestamp.
     *
     * @param list
     *            a <code>List</code> of <code>PrimitiveParameter</code>
     *            objects all with the same paramId, cannot be <code>null</code>.
     * @param tstamp
     *            the timestamp we're interested in finding.
     * @return a <code>PrimitiveParameter</code>, or null if not found.
     */
    public static PrimitiveParameter binarySearch(
            List<PrimitiveParameter> params, long timestamp) {
        /*
         * The conditions for using index versus iterator are grabbed from the
         * JDK source code.
         */
        if (params.size() < 5000 || params instanceof RandomAccess) {
            return indexedBinarySearch(params, timestamp);
        } else {
            return iteratorBinarySearch(params, timestamp);
        }
    }

    public static Interval createInterval(Long minStart, Long maxStart,
            Granularity startGran, Long minFinish, Long maxFinish,
            Granularity finishGran) {
        if (minStart == null || maxStart == null || minFinish == null
                || maxFinish == null) {
            return new DefaultInterval(minStart, maxStart, startGran,
                    minFinish, maxFinish, finishGran, null, null, null);
        } else {
            return new SimpleAbstractParameterInterval(minStart, maxStart,
                    startGran, minFinish, maxFinish, finishGran);
        }
    }

    static final Comparator<TemporalProposition> TEMP_PROP_COMP =
            new TemporalPropositionIntervalComparator();

    static final Comparator<TemporalProposition> MAX_FINISH_COMP =
            new MaxFinishTemporalPropositionComparator();
}
