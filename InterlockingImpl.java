import java.util.*;

public class InterlockingImpl implements Interlocking {

    private static final Set<Integer> ALL_SECTIONS =
            new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11));
    private static final Set<Integer> FREIGHT_SECTIONS =
            new HashSet<>(Arrays.asList(3, 7, 11));
    private static final Set<Integer> PASSENGER_SECTIONS =
            new HashSet<>(Arrays.asList(1, 2, 4, 5, 6, 8, 9, 10));
    private static final Set<Integer> SB_ENTRIES =
            new HashSet<>(Arrays.asList(1, 3));
    private static final Set<Integer> SB_EXITS =
            new HashSet<>(Arrays.asList(4, 8, 9, 11));
    private static final Set<Integer> NB_ENTRIES =
            new HashSet<>(Arrays.asList(4, 9, 10, 11));
    private static final Set<Integer> NB_EXITS =
            new HashSet<>(Arrays.asList(2, 3));

    private static final Map<Integer, List<Integer>> NEXT = new HashMap<>();

    static {
        NEXT.put(1,  Arrays.asList(5));
        NEXT.put(2,  Arrays.asList(6));
        NEXT.put(3,  Arrays.asList(7));
        NEXT.put(4,  Arrays.asList(5));
        NEXT.put(5,  Arrays.asList(4, 8, 9, 1, 2));
        NEXT.put(6,  Arrays.asList(9, 10, 2));
        NEXT.put(7,  Arrays.asList(11, 3));
        NEXT.put(8,  Arrays.asList(5));
        NEXT.put(9,  Arrays.asList(5, 6));
        NEXT.put(10, Arrays.asList(6));
        NEXT.put(11, Arrays.asList(7));
    }

    private enum TrainType { PASSENGER, FREIGHT }

    private final Map<Integer, String>   sectionOccupant = new HashMap<>();
    private final Map<String, Integer>   trainSection    = new LinkedHashMap<>();
    private final Map<String, Integer>   trainDest       = new HashMap<>();
    private final Map<String, TrainType> trainType       = new HashMap<>();

    public InterlockingImpl() {
        for (int s : ALL_SECTIONS) sectionOccupant.put(s, null);
    }

    @Override
    public void addTrain(String trainName, int entryTrackSection, int destinationTrackSection)
            throws IllegalArgumentException, IllegalStateException {

        if (trainSection.containsKey(trainName))
            throw new IllegalArgumentException("Train name already in use: " + trainName);

        if (!ALL_SECTIONS.contains(entryTrackSection))
            throw new IllegalArgumentException("Entry section does not exist: " + entryTrackSection);

        if (!ALL_SECTIONS.contains(destinationTrackSection))
            throw new IllegalArgumentException("Destination section does not exist: " + destinationTrackSection);

        boolean isSB = SB_ENTRIES.contains(entryTrackSection);
        boolean isNB = NB_ENTRIES.contains(entryTrackSection);
        if (!isSB && !isNB)
            throw new IllegalArgumentException("Section " + entryTrackSection + " is not a valid entry point");

        boolean southbound = isSB;

        Set<Integer> validExits = southbound ? SB_EXITS : NB_EXITS;
        if (!validExits.contains(destinationTrackSection))
            throw new IllegalArgumentException(
                    "Section " + destinationTrackSection +
                            " is not a valid " + (southbound ? "southbound" : "northbound") + " exit");

        TrainType type = (entryTrackSection == 3 || entryTrackSection == 11)
                ? TrainType.FREIGHT : TrainType.PASSENGER;

        if (!hasPath(entryTrackSection, destinationTrackSection, type))
            throw new IllegalArgumentException("No valid path from section " +
                    entryTrackSection + " to section " + destinationTrackSection);

        if (sectionOccupant.get(entryTrackSection) != null)
            throw new IllegalStateException("Entry section " + entryTrackSection +
                    " is occupied by " + sectionOccupant.get(entryTrackSection));

        sectionOccupant.put(entryTrackSection, trainName);
        trainSection.put(trainName, entryTrackSection);
        trainDest.put(trainName, destinationTrackSection);
        trainType.put(trainName, type);
    }

    @Override
    public int moveTrains(String[] trainNames) throws IllegalArgumentException {
        for (String name : trainNames) {
            if (!trainSection.containsKey(name))
                throw new IllegalArgumentException("Unknown train: " + name);
            if (trainSection.get(name) == -1)
                throw new IllegalArgumentException("Train has already exited: " + name);
        }

        List<String> toMove = dedup(trainNames);

        Map<String, Integer> intended = new LinkedHashMap<>();
        for (String name : toMove) {
            int cur  = trainSection.get(name);
            int dest = trainDest.get(name);
            if (cur == dest) {
                intended.put(name, -1);
            } else {
                int next = chooseNext(name, cur, dest);
                if (next != -2)
                    intended.put(name, next);
            }
        }

        Map<String, Integer> confirmed = resolveConflicts(toMove, intended);

        int moved = 0;
        for (Map.Entry<String, Integer> entry : confirmed.entrySet()) {
            String name   = entry.getKey();
            int    target = entry.getValue();
            int    cur    = trainSection.get(name);

            sectionOccupant.put(cur, null);
            if (target == -1) {
                trainSection.put(name, -1);
            } else {
                sectionOccupant.put(target, name);
                trainSection.put(name, target);
            }
            moved++;
        }
        return moved;
    }

    @Override
    public String getSection(int trackSection) throws IllegalArgumentException {
        if (!ALL_SECTIONS.contains(trackSection))
            throw new IllegalArgumentException("Section does not exist: " + trackSection);
        return sectionOccupant.get(trackSection);
    }

    @Override
    public int getTrain(String trainName) throws IllegalArgumentException {
        if (!trainSection.containsKey(trainName))
            throw new IllegalArgumentException("Unknown train: " + trainName);
        return trainSection.get(trainName);
    }

    private boolean hasPath(int src, int dest, TrainType type) {
        if (src == dest) return true;
        Set<Integer>   visited = new HashSet<>();
        Queue<Integer> queue   = new LinkedList<>();
        queue.add(src);
        while (!queue.isEmpty()) {
            int cur = queue.poll();
            if (cur == dest) return true;
            if (!visited.add(cur)) continue;
            for (int nb : NEXT.getOrDefault(cur, Collections.emptyList()))
                if (typeAllowed(nb, type)) queue.add(nb);
        }
        return false;
    }

    private boolean typeAllowed(int section, TrainType type) {
        if (type == TrainType.PASSENGER && FREIGHT_SECTIONS.contains(section))  return false;
        if (type == TrainType.FREIGHT   && PASSENGER_SECTIONS.contains(section)) return false;
        return true;
    }

    private List<Integer> shortestPath(int src, int dest, TrainType type) {
        if (src == dest) return Collections.emptyList();
        Map<Integer, Integer> parent = new HashMap<>();
        Queue<Integer>        queue  = new LinkedList<>();
        queue.add(src);
        parent.put(src, -1);
        while (!queue.isEmpty()) {
            int cur = queue.poll();
            if (cur == dest) {
                List<Integer> path = new LinkedList<>();
                int c = dest;
                while (c != src) {
                    path.add(0, c);
                    c = parent.get(c);
                }
                return path;
            }
            for (int nb : NEXT.getOrDefault(cur, Collections.emptyList())) {
                if (!parent.containsKey(nb) && typeAllowed(nb, type)) {
                    parent.put(nb, cur);
                    queue.add(nb);
                }
            }
        }
        return Collections.emptyList();
    }

    private int chooseNext(String name, int cur, int dest) {
        TrainType type      = trainType.get(name);
        int       bestSec   = -2;
        int       bestLen   = Integer.MAX_VALUE;

        for (int candidate : NEXT.getOrDefault(cur, Collections.emptyList())) {
            if (!typeAllowed(candidate, type)) continue;
            if (sectionOccupant.get(candidate) != null) continue;
            if (candidate != dest && !hasPath(candidate, dest, type)) continue;

            int len = (candidate == dest) ? 0 : shortestPath(candidate, dest, type).size();
            if (len < bestLen) { bestLen = len; bestSec = candidate; }
        }
        return bestSec;
    }

    private Map<String, Integer> resolveConflicts(List<String> toMove,
                                                  Map<String, Integer> intended) {
        Map<String, Integer> result  = new LinkedHashMap<>();
        Set<Integer>         claimed = new HashSet<>();

        for (String name : toMove) {
            if (!intended.containsKey(name)) continue;
            int target = intended.get(name);

            if (target == -1) { result.put(name, -1); continue; }
            if (claimed.contains(target)) continue;

            int    myCur    = trainSection.get(name);
            String occupant = sectionOccupant.get(target);
            if (occupant != null && intended.containsKey(occupant)
                    && intended.get(occupant) == myCur) continue;

            result.put(name, target);
            claimed.add(target);
        }
        return result;
    }

    private List<String> dedup(String[] names) {
        List<String> result  = new ArrayList<>();
        Set<String>  seen    = new LinkedHashSet<>();
        for (String n : names) if (seen.add(n)) result.add(n);
        return result;
    }
}