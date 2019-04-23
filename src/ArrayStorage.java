import java.util.Arrays;

/**
 * Array based storage for Resumes
 */
public class ArrayStorage {

    private int currentSize;
    private final int MAX_CAPACITY = 10_000;
    private Resume[] storage = new Resume[MAX_CAPACITY];

    Resume get(String uuid) {
        int index = findIndex(uuid);
        if (index >= 0) {
            return storage[index];
        } else {
            System.out.println("Resume hasn't been found.");
            return null;
        }
    }

    void update(Resume resume) {
        int index = findIndex(resume.getUuid());
        if (index >= 0) {
            storage[index] = resume;
        } else {
            System.out.println("Resume hasn't been found.");
        }
    }

    void save(Resume resume) {
        if (currentSize == MAX_CAPACITY) {
            System.out.println("Resume hasn't been added. Maximum storage size reached.");
            return;
        }

        int index = findIndex(resume.getUuid());
        if (index >= 0) {
            System.out.println("Resume hasn't been added. Resume with such uuid exists.");
            return;
        }
        storage[currentSize] = resume;
        currentSize++;
    }

    void delete(String uuid) {
        int index = findIndex(uuid);
        if (index >= 0) {
            storage[index] = storage[currentSize - 1];
            storage[currentSize - 1] = null;
            currentSize--;
        } else {
            System.out.println("Resume hasn't been found.");
        }
    }

    int size() {
        return currentSize;
    }

    void clear() {
        Arrays.fill(storage, 0, currentSize, null);
        currentSize = 0;
    }

    Resume[] getAll() {
        return Arrays.copyOf(storage, currentSize);
    }

    private int findIndex(String uuid) {
        for (int i = 0; i < currentSize; i++) {
            if (storage[i].getUuid().equals(uuid)) {
                return i;
            }
        }
        return -1;
    }

}
