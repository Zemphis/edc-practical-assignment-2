import java.util.*;

public class InterlockingImpl {
    private static final Set<Integer> ALL_SECTIONS =
            new HashSet<> (Arrays.asList(1,2,3,4,5,6,7,8,9,10,11));

    private static final Map<Integer, List<Integer>> NEXT = new HashMap<>();

    static {
        NEXT.put(1, Arrays.asList(2));
        NEXT.put(2, Arrays.asList(3));

        NEXT.put(3, Collection.emptyList()); // east exit
        NEXT.put(4, Arrays.asList(5));
        NEXT.put(5, Arrays.asList(6, 10)); // main line preferred

        NEXT.put(6, Collections.emptyList()); // east exit
        NEXT.put(7, Arrays.asList(8));
        NEXT.put(8, Arrays.asList(9));

        NEXT.put(9, Collections.emptyList());
        NEXT.put(10, Arrays.asList(6));
    }

    private static final Set<Integer> ENTRY_SECTIONS = new HashSet<>(Arrays.asList(1,4,7));

    private final Map<Integer, String> sectionOccupant =  new HashMap<>();
    private final Map<Integer, List<Integer>> trainPosition = new HashMap<>();
    private final Map<Integer, List<Integer>> trainSection = new HashMap<>();
    private final Map<Integer, List<Integer>> trainDestination = new HashMap<>();

    public InterlockingImpl() {
        for (int s :VALID_SECTIONS) sectionOccupant.put(s, null);
    }

    @Override
    public void addTrain(String trainName, int entryTrackSection, int destinationTrackSection)
            throws IllegalArgumentException, IllegalStateException {
        if (trainSection.containsKey(trainName))
            throw new IllegalStateException("Train already exists: " + trainName);
        if (!VALID_SECTIONS.contains(destinationTrackSection))
            throw new IllegalStateException("Destination track section does not exist: " + destinationTrackSection);
        if (!hasPath(entryTrackSection, destinationTrackSection))
            throw new IllegalStateException("No valid path from " + entryTrackSection + " to " + destinationTrackSection));
        if (sectionOccupant.get(trainName) != null)
            throw new IllegalStateException( "Entry section " + entryTrackSection + " is already occupied by " + sectionOccupant.get(entryTrackSection)));
        sectionOccupant.put(entryTrackSection, trainName);
        trainSection.put(trainName, entryTrackSection);
        trainDestination.put(trainName, destinationTrackSection);
    }

    @Override
    public int moveTrains(String[] trainNames) throws IllegalArguementException()
}