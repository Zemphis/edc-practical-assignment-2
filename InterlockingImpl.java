import java.util.*;

public class InterlockingImpl implements Interlocking {

    private static final Set<Integer> ALL_SECTIONS =
            new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11));

    private static final Set<Integer> SB_ENTRIES =
            new HashSet<>(Arrays.asList(1, 3));
    private static final Set<Integer> SB_EXITS =
            new HashSet<>(Arrays.asList(4, 8, 9, 11));
    private static final Set<Integer> NB_ENTRIES =
            new HashSet<>(Arrays.asList(4, 9, 10, 11));
    private static final Set<Integer> NB_EXITS =
            new HashSet<>(Arrays.asList(2, 3));

    private static final Map<Integer, List<Integer>> ADJ = new HashMap<>();

    static {
        ADJ.put(1,  Arrays.asList(5));
        ADJ.put(2,  Arrays.asList(6));
        ADJ.put(3,  Arrays.asList(7));
        ADJ.put(4,  Arrays.asList(5));
        ADJ.put(5,  Arrays.asList(4, 8, 9, 1, 2));
        ADJ.put(6,  Arrays.asList(9, 10, 2));
        ADJ.put(7,  Arrays.asList(11, 3));
        ADJ.put(8,  Arrays.asList(5));
        ADJ.put(9,  Arrays.asList(5, 6));
        ADJ.put(10, Arrays.asList(6));
        ADJ.put(11, Arrays.asList(7));
    }

    private final Map<Integer, String> sectionOccupant = new HashMap<>();
    private final Map<String, Integer> trainSection = new LinkedHashMap<>();
    private final Map<String, Integer> trainDestination = new HashMap<>();

    public InterlockingImpl() {
        for (int s : ALL_SECTIONS) sectionOccupant.put(s, null);
    }

    @Override
    public void addTrain(String trainName, int entryTrackSection, int destinationTrackSection)
            throws IllegalArgumentException, IllegalStateException {

        if (trainSection.containsKey(trainName))
            throw new IllegalArgumentException("Name already in use: " + trainName);

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
            throw new IllegalArgumentException("Section " + destinationTrackSection +
                            " is not a valid " + (southbound ? "southbound" : "northbound") + " exit");

        if (!hasPath(entryTrackSection, destinationTrackSection))
            throw new IllegalArgumentException("No valid path from section " + entryTrackSection +
                            " to section " + destinationTrackSection);

        if (sectionOccupant.get(entryTrackSection) != null)
            throw new IllegalStateException("Entry section " + entryTrackSection +
                            " is occupied by " + sectionOccupant.get(entryTrackSection));

        sectionOccupant.put(entryTrackSection, trainName);
        trainSection.put(trainName, entryTrackSection);
        trainDestination.put(trainName, destinationTrackSection);
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
            int cur = trainSection.get(name);
            int dest = trainDestination.get(name);
            if (cur == dest) {
                intended.put(name, -1);
            } else {
                int next = chooseNext(cur, dest);
                if (next != -2)
                    intended.put(name, next);
            }
        }

        Map<String, Integer> confirmed = resolveConflicts(toMove, intended);

        // apply
        int moved = 0;
        for (Map.Entry<String, Integer> e : confirmed.entrySet()) {
            String name = e.getKey();
            int target = e.getValue();
            int cur = trainSection.get(name);

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
            throw new IllegalArgumentException(
                    "Section does not exist: " + trackSection);
        return sectionOccupant.get(trackSection);
    }

    @Override
    public int getTrain(String trainName) throws IllegalArgumentException {
        if (!trainSection.containsKey(trainName))
            throw new IllegalArgumentException("Unknown train: " + trainName);
        return trainSection.get(trainName);

    private boolean hasPath(int src, int dst) {
        if (src == dst) return true;
        Set<Integer>   visited = new HashSet<>();
        Queue<Integer> queue   = new LinkedList<>();
        queue.add(src);
        while (!queue.isEmpty()) {
            int cur = queue.poll();
            if (cur == dst) return true;
            if (!visited.add(cur)) continue;
            for (int nb : ADJ.getOrDefault(cur, Collections.emptyList()))
                queue.add(nb);
        }
        return false;
    }

    private int bfsDist(int src, int dst) {
        if (src == dst) return 0;
        Map<Integer, Integer> dist = new HashMap<>();
        Queue<Integer>        queue = new LinkedList<>();
        queue.add(src);
        dist.put(src, 0);
        while (!queue.isEmpty()) {
            int cur = queue.poll();
            int d   = dist.get(cur);
            for (int nb : ADJ.getOrDefault(cur, Collections.emptyList())) {
                if (!dist.containsKey(nb)) {
                    dist.put(nb, d + 1);
                    if (nb == dst) return d + 1;
                    queue.add(nb);
                }
            }
        }
        return Integer.MAX_VALUE;
    }

    private int chooseNext(int cur, int dest) {
        int bestSec = -2;
        int bestDist = Integer.MAX_VALUE;

        for (int candidate : ADJ.getOrDefault(cur, Collections.emptyList())) {
            // Must be unoccupied
            if (sectionOccupant.get(candidate) != null) continue;
            int d = (candidate == dest) ? 0 : bfsDist(candidate, dest);
            if (d == Integer.MAX_VALUE) continue;
            if (d < bestDist) {
                bestDist = d;
                bestSec  = candidate;
            }
        }
        return bestSec;
    }
    
    private Map<String, Integer> resolveConflicts(List<String> order, Map<String, Integer> intended) {
        Map<String, Integer> result  = new LinkedHashMap<>();
        Set<Integer> claimed = new HashSet<>();

        for (String name : order) {
            if (!intended.containsKey(name)) continue;
            int target = intended.get(name);
            if (target == -1) {
                result.put(name, -1);
                continue;
            }

            if (claimed.contains(target)) continue;

            int myCur = trainSection.get(name);
            String occupant = sectionOccupant.get(target);
            if (occupant != null && intended.containsKey(occupant)
                    && Objects.equals(intended.get(occupant), myCur))
                continue;

            result.put(name, target);
            claimed.add(target);
        }
        return result;
    }
    
    private List<String> dedup(String[] names) {
        List<String> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String n : names) if (seen.add(n)) result.add(n);
        return result;
    }
}